(function () {
  const NATIVE_APP = "pcgprobe";

  if (window.__pcgInventoryBallCollectorLoaded) {
    return;
  }
  window.__pcgInventoryBallCollectorLoaded = true;

  const INITIAL_DELAY_MS = 1200;
  const SETTLE_DELAY_MS = 1800;

  let scheduled = false;
  let running = false;
  let rerunRequested = false;

  function send(type, payload) {
    try {
      browser.runtime.sendNativeMessage(NATIVE_APP, {
        type,
        payload
      });
    } catch (e) {
      console.error("PCG inventory collector send error", e);
    }
  }

  function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  function compactText(text, maxLen = 200) {
    return (text || "")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, maxLen);
  }

  function frameInfo() {
    let isTop = false;
    try {
      isTop = window.top === window;
    } catch (_) {
      isTop = false;
    }

    return {
      href: location.href,
      title: document.title,
      isTop,
      readyState: document.readyState,
      host: location.host
    };
  }

  function isRealPcgFrame() {
    const host = (location.host || "").toLowerCase();
    return host.endsWith(".ext-twitch.tv") && !host.startsWith("supervisor.");
  }

  function normalizeBallName(name) {
    const cleaned = compactText(name, 80);

    if (/^poke ball$/i.test(cleaned)) return "Poké Ball";
    return cleaned;
  }

  function ballIdFromName(name) {
    const n = normalizeBallName(name).toLowerCase();
    return n
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]+/g, "_")
      .replace(/^_+|_+$/g, "");
  }


  function findBallSectionGrid() {
    const headings = Array.from(document.querySelectorAll("h1,h2,h3,h4,h5,h6,.category-heading"));

    for (const heading of headings) {
      const text = compactText(heading.innerText || heading.textContent || "", 80);
      if (!/^ball$/i.test(text)) continue;

      let sib = heading.nextElementSibling;
      while (sib) {
        if (sib.matches && sib.matches(".item-grid")) {
          return sib;
        }
        sib = sib.nextElementSibling;
      }
    }

    return document.querySelector(".item-grid");
  }

  function extractBallsFromGrid(grid) {
    if (!grid) return [];

    const cards = Array.from(grid.querySelectorAll(".item-entry-button.text-center.item-card"));
    const balls = [];

    for (const card of cards) {
      const nameEl = card.querySelector(".item-entry-button__name");
      const amountEl = card.querySelector(".item-entry-button__amount");

      const rawName = compactText(nameEl?.innerText || nameEl?.textContent || "", 80);
      const normalizedName = normalizeBallName(rawName);

      if (!/ball$/i.test(normalizedName)) {
        continue;
      }

      const rawCount = compactText(amountEl?.innerText || amountEl?.textContent || "", 40);
      const count = parseInt(rawCount, 10);

      if (!normalizedName) continue;
      if (!Number.isFinite(count)) continue;

      balls.push({
        ballId: ballIdFromName(normalizedName),
        name: normalizedName,
        count
      });
    }

    balls.sort((a, b) => a.name.localeCompare(b.name));
    return balls;
  }

  async function runExtraction(triggerReason) {
    if (!isRealPcgFrame()) return;

    send("pcg_probe_progress", {
      step: 9820,
      phase: "inventory_ball_final_probe_loaded",
      href: location.href,
      host: location.host
    });

    await sleep(SETTLE_DELAY_MS);

    const grid = findBallSectionGrid();
    const balls = extractBallsFromGrid(grid);

    if (!grid) {
      send("pcg_inventory_ball_extract", {
        ok: false,
        reason: "ball_grid_not_found",
        triggerReason,
        frame: frameInfo()
      });
      return;
    }

    if (balls.length === 0) {
      send("pcg_inventory_ball_extract", {
        ok: false,
        reason: "ball_cards_not_found_or_empty",
        triggerReason,
        frame: frameInfo()
      });
      return;
    }

    send("pcg_inventory_ball_extract", {
      ok: true,
      triggerReason,
      frame: frameInfo(),
      count: balls.length,
      balls,
      firstBalls: balls.slice(0, 20)
    });
  }

  function scheduleExtraction(reason) {
    if (running || scheduled) {
      rerunRequested = true;
      return;
    }

    scheduled = true;

    setTimeout(async () => {
      scheduled = false;
      running = true;

      try {
        await runExtraction(reason);
      } catch (e) {
        send("pcg_inventory_ball_extract", {
          ok: false,
          reason: "exception",
          triggerReason: reason,
          error: String(e),
          frame: frameInfo()
        });
      } finally {
        running = false;

        if (rerunRequested) {
          rerunRequested = false;
          scheduleExtraction("queued_rerun");
        }
      }
    }, INITIAL_DELAY_MS);
  }

  if (isRealPcgFrame()) {
    scheduleExtraction("initial_real_frame");
  }

  const observer = new MutationObserver(() => {
    if (isRealPcgFrame()) {
      scheduleExtraction("mutation_real_frame");
    }
  });

  observer.observe(document.documentElement, {
    childList: true,
    subtree: true
  });
})();