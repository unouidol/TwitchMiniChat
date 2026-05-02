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

    let activePokedexUpdateRunId = null;
    let lastReportedPokedexTabVisible = null;

    /**
     * Returns true only while Android has explicitly requested a manual Pokédex update.
     *
     * PCG filters must never be changed by passive observers, timers, boot logic,
     * or page-readiness checks. Without this guard, the probe can fight the user by
     * turning Spawnable back on after the user manually disabled it.
     */
    function canModifyPokedexFilters() {
      return activePokedexUpdateRunId !== null;
    }

    /**
     * Reports that a filter-changing path was blocked because no manual update is active.
     *
     * This makes old automatic paths visible in Logcat while preventing them from
     * modifying the user's current PCG view.
     */
    function reportBlockedPokedexFilterChange(reason) {
      send("pcg_probe_filter_change_blocked", {
        reason: reason || "no_active_manual_update",
        frame: frameInfo()
      });
    }

      /**
       * Sends a request to Android and waits for a response.
       *
       * This is used so the PCG side can ask Android whether the user has pressed
       * the manual Update Pokédex button.
       */
      async function requestNative(type, payload) {
        try {
          return await browser.runtime.sendNativeMessage(NATIVE_APP, {
            type,
            payload
          });
        } catch (e) {
          console.error("PCG collector requestNative error", e);
          return null;
        }
      }

        /**
         * Normalizes the response returned by Android.
         *
         * GeckoView can return the native response either as an object or as a JSON-like
         * string depending on the bridge path. This keeps the command polling tolerant
         * instead of silently dropping a valid response.
         */
        function normalizeNativeCommandResponse(response) {
          if (!response) {
            return null;
          }

          if (typeof response === "object") {
            return response;
          }

          if (typeof response === "string") {
            try {
              return JSON.parse(response);
            } catch (e) {
              send("pcg_probe_progress", {
                step: 9501,
                phase: "command_poll_response_parse_failed",
                responsePreview: compactText(response, 300),
                error: String(e),
                frame: frameInfo()
              });

              return null;
            }
          }

          send("pcg_probe_progress", {
            step: 9502,
            phase: "command_poll_response_unsupported_type",
            responseType: typeof response,
            frame: frameInfo()
          });

          return null;
        }

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

  send("pcg_probe_boot", {
    source: "pokedex_collector",
    frame: frameInfo()
  });

  function isRealPcgFrame() {
    const host = (location.host || "").toLowerCase();

    if (host.endsWith(".ext-twitch.tv") && !host.startsWith("supervisor.")) {
      return true;
    }

    return false;
  }

  function looksLikePokedex() {
    /*
     * Keep this intentionally simple.
     *
     * Do not use "actual visibility" checks here. PCG can keep content inside
     * nested iframe/layout structures where getClientRects or visibility checks
     * are unreliable from this content script.
     *
     * For this collector, the useful question is only:
     * "Can the Pokédex DOM be found and read right now?"
     */
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

      /**
       * Applies the Pokédex filters required for missing-spawnable extraction.
       *
       * This function is allowed to modify PCG filters only during a user-requested
       * Pokédex update run. Passive observers may read the DOM, but they must not
       * click filters or change the user's current PCG view.
       */
      async function ensureWantedFilters() {
        if (!canModifyPokedexFilters()) {
          reportBlockedPokedexFilterChange("ensure_wanted_filters_without_manual_update");

          return {
            blocked: true,
            spawnable: null,
            obtained: null
          };
        }

        const spawnableResult = await setFilterState("Spawnable", true);
        const obtainedResult = await setFilterState("Obtained", false);

        await sleep(900);

        return {
          blocked: false,
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
      const found = isRealPcgFrame() && looksLikePokedex();

      send("pcg_probe_progress", {
        step: 9200,
        phase: "pokedex_wait_for_dom",
        attempt: i + 1,
        maxAttempts,
        found,
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
        frame: frameInfo()
      });
      return;
    }

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

    send("pcg_probe_progress", {
      step: 9250,
      phase: "pokedex_dom_found_applying_filters",
      triggerReason,
      host: location.host,
      href: location.href
    });

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

   /**
    * Returns true when the current PCG frame looks like the Pokédex surface.
    *
    * This detector must stay read-only. It intentionally uses several weak signals
    * because PCG can render the Pokédex progressively: filters may appear before
    * entries, entries may appear after loading, and text casing/accents can vary.
    */
   function isPokedexTabVisible() {
     if (!isRealPcgFrame()) {
       return false;
     }

     if (looksLikePokedex()) {
       return true;
     }

     if (
       getFilterLabelByText("Spawnable") ||
       getFilterLabelByText("Obtained") ||
       getFilterLabelByText("Starters") ||
       getFilterLabelByText("Legendaries")
     ) {
       return true;
     }

     const bodyText = compactText(
       document.body ? document.body.innerText || document.body.textContent || "" : "",
       4000
     ).toLowerCase();

     return (
       bodyText.includes("spawnable") ||
       bodyText.includes("obtained") ||
       bodyText.includes("pokedex") ||
       bodyText.includes("pokédex")
     );
   }

    /**
     * Reports the current PCG tab state using the existing Android tab-state contract.
     *
     * GeckoSessionManager already listens for "pcg_tab_state" and uses it to enable
     * or disable the manual Inventory/Pokédex buttons.
     */
    function reportPokedexTabStateIfChanged(force) {
      const visible = isPokedexTabVisible();

      if (!force && lastReportedPokedexTabVisible === visible) {
        return;
      }

      lastReportedPokedexTabVisible = visible;

      send("pcg_tab_state", {
        activeTab: visible ? "pokedex" : "unknown",
        pokedexVisible: visible,
        inventoryVisible: false,
        anyLoadedSurface: visible,
        frame: frameInfo()
      });

      send("pcg_probe_progress", {
        step: 9400,
        phase: "pokedex_tab_state_reported",
        visible,
        activeTab: visible ? "pokedex" : "unknown",
        host: location.host,
        href: location.href
      });
    }

      /**
       * Runs a user-requested Pokédex update.
       *
       * This is the only high-level path where the probe may temporarily modify PCG
       * filters. The user must already be in the Pokédex tab; the probe must not
       * navigate to Pokédex by itself.
       */
      async function runUserRequestedPokedexUpdate(runId) {
        if (activePokedexUpdateRunId !== null) {
          send("pcg_pokedex_update_error", {
            runId,
            message: "Pokédex update already running",
            frame: frameInfo()
          });
          return;
        }

        if (!isPokedexTabVisible()) {
          send("pcg_pokedex_update_error", {
            runId,
            message: "Open the Pokédex tab before updating",
            frame: frameInfo()
          });
          return;
        }

        activePokedexUpdateRunId = runId;

        try {
          send("pcg_pokedex_update_started", {
            runId,
            frame: frameInfo()
          });

          await runExtraction("manual_pokedex_update");

          send("pcg_pokedex_update_finished", {
            runId,
            frame: frameInfo()
          });
        } catch (e) {
          send("pcg_pokedex_update_error", {
            runId,
            message: String(e),
            frame: frameInfo()
          });
        } finally {
          activePokedexUpdateRunId = null;
          reportPokedexTabStateIfChanged(true);
        }
      }

        let commandPollRunning = false;

        /**
         * Checks whether Android has a pending user-requested Pokédex update.
         *
         * This keeps the update tied to the Android button without requiring Android
         * to initiate a direct message into the PCG page.
         */
        pollAndroidCommandOnce

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

  try {
    send("pcg_probe_progress", {
      step: 9480,
      phase: "pokedex_startup_before_final_block",
      isRealFrame: isRealPcgFrame(),
      frame: frameInfo()
    });

    if (isRealPcgFrame()) {
      reportPokedexTabStateIfChanged(true);
    }

    const observer = new MutationObserver(() => {
      if (isRealPcgFrame()) {
        reportPokedexTabStateIfChanged(false);
      }
    });

    observer.observe(document.documentElement, {
      childList: true,
      subtree: true
    });

    send("pcg_probe_progress", {
      step: 9481,
      phase: "pokedex_observer_started",
      frame: frameInfo()
    });

    setInterval(() => {
      send("pcg_probe_progress", {
        step: 9482,
        phase: "pokedex_interval_tick",
        isRealFrame: isRealPcgFrame(),
        pokedexVisible: isPokedexTabVisible(),
        frame: frameInfo()
      });

      reportPokedexTabStateIfChanged(false);
      pollAndroidCommandOnce();
    }, 750);

    send("pcg_probe_progress", {
      step: 9483,
      phase: "pokedex_polling_started",
      frame: frameInfo()
    });
  } catch (e) {
    send("pcg_probe_progress", {
      step: 9489,
      phase: "pokedex_startup_exception",
      error: String(e),
      frame: frameInfo()
    });
  }
})();