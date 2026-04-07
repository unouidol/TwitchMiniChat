(function () {
  const NATIVE_APP = "pcgprobe";

  if (window.__pcgMissingSpawnableCollectorLoaded) {
    return;
  }
  window.__pcgMissingSpawnableCollectorLoaded = true;

  function send(type, payload) {
    try {
      browser.runtime.sendNativeMessage(NATIVE_APP, {
        type,
        payload,
      });
    } catch (e) {
      console.error("PCG collector send error", e);
    }
  }

  function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  function compactText(text, maxLen = 300) {
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

    // il frame buono è quello tipo pm0....ext-twitch.tv
    if (host.endsWith(".ext-twitch.tv") && !host.startsWith("supervisor.")) {
      return true;
    }

    return false;
  }

  function looksLikePokedex() {
    return !!document.querySelector(
      ".pokedex__container, .pokedex__grid, .pokedex__entry, .pokedex__entry-name"
    );
  }

  function normalizePokemonName(text) {
    return (text || "")
      .replace(/^🎊\s*/, "")
      .replace(/\s+/g, " ")
      .trim();
  }

  function parseProgressLines() {
    return Array.from(document.querySelectorAll(".pokedex__progress-line"))
      .map(el => compactText(el.innerText || el.textContent || "", 120))
      .filter(Boolean);
  }

  function getFilterLabelByText(name) {
    const wanted = name.trim().toLowerCase();
    const labels = Array.from(document.querySelectorAll(".pokedex__filter-row label"));

    for (const label of labels) {
      const text = compactText(label.innerText || label.textContent || "", 80).toLowerCase();
      if (text === wanted) {
        return label;
      }
    }

    return null;
  }

  function getCheckboxState(label) {
    if (!label) return null;

    const input = label.querySelector('input[type="checkbox"]');
    if (input) return !!input.checked;

    return null;
  }

  async function setFilterState(name, desiredState) {
    const label = getFilterLabelByText(name);
    if (!label) {
      return {
        found: false,
        name,
        desiredState,
        before: null,
        after: null
      };
    }

    const before = getCheckboxState(label);

    if (before === desiredState) {
      return {
        found: true,
        name,
        desiredState,
        before,
        after: before
      };
    }

    label.click();
    await sleep(700);

    const after = getCheckboxState(label);

    return {
      found: true,
      name,
      desiredState,
      before,
      after
    };
  }

  async function ensureWantedFilters() {
    const spawnableResult = await setFilterState("Spawnable", true);
    const obtainedResult = await setFilterState("Obtained", false);

    await sleep(900);

    return {
      spawnable: spawnableResult,
      obtained: obtainedResult
    };
  }

  function collectVisibleNames() {
    let nodes = Array.from(document.querySelectorAll(".pokedex__entry-name"));

    if (nodes.length === 0) {
      nodes = Array.from(document.querySelectorAll(".pokedex__entry"));
    }

    const out = [];
    const seen = new Set();

    for (const el of nodes) {
      const raw = compactText(el.innerText || el.textContent || "", 120);
      if (!raw) continue;

      const name = normalizePokemonName(raw);
      if (!name) continue;

      const key = name.toLowerCase();
      if (seen.has(key)) continue;
      seen.add(key);

      out.push(name);
    }

    return out;
  }

  function findScrollableContainer() {
    const candidates = [
      document.querySelector(".pokedex__wrapper"),
      document.querySelector(".pokedex__grid"),
      document.querySelector(".pokedex__container")
    ].filter(Boolean);

    for (const el of candidates) {
      const style = window.getComputedStyle(el);
      const overflowY = style.overflowY;
      const canScroll = el.scrollHeight > el.clientHeight + 20;
      if (canScroll || overflowY === "auto" || overflowY === "scroll") {
        return el;
      }
    }

    return document.scrollingElement || document.documentElement || document.body;
  }

  async function collectAllNamesByScrolling() {
    const container = findScrollableContainer();
    const names = new Set();

    let stableRounds = 0;
    let previousCount = -1;

    if (container && typeof container.scrollTop === "number") {
      container.scrollTop = 0;
      await sleep(500);
    }

    for (let i = 0; i < 120; i++) {
      const visible = collectVisibleNames();
      for (const name of visible) {
        names.add(name);
      }

      const currentCount = names.size;
      const maxScrollTop = Math.max(0, container.scrollHeight - container.clientHeight);
      const currentScrollTop = container.scrollTop || 0;

      if (currentCount === previousCount) {
        stableRounds += 1;
      } else {
        stableRounds = 0;
      }
      previousCount = currentCount;

      if (currentScrollTop >= maxScrollTop - 5 && stableRounds >= 2) {
        break;
      }

      const step = Math.max(Math.floor((container.clientHeight || 400) * 0.85), 220);
      const nextScrollTop = Math.min(maxScrollTop, currentScrollTop + step);

      if (nextScrollTop === currentScrollTop) {
        if (stableRounds >= 2) {
          break;
        }
      } else {
        container.scrollTop = nextScrollTop;
      }

      await sleep(350);
    }

    return {
      names: Array.from(names).sort((a, b) => a.localeCompare(b)),
      scrollInfo: {
        scrollTop: container.scrollTop || 0,
        scrollHeight: container.scrollHeight || 0,
        clientHeight: container.clientHeight || 0,
        tag: container.tagName || null,
        className: (container.className || "").toString()
      }
    };
  }

  async function waitForRealPokedex(maxAttempts = 60, delayMs = 500) {
    for (let i = 0; i < maxAttempts; i++) {
      if (isRealPcgFrame() && looksLikePokedex()) {
        return true;
      }
      await sleep(delayMs);
    }
    return false;
  }

  async function runExtraction(triggerReason) {
    if (!isRealPcgFrame()) {
      return;
    }

    send("pcg_missing_spawnable_extract", {
      ok: false,
      reason: "frame_seen_waiting_for_pokedex",
      triggerReason,
      frame: frameInfo()
    });

    const ready = await waitForRealPokedex();
    if (!ready) {
      send("pcg_missing_spawnable_extract", {
        ok: false,
        reason: "pokedex_not_found_in_real_frame",
        triggerReason,
        frame: frameInfo()
      });
      return;
    }

    const filterState = await ensureWantedFilters();
    const progressLines = parseProgressLines();
    const result = await collectAllNamesByScrolling();

    send("pcg_missing_spawnable_extract", {
      ok: true,
      triggerReason,
      frame: frameInfo(),
      filterState,
      progressLines,
      count: result.names.length,
      firstNames: result.names.slice(0, 40),
      lastNames: result.names.slice(-20),
      names: result.names,
      scrollInfo: result.scrollInfo
    });
  }

  let extractionScheduled = false;

  function scheduleExtraction(reason) {
    if (extractionScheduled) return;
    extractionScheduled = true;

    setTimeout(() => {
      runExtraction(reason).catch((e) => {
        send("pcg_missing_spawnable_extract", {
          ok: false,
          reason: "exception",
          triggerReason: reason,
          error: String(e),
          frame: frameInfo()
        });
      });
    }, 1200);
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