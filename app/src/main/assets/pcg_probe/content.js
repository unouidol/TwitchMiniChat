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

  /*
   * Android now understands these two semantic events.
   *
   * They are intentionally sent by the content script even when Android has not
   * requested anything yet. GeckoSessionManager ignores them unless a manual
   * Register inventory / Register Pokédex request is currently pending.
   */
  const TYPE_POKEDEX_WRONG_TAB = "pcg_pokedex_wrong_tab";
  const TYPE_INVENTORY_WRONG_TAB = "pcg_inventory_wrong_tab";

  /*
   * This monitor makes the wrong-tab toast feel immediate.
   *
   * Without it, Android can only notice the wrong tab after the 15 second manual
   * update timeout. With this, a pending manual request is usually answered within
   * about 1.5 seconds.
   */
  const WRONG_TAB_MONITOR_INTERVAL_MS = 1500;
  const WRONG_TAB_MESSAGE_COOLDOWN_MS = 1500;

  let lastPokedexWrongTabSentAt = 0;
  let lastInventoryWrongTabSentAt = 0;

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

  function bodyText(maxLen = 2500) {
    const body = document.body;
    if (!body) return "";

    return compactText(body.innerText || body.textContent || "", maxLen);
  }

  function lowerBodyText(maxLen = 2500) {
    return bodyText(maxLen).toLowerCase();
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

  function looksLikeInventory() {
    const selectorMatch = document.querySelector(
      [
        ".inventory__container",
        ".inventory__wrapper",
        ".inventory__grid",
        ".inventory__item",
        ".inventory__item-name",
        ".inventory-page",
        "[class*='inventory']"
      ].join(", ")
    );

    if (selectorMatch) {
      return true;
    }

    const text = lowerBodyText(3000);

    const hasInventoryWord = text.includes("inventory");
    const knownBallNames = [
      "poké ball",
      "poke ball",
      "great ball",
      "ultra ball",
      "premier ball",
      "quick ball",
      "timer ball",
      "repeat ball"
    ];

    let ballMentions = 0;
    for (const ballName of knownBallNames) {
      if (text.includes(ballName)) {
        ballMentions += 1;
      }
    }

    /*
     * The Inventory tab normally contains several ball names together.
     *
     * This fallback helps if PCG changes its CSS class names but the visible
     * text still clearly looks like the ball inventory.
     */
    return hasInventoryWord || ballMentions >= 2;
  }

  function looksLikeAnyLoadedPcgSurface() {
    if (looksLikePokedex() || looksLikeInventory()) {
      return true;
    }

    const text = lowerBodyText(1800);

    if (!text || text.length < 30) {
      return false;
    }

    return (
      text.includes("pokédex") ||
      text.includes("pokedex") ||
      text.includes("inventory") ||
      text.includes("spawnable") ||
      text.includes("obtained") ||
      text.includes("poké ball") ||
      text.includes("poke ball") ||
      text.includes("great ball") ||
      text.includes("ultra ball")
    );
  }

  function sendWrongTabMessage(type, reason, details) {
    const now = Date.now();

    if (type === TYPE_POKEDEX_WRONG_TAB) {
      if (now - lastPokedexWrongTabSentAt < WRONG_TAB_MESSAGE_COOLDOWN_MS) {
        return;
      }
      lastPokedexWrongTabSentAt = now;
    }

    if (type === TYPE_INVENTORY_WRONG_TAB) {
      if (now - lastInventoryWrongTabSentAt < WRONG_TAB_MESSAGE_COOLDOWN_MS) {
        return;
      }
      lastInventoryWrongTabSentAt = now;
    }

    send(type, {
      ok: false,
      reason,
      frame: frameInfo(),
      pokedexVisible: looksLikePokedex(),
      inventoryVisible: looksLikeInventory(),
      details: details || {}
    });
  }

  function monitorWrongTabsOnce(reason) {
    if (!isRealPcgFrame()) {
      return;
    }

    /*
     * Avoid sending wrong-tab events while the extension iframe is still blank or
     * very early in its loading phase. Android still has its 15 second timeout as
     * a fallback for ambiguous cases.
     */
    if (!looksLikeAnyLoadedPcgSurface()) {
      return;
    }

    const pokedexVisible = looksLikePokedex();
    const inventoryVisible = looksLikeInventory();

    if (!pokedexVisible) {
      sendWrongTabMessage(TYPE_POKEDEX_WRONG_TAB, reason, {
        expectedTab: "pokedex",
        actualSurfaceLooksLikeInventory: inventoryVisible
      });
    }

    if (!inventoryVisible) {
      sendWrongTabMessage(TYPE_INVENTORY_WRONG_TAB, reason, {
        expectedTab: "inventory",
        actualSurfaceLooksLikePokedex: pokedexVisible
      });
    }
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

      /*
       * While waiting, keep publishing wrong-tab state. Android only reacts if the
       * user has just pressed Register Pokédex.
       */
      monitorWrongTabsOnce("waiting_for_pokedex");

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

    monitorWrongTabsOnce("pokedex_extraction_started");

    send("pcg_missing_spawnable_extract", {
      ok: false,
      reason: "frame_seen_waiting_for_pokedex",
      triggerReason,
      frame: frameInfo()
    });

    const ready = await waitForRealPokedex();
    if (!ready) {
      sendWrongTabMessage(TYPE_POKEDEX_WRONG_TAB, "pokedex_not_visible_after_wait", {
        triggerReason
      });

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
    monitorWrongTabsOnce("initial_real_frame");
    scheduleExtraction("initial_real_frame");
  }

  const observer = new MutationObserver(() => {
    if (isRealPcgFrame()) {
      monitorWrongTabsOnce("mutation_real_frame");
      scheduleExtraction("mutation_real_frame");
    }
  });

  observer.observe(document.documentElement, {
    childList: true,
    subtree: true
  });

  setInterval(() => {
    monitorWrongTabsOnce("wrong_tab_monitor");
  }, WRONG_TAB_MONITOR_INTERVAL_MS);
})();