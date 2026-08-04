(function () {
  const NATIVE_APP = "pcgprobe";

  if (window.__pcgMissingSpawnableCollectorLoaded) {
    return;
  }
  window.__pcgMissingSpawnableCollectorLoaded = true;

  /*
   * The collector is intentionally passive.
   *
   * It never clicks PCG filters, never changes the selected tab, and never
   * tries to "fix" the Pokédex state for the user. It only publishes a valid
   * snapshot when the user is already on the Pokédex with the Spawnable-only
   * filter state active.
   */
  const INITIAL_SCHEDULE_DELAY_MS = 800;
  const PASSIVE_STATE_POLL_MS = 750;
  const SNAPSHOT_MIN_INTERVAL_MS = 1400;
  const SETTLE_BEFORE_READ_MS = 900;
  const INVALID_RETRY_DELAY_MS = 1200;
  const WAIT_FOR_POKEDEX_ATTEMPTS = 12;
  const WAIT_FOR_POKEDEX_DELAY_MS = 500;

  let extractionScheduled = false;
  let extractionRunning = false;
  let extractionRerunRequested = false;
  let lastSnapshotAttemptAtMs = 0;
  let lastStateSignature = "";

  function send(type, payload) {
    try {
      browser.runtime.sendNativeMessage(NATIVE_APP, {
        type,
        payload,
      });
    } catch {
      console.error("PCG collector send failed");
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

  send("pcg_probe_boot", {
    source: "pokedex_collector",
    mode: "passive_spawnable_only_snapshot",
    frame: frameInfo()
  });

  function isRealPcgFrame() {
    const host = (location.host || "").toLowerCase();

    if (host.endsWith(".ext-twitch.tv") && !host.startsWith("supervisor.")) {
      return true;
    }

    return false;
  }

  function looksLikePokedexDomExists() {
    /*
     * Keep this intentionally simple.
     *
     * PCG can keep content inside nested iframe/layout structures where strict
     * visibility checks are unreliable from this content script. This function
     * only answers: "Can the Pokédex DOM be found and read right now?"
     */
    return !!document.querySelector(
      ".pokedex__container, .pokedex__grid, .pokedex__entry, .pokedex__entry-name"
    );
  }

  function normalizeForComparison(text) {
    return compactText(text, 120)
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "");
  }

  function textLooksLikePokedex(text) {
    const normalized = normalizeForComparison(text);
    return normalized.includes("pokedex") || normalized.includes("pokédex");
  }

  function elementLooksSelected(el) {
    if (!el) return false;

    const ariaSelected = (el.getAttribute("aria-selected") || "").toLowerCase();
    const ariaCurrent = (el.getAttribute("aria-current") || "").toLowerCase();
    const dataActive = (el.getAttribute("data-active") || "").toLowerCase();
    const className = (el.className || "").toString().toLowerCase();

    return (
      ariaSelected === "true" ||
      ariaCurrent === "true" ||
      ariaCurrent === "page" ||
      dataActive === "true" ||
      className.includes("active") ||
      className.includes("selected") ||
      className.includes("current")
    );
  }

  function detectPokedexTabFromControls() {
    /*
     * Best-effort tab detection.
     *
     * Some PCG builds expose selected tabs through classes or ARIA attributes.
     * When that information is not available, the caller falls back to checking
     * whether the Pokédex DOM is readable.
     */
    const candidates = Array.from(document.querySelectorAll(
      "button, [role='tab'], a, .tab, .tabs__tab, .nav-item, .navigation__item"
    ));

    let sawPokedexControl = false;
    let sawSelectedPokedexControl = false;
    let sawSelectedNonPokedexControl = false;

    for (const el of candidates) {
      const text = compactText(el.innerText || el.textContent || "", 120);
      if (!text) continue;

      const isPokedex = textLooksLikePokedex(text);
      const isSelected = elementLooksSelected(el);

      if (isPokedex) {
        sawPokedexControl = true;
        if (isSelected) {
          sawSelectedPokedexControl = true;
        }
      } else if (isSelected) {
        sawSelectedNonPokedexControl = true;
      }
    }

    if (sawSelectedPokedexControl) {
      return true;
    }

    if (sawPokedexControl && sawSelectedNonPokedexControl) {
      return false;
    }

    return null;
  }

  function isPokedexTabActive() {
    const detectedFromControls = detectPokedexTabFromControls();

    if (detectedFromControls !== null) {
      return {
        active: detectedFromControls,
        detectionMode: "tab_control"
      };
    }

    return {
      active: looksLikePokedexDomExists(),
      detectionMode: "pokedex_dom"
    };
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

  function getFilterLabels() {
    return Array.from(document.querySelectorAll(".pokedex__filter-row label"));
  }

  function getCheckboxState(label) {
    if (!label) return null;

    const input = label.querySelector('input[type="checkbox"]');
    if (input) return !!input.checked;

    return null;
  }

  function readPokedexFilters() {
    /*
     * Passive filter reader.
     *
     * This replaces the previous automatic filter setter. It only reads the
     * current checkbox state and reports whether the user already selected the
     * required Spawnable-only view.
     */
    const labels = getFilterLabels();

    const filters = labels
      .map(label => {
        const name = compactText(label.innerText || label.textContent || "", 80);
        const key = normalizeForComparison(name);
        const checked = getCheckboxState(label);

        return {
          name,
          key,
          foundCheckbox: checked !== null,
          checked: checked === true
        };
      })
      .filter(filter => filter.name);

    const spawnableFilter = filters.find(filter => filter.key === "spawnable") || null;
    const obtainedFilter = filters.find(filter => filter.key === "obtained") || null;

    const activeNonSpawnableFilters = filters
      .filter(filter => filter.checked && filter.key !== "spawnable")
      .map(filter => filter.name);

    const spawnable = spawnableFilter ? spawnableFilter.checked : false;
    const obtained = obtainedFilter ? obtainedFilter.checked : false;

    const spawnableOnly =
      !!spawnableFilter &&
      spawnable === true &&
      activeNonSpawnableFilters.length === 0;

    return {
      found: filters.length > 0,
      filters,
      spawnable,
      obtained,
      activeNonSpawnableFilters,
      spawnableOnly,

      /*
       * Keep a legacy-shaped filterState payload so existing Android logging
       * remains readable while making it clear that no click happened.
       */
      legacyFilterState: {
        passive: true,
        spawnable: {
          found: !!spawnableFilter,
          name: "Spawnable",
          desiredState: true,
          before: spawnableFilter ? spawnableFilter.checked : null,
          after: spawnableFilter ? spawnableFilter.checked : null
        },
        obtained: {
          found: !!obtainedFilter,
          name: "Obtained",
          desiredState: false,
          before: obtainedFilter ? obtainedFilter.checked : null,
          after: obtainedFilter ? obtainedFilter.checked : null
        },
        activeNonSpawnableFilters
      }
    };
  }

  function readPokedexState() {
    const realFrame = isRealPcgFrame();
    const tabState = isPokedexTabActive();
    const pokedexDomFound = looksLikePokedexDomExists();

    const filters = pokedexDomFound
      ? readPokedexFilters()
      : {
          found: false,
          filters: [],
          spawnable: false,
          obtained: false,
          activeNonSpawnableFilters: [],
          spawnableOnly: false,
          legacyFilterState: {
            passive: true,
            spawnable: {
              found: false,
              name: "Spawnable",
              desiredState: true,
              before: null,
              after: null
            },
            obtained: {
              found: false,
              name: "Obtained",
              desiredState: false,
              before: null,
              after: null
            },
            activeNonSpawnableFilters: []
          }
        };

    let reason = null;

    if (!realFrame) {
      reason = "not_real_pcg_frame";
    } else if (!tabState.active || !pokedexDomFound) {
      reason = "pokedex_tab_not_active";
    } else if (!filters.found) {
      reason = "pokedex_filters_not_found";
    } else if (!filters.spawnable) {
      reason = "spawnable_filter_off";
    } else if (!filters.spawnableOnly) {
      reason = "spawnable_only_required";
    }

    const validForMissingDexUpload =
      realFrame &&
      tabState.active &&
      pokedexDomFound &&
      filters.found &&
      filters.spawnableOnly;

    return {
      source: "pokedex_collector",
      frame: frameInfo(),
      realFrame,
      onPokedexTab: tabState.active,
      tabDetectionMode: tabState.detectionMode,
      pokedexDomFound,
      filters: {
        found: filters.found,
        spawnable: filters.spawnable,
        obtained: filters.obtained,
        activeNonSpawnableFilters: filters.activeNonSpawnableFilters,
        all: filters.filters
      },
      filterState: filters.legacyFilterState,
      validForMissingDexUpload,
      reason,
      capturedAtMs: Date.now()
    };
  }

  function stateSignature(state) {
    return JSON.stringify({
      realFrame: state.realFrame,
      onPokedexTab: state.onPokedexTab,
      pokedexDomFound: state.pokedexDomFound,
      spawnable: state.filters.spawnable,
      obtained: state.filters.obtained,
      activeNonSpawnableFilters: state.filters.activeNonSpawnableFilters,
      validForMissingDexUpload: state.validForMissingDexUpload,
      reason: state.reason
    });
  }

  function sendPokedexState(state, force = false) {
    const signature = stateSignature(state);

    if (!force && signature === lastStateSignature) {
      return;
    }

    lastStateSignature = signature;
    send("pcg_pokedex_state", state);
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

    const originalScrollTop =
      container && typeof container.scrollTop === "number"
        ? container.scrollTop
        : 0;

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

      send("pcg_probe_progress", {
        step: 9310,
        phase: "pokedex_scroll_collecting",
        trigger: "collectAllNamesByScrolling",
        collected: currentCount,
        lockedCount: lockedNames.size,
        scrollTop: currentScrollTop,
        scrollHeight: container.scrollHeight || 0,
        clientHeight: container.clientHeight || 0,
        host: location.host,
        href: location.href
      });

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

    /*
     * Restore the user's scroll position as best as possible. The collector is
     * passive, so it should avoid leaving the PCG UI in a different visual state.
     */
    try {
      if (container && typeof container.scrollTop === "number") {
        container.scrollTop = originalScrollTop;
      }
    } catch (_) {
      /* Best effort only. */
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

  async function waitForRealPokedex(
    maxAttempts = WAIT_FOR_POKEDEX_ATTEMPTS,
    delayMs = WAIT_FOR_POKEDEX_DELAY_MS
  ) {
    for (let i = 0; i < maxAttempts; i++) {
      const state = readPokedexState();
      const found = state.realFrame && state.pokedexDomFound;

      sendPokedexState(state);

      send("pcg_probe_progress", {
        step: 9200,
        phase: "pokedex_wait_for_dom",
        attempt: i + 1,
        maxAttempts,
        found,
        validForMissingDexUpload: state.validForMissingDexUpload,
        reason: state.reason,
        host: location.host,
        href: location.href,
        title: document.title,
        readyState: document.readyState
      });

      if (found) {
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

  async function readSnapshot(triggerReason, phase, initialState) {
    await sleep(SETTLE_BEFORE_READ_MS);

    const stateBeforeRead = readPokedexState();
    sendPokedexState(stateBeforeRead);

    if (!stateBeforeRead.validForMissingDexUpload) {
      return {
        ok: false,
        triggerReason,
        phase,
        frame: frameInfo(),
        filterState: stateBeforeRead.filterState,
        progressLines: parseProgressLines(),
        count: 0,
        lockedCount: 0,
        lockedNamesPreview: [],
        firstNames: [],
        lastNames: [],
        names: [],
        scrollInfo: null,
        validForMissingDexUpload: false,
        capturedAtMs: Date.now(),
        invalidReason: stateBeforeRead.reason || "invalid_pokedex_state_before_read"
      };
    }

    const progressLines = parseProgressLines();
    const result = await collectAllNamesByScrolling();

    const stateAfterRead = readPokedexState();
    sendPokedexState(stateAfterRead);

    const invalidReason =
      !stateAfterRead.validForMissingDexUpload
        ? stateAfterRead.reason || "invalid_pokedex_state_after_read"
        : result.names.length === 0
          ? "empty_snapshot"
          : result.lockedCount > 0
            ? "locked_entries_present"
            : null;

    return {
      ok: !invalidReason && !isClearlyBrokenSnapshot(result),
      triggerReason,
      phase,
      frame: frameInfo(),
      filterState: stateAfterRead.filterState || initialState.filterState,
      progressLines,
      count: result.names.length,
      lockedCount: result.lockedCount,
      lockedNamesPreview: result.lockedNames.slice(0, 30),
      firstNames: result.names.slice(0, 40),
      lastNames: result.names.slice(-20),
      names: result.names,
      scrollInfo: result.scrollInfo,
      validForMissingDexUpload: stateAfterRead.validForMissingDexUpload,
      capturedAtMs: Date.now(),
      invalidReason
    };
  }

  async function runExtraction(triggerReason) {
    send("pcg_probe_progress", {
      step: 9100,
      phase: "pokedex_run_extraction_started",
      triggerReason,
      href: location.href,
      host: location.host,
      title: document.title,
      readyState: document.readyState
    });

    if (!isRealPcgFrame()) {
      send("pcg_missing_spawnable_extract", {
        ok: false,
        reason: "not_real_pcg_frame",
        triggerReason,
        frame: frameInfo(),
        validForMissingDexUpload: false,
        capturedAtMs: Date.now()
      });
      return;
    }

    const ready = await waitForRealPokedex();
    if (!ready) {
      send("pcg_missing_spawnable_extract", {
        ok: false,
        reason: "pokedex_not_found_in_real_frame",
        triggerReason,
        frame: frameInfo(),
        validForMissingDexUpload: false,
        capturedAtMs: Date.now()
      });
      return;
    }

    const state = readPokedexState();
    sendPokedexState(state, true);

    if (!state.validForMissingDexUpload) {
      send("pcg_probe_progress", {
        step: 9250,
        phase: "pokedex_waiting_for_spawnable_only_filters",
        triggerReason,
        reason: state.reason,
        onPokedexTab: state.onPokedexTab,
        tabDetectionMode: state.tabDetectionMode,
        filters: state.filters,
        host: location.host,
        href: location.href
      });

      /*
       * Do not send a successful snapshot and do not change PCG filters.
       * Android can use pcg_pokedex_state to show a hint only after the user
       * explicitly presses the Pokédex update button.
       */
      return;
    }

    const now = Date.now();
    if (now - lastSnapshotAttemptAtMs < SNAPSHOT_MIN_INTERVAL_MS) {
      return;
    }
    lastSnapshotAttemptAtMs = now;

    let payload = await readSnapshot(triggerReason, "first_read", state);

    if (!payload.ok) {
      send("pcg_probe_progress", {
        step: 9991,
        collected: payload.count,
        lockedCount: payload.lockedCount,
        reason: payload.invalidReason,
        phase: "retry_scheduled_after_invalid_snapshot"
      });

      await sleep(INVALID_RETRY_DELAY_MS);

      const retryState = readPokedexState();
      sendPokedexState(retryState, true);

      if (!retryState.validForMissingDexUpload) {
        send("pcg_probe_progress", {
          step: 9992,
          phase: "retry_cancelled_invalid_pokedex_state",
          reason: retryState.reason,
          filters: retryState.filters
        });
        return;
      }

      payload = await readSnapshot(triggerReason, "retry_after_invalid_snapshot", retryState);

      if (!payload.ok) {
        send("pcg_missing_spawnable_extract", {
          ok: false,
          reason: payload.invalidReason || "invalid_snapshot_after_retry",
          triggerReason,
          frame: frameInfo(),
          filterState: payload.filterState,
          count: payload.count,
          lockedCount: payload.lockedCount,
          lockedNamesPreview: payload.lockedNamesPreview,
          firstNames: payload.firstNames,
          lastNames: payload.lastNames,
          progressLines: payload.progressLines,
          scrollInfo: payload.scrollInfo,
          validForMissingDexUpload: false,
          capturedAtMs: payload.capturedAtMs
        });
        return;
      }
    }

    send("pcg_missing_spawnable_extract", {
      ok: true,
      triggerReason,
      frame: payload.frame,
      filterState: payload.filterState,
      filters: {
        spawnable: true,
        obtained: false,
        activeNonSpawnableFilters: []
      },
      progressLines: payload.progressLines,
      count: payload.count,
      lockedCount: payload.lockedCount,
      firstNames: payload.firstNames,
      lastNames: payload.lastNames,
      names: payload.names,
      scrollInfo: payload.scrollInfo,
      validForMissingDexUpload: true,
      capturedAtMs: payload.capturedAtMs
    });
  }

  function scheduleExtraction(reason) {
    if (extractionRunning || extractionScheduled) {
      /*
       * Mutation and passive polling can fire while a scroll read is already in
       * progress. Do not chain endless reruns from our own DOM reads.
       */
      if (!reason.startsWith("mutation_") && !reason.startsWith("passive_state_poll")) {
        extractionRerunRequested = true;
      }
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
          frame: frameInfo(),
          validForMissingDexUpload: false,
          capturedAtMs: Date.now()
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

  function pollPokedexStateAndMaybeSchedule(reason) {
    if (!isRealPcgFrame()) {
      return;
    }

    const state = readPokedexState();
    sendPokedexState(state);

    if (state.validForMissingDexUpload) {
      scheduleExtraction(reason);
    }
  }

  if (isRealPcgFrame()) {
    pollPokedexStateAndMaybeSchedule("initial_real_frame");
  }

  const observer = new MutationObserver(() => {
    pollPokedexStateAndMaybeSchedule("mutation_real_frame");
  });

  observer.observe(document.documentElement, {
    childList: true,
    subtree: true
  });

  setInterval(() => {
    pollPokedexStateAndMaybeSchedule("passive_state_poll");
  }, PASSIVE_STATE_POLL_MS);
})();
