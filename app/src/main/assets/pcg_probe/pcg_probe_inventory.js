(function () {
  const NATIVE_APP = "pcgprobe";

  if (window.__pcgInventoryBallCollectorLoaded) {
    return;
  }
  window.__pcgInventoryBallCollectorLoaded = true;

  const INITIAL_DELAY_MS = 1200;
  const SETTLE_DELAY_MS = 1800;

  /*
   * Passive Inventory snapshots keep Android's in-memory candidate fresh while
   * the user stays on the Inventory tab.
   *
   * This does not save data by itself. Android still commits the snapshot only
   * after the user presses Register Inventory.
   */
  const PASSIVE_POLL_INTERVAL_MS = 2000;
  const PASSIVE_POLL_REASON = "passive_poll";

  let scheduled = false;
  let running = false;
  let rerunRequested = false;

  function send(type, payload) {
    try {
      browser.runtime.sendNativeMessage(NATIVE_APP, {
        type,
        payload
      });
    } catch {
      console.error("PCG inventory collector send failed");
    }
  }

  function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Returns true when an extraction was triggered only to refresh Android's
   * passive Inventory candidate.
   *
   * Passive polls should send successful snapshots, but they should avoid sending
   * repeated failure payloads while the user is not on the Inventory tab.
   */
  function isPassivePoll(triggerReason) {
    return triggerReason === PASSIVE_POLL_REASON;
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

    const passivePoll = isPassivePoll(triggerReason);

    /*
     * FAST PATH: If we can't find the grid markers even BEFORE the settle delay,
     * it's a strong signal that the user is not on the Inventory tab.
     *
     * For passive polling we stay quiet on failures, otherwise Android would get
     * repeated "not Inventory" failures every few seconds while the user is on
     * another PCG tab. Successful passive snapshots are still sent normally.
     */
    const quickGrid = findBallSectionGrid();
    if (!quickGrid && !passivePoll) {
      send("pcg_inventory_ball_extract", {
        ok: false,
        reason: "ball_grid_not_found",
        isQuickCheck: true,
        triggerReason,
        frame: frameInfo()
      });
      // We don't return here, in case the grid appears during the settle delay.
    }

    if (!passivePoll) {
      send("pcg_probe_progress", {
        step: 9820,
        phase: "inventory_ball_final_probe_loaded",
        href: location.href,
        host: location.host
      });
    }

    await sleep(SETTLE_DELAY_MS);

    const grid = findBallSectionGrid();
    const balls = extractBallsFromGrid(grid);

    if (!grid) {
      if (!passivePoll) {
        send("pcg_inventory_ball_extract", {
          ok: false,
          reason: "ball_grid_not_found",
          triggerReason,
          frame: frameInfo()
        });
      }
      return;
    }

    if (balls.length === 0) {
      if (!passivePoll) {
        send("pcg_inventory_ball_extract", {
          ok: false,
          reason: "ball_cards_not_found_or_empty",
          triggerReason,
          frame: frameInfo()
        });
      }
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

  /**
   * Schedules one Inventory extraction while preventing overlapping reads.
   *
   * Mutation-driven reads can request a queued rerun because DOM changes may mean
   * the inventory grid is still settling. Passive polling, instead, should skip a
   * tick when the extractor is already busy so it does not create an endless queue.
   */
  function scheduleExtraction(reason, options = {}) {
    const queueIfBusy = options.queueIfBusy !== false;

    if (running || scheduled) {
      if (queueIfBusy) {
        rerunRequested = true;
      }
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

  /**
   * Starts a lightweight passive polling loop for Inventory snapshots.
   *
   * This keeps Android's latest Inventory candidate fresh while the user remains
   * on the Inventory tab. The loop does not force any save: Android still stores
   * the result only after the user presses Register Inventory.
   */
  function startPassiveInventoryPolling() {
    if (!isRealPcgFrame()) return;

    window.setInterval(() => {
      scheduleExtraction(PASSIVE_POLL_REASON, {
        queueIfBusy: false
      });
    }, PASSIVE_POLL_INTERVAL_MS);
  }

  if (isRealPcgFrame()) {
    scheduleExtraction("initial_real_frame");
    startPassiveInventoryPolling();
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
