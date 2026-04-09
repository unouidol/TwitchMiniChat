(function () {
  const NATIVE_APP = "pcgprobe";

  if (window.__pcgMissingSpawnableCollectorLoaded) {
    return;
  }
  window.__pcgMissingSpawnableCollectorLoaded = true;

  const INITIAL_SCHEDULE_DELAY_MS = 1200;
  const SETTLE_BEFORE_READ_MS = 3000;
  const INVALID_RETRY_DELAY_MS = 1800;
  const WAIT_FOR_POKEDEX_ATTEMPTS = 60;
  const WAIT_FOR_POKEDEX_DELAY_MS = 500;

  let extractionScheduled = false;
  let extractionRunning = false;
  let extractionRerunRequested = false;

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

  function collectVisibleEntries() {
    let nodes = Array.from(document.querySelectorAll(".pokedex__entry-name"));

    if (nodes.length === 0) {
      nodes = Array.from(document.querySelectorAll(".pokedex__entry"));
    }

    const names = [];
    const lockedNames = [];
    const seen = new Set();
    const seenLocked = new Set();

    for (const el of nodes) {
      const raw = compactText(el.innerText || el.textContent || "", 120);
      if (!raw) continue;

      const name = normalizePokemonName(raw);
      if (!name) continue;

      const key = name.toLowerCase();
      if (!seen.has(key)) {
        seen.add(key);
        names.push(name);
      }

      if (raw.includes("🔒") && !seenLocked.has(key)) {
        seenLocked.add(key);
        lockedNames.push(name);
      }
    }

    return {
      names,
      lockedNames
    };
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
    const lockedNames = new Set();

    let stableRounds = 0;
    let previousCount = -1;

    if (container && typeof container.scrollTop === "number") {
      container.scrollTop = 0;
      await sleep(500);
    }

    for (let i = 0; i < 120; i++) {
      const visible = collectVisibleEntries();

      for (const name of visible.names) {
        names.add(name);
      }

      for (const name of visible.lockedNames) {
        lockedNames.add(name);
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
      lockedNames: Array.from(lockedNames).sort((a, b) => a.localeCompare(b)),
      lockedCount: lockedNames.size,
      scrollInfo: {
        scrollTop: container.scrollTop || 0,
        scrollHeight: container.scrollHeight || 0,
        clientHeight: container.clientHeight || 0,
        tag: container.tagName || null,
        className: (container.className || "").toString()
      }
    };
  }

  async function waitForRealPokedex(maxAttempts = WAIT_FOR_POKEDEX_ATTEMPTS, delayMs = WAIT_FOR_POKEDEX_DELAY_MS) {
    for (let i = 0; i < maxAttempts; i++) {
      if (isRealPcgFrame() && looksLikePokedex()) {
        return true;
      }
      await sleep(delayMs);
    }
    return false;
  }

  function isClearlyBrokenSnapshot(result) {
    if (!result || !Array.isArray(result.names) || result.names.length === 0) {
      return true;
    }

    if ((result.lockedCount || 0) > 0) {
      return true;
    }

    return false;
  }

  async function readSnapshot(triggerReason, phase, filterState) {
    await sleep(SETTLE_BEFORE_READ_MS);

    const progressLines = parseProgressLines();
    const result = await collectAllNamesByScrolling();

    const invalidReason =
      result.names.length === 0
        ? "empty_snapshot"
        : result.lockedCount > 0
          ? "locked_entries_present"
          : null;

    return {
      ok: !isClearlyBrokenSnapshot(result),
      triggerReason,
      phase,
      frame: frameInfo(),
      filterState,
      progressLines,
      count: result.names.length,
      lockedCount: result.lockedCount,
      lockedNamesPreview: result.lockedNames.slice(0, 30),
      firstNames: result.names.slice(0, 40),
      lastNames: result.names.slice(-20),
      names: result.names,
      scrollInfo: result.scrollInfo,
      invalidReason
    };
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

    let filterState = await ensureWantedFilters();
    let payload = await readSnapshot(triggerReason, "first_read", filterState);

    if (!payload.ok) {
      send("pcg_probe_progress", {
        step: 9991,
        collected: payload.count,
        lockedCount: payload.lockedCount,
        reason: payload.invalidReason,
        phase: "retry_scheduled_after_invalid_snapshot"
      });

      await sleep(INVALID_RETRY_DELAY_MS);

      filterState = await ensureWantedFilters();
      payload = await readSnapshot(triggerReason, "retry_after_invalid_snapshot", filterState);

      if (!payload.ok) {
        send("pcg_missing_spawnable_extract", {
          ok: false,
          reason: payload.invalidReason || "invalid_snapshot_after_retry",
          triggerReason,
          frame: frameInfo(),
          filterState,
          count: payload.count,
          lockedCount: payload.lockedCount,
          lockedNamesPreview: payload.lockedNamesPreview,
          firstNames: payload.firstNames,
          lastNames: payload.lastNames,
          progressLines: payload.progressLines,
          scrollInfo: payload.scrollInfo
        });
        return;
      }
    }

    send("pcg_missing_spawnable_extract", {
      ok: true,
      triggerReason,
      frame: payload.frame,
      filterState: payload.filterState,
      progressLines: payload.progressLines,
      count: payload.count,
      lockedCount: payload.lockedCount,
      firstNames: payload.firstNames,
      lastNames: payload.lastNames,
      names: payload.names,
      scrollInfo: payload.scrollInfo
    });
  }

  function scheduleExtraction(reason) {
    if (extractionRunning || extractionScheduled) {
      extractionRerunRequested = true;
      return;
    }

    extractionScheduled = true;

    setTimeout(async () => {
      extractionScheduled = false;
      extractionRunning = true;

      try {
        await runExtraction(reason);
      } catch (e) {
        send("pcg_missing_spawnable_extract", {
          ok: false,
          reason: "exception",
          triggerReason: reason,
          error: String(e),
          frame: frameInfo()
        });
      } finally {
        extractionRunning = false;

        if (extractionRerunRequested) {
          extractionRerunRequested = false;
          scheduleExtraction("queued_rerun");
        }
      }
    }, INITIAL_SCHEDULE_DELAY_MS);
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