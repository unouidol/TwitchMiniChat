(function () {
  const NATIVE_APP = "pcgprobe";

  if (window.__pcgTabStateScoutLoaded) {
    return;
  }
  window.__pcgTabStateScoutLoaded = true;

  /*
   * Fast PCG tab-state scout.
   *
   * This script is the only source of truth for Android's manual update buttons.
   *
   * It does not extract Inventory data.
   * It does not extract Pokédex data.
   * It only reports which PCG tab appears selected right now:
   *
   * - inventory
   * - pokedex
   * - other
   * - unknown
   */
  const TAB_STATE_HEARTBEAT_MS = 400;

  let lastSentSignature = "";

  function send(type, payload) {
    try {
      browser.runtime.sendNativeMessage(NATIVE_APP, {
        type,
        payload
      });
    } catch {
      console.error("PCG tab-state scout send failed");
    }
  }

  function compactText(text, maxLen = 160) {
    return (text || "")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, maxLen);
  }

  function normalizeText(text) {
    return compactText(text, 160)
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "");
  }

  function isRealPcgFrame() {
    const host = (location.host || "").toLowerCase();
    return host.endsWith(".ext-twitch.tv") && !host.startsWith("supervisor.");
  }

  function elementLooksVisible(el) {
    if (!el) return false;

    const rect = el.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) return false;

    const style = window.getComputedStyle(el);
    return (
      style.display !== "none" &&
      style.visibility !== "hidden" &&
      Number(style.opacity || "1") > 0
    );
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

  /**
   * Finds visible tab-like controls that PCG marks as selected.
   *
   * Important: this function intentionally does not look at Inventory grids or
   * Pokédex content as fallback evidence. PCG can keep inactive tab contents
   * mounted in the DOM, so content presence is not reliable enough for buttons.
   */
  function findSelectedPcgTabText() {
    const candidates = Array.from(document.querySelectorAll([
      "[role='tab']",
      "button",
      "a",
      ".tab",
      ".tabs__tab",
      ".nav-link",
      ".nav-item",
      ".navigation__item",
      "[class*='tab']",
      "[class*='nav']"
    ].join(",")));

    const selectedTexts = [];

    for (const el of candidates) {
      if (!elementLooksVisible(el)) continue;
      if (!elementLooksSelected(el)) continue;

      const text = compactText(el.innerText || el.textContent || "", 160);
      if (text) {
        selectedTexts.push(text);
      }
    }

    const inventoryText = selectedTexts.find(text =>
      normalizeText(text).includes("inventory")
    );

    if (inventoryText) return inventoryText;

    const pokedexText = selectedTexts.find(text =>
      normalizeText(text).includes("pokedex")
    );

    if (pokedexText) return pokedexText;

    return selectedTexts[0] || "";
  }

/**
 * Finds the Inventory ball grid using the same broad markers used by the
 * Inventory extractor.
 *
 * This is only used as a tab-state fallback when selected tab controls are not
 * exposed clearly by PCG.
 */
function findInventoryBallGrid() {
  const headings = Array.from(document.querySelectorAll("h1,h2,h3,h4,h5,h6,.category-heading"));

  for (const heading of headings) {
    const text = compactText(heading.innerText || heading.textContent || "", 80);
    if (!/^ball$/i.test(text)) continue;

    let sibling = heading.nextElementSibling;
    while (sibling) {
      if (sibling.matches && sibling.matches(".item-grid")) {
        return sibling;
      }
      sibling = sibling.nextElementSibling;
    }
  }

  const grid = document.querySelector(".item-grid");
  if (!grid) return null;

  const ballCards = grid.querySelectorAll(".item-entry-button__name");
  for (const cardName of ballCards) {
    const text = compactText(cardName.innerText || cardName.textContent || "", 80);
    if (/ball$/i.test(text) || /^poke ball$/i.test(text) || /^poké ball$/i.test(text)) {
      return grid;
    }
  }

  return null;
}

/**
 * Returns true when the Pokédex DOM appears to be readable.
 *
 * This mirrors the passive Pokédex collector's broad DOM check, but does not
 * read or scroll entries.
 */
function looksLikePokedexDomExists() {
  return !!document.querySelector(
    ".pokedex__container, .pokedex__grid, .pokedex__entry, .pokedex__entry-name"
  );
}

  function detectTabState() {
    if (!isRealPcgFrame()) {
      return {
        activeTab: "unknown",
        inventoryVisible: false,
        pokedexVisible: false,
        anyLoadedSurface: false,
        selectedText: "",
        detectionMode: "not_real_pcg_frame"
      };
    }

    const selectedText = findSelectedPcgTabText();
    const normalized = normalizeText(selectedText);

    /*
     * First choice: explicit selected tab controls.
     *
     * This is the cleanest signal when PCG exposes selected/active classes or
     * ARIA state on its tab buttons.
     */
    if (normalized.includes("inventory")) {
      return {
        activeTab: "inventory",
        inventoryVisible: true,
        pokedexVisible: false,
        anyLoadedSurface: true,
        selectedText,
        detectionMode: "selected_tab_control"
      };
    }

    if (normalized.includes("pokedex")) {
      return {
        activeTab: "pokedex",
        inventoryVisible: false,
        pokedexVisible: true,
        anyLoadedSurface: true,
        selectedText,
        detectionMode: "selected_tab_control"
      };
    }

    if (selectedText) {
      return {
        activeTab: "other",
        inventoryVisible: false,
        pokedexVisible: false,
        anyLoadedSurface: true,
        selectedText,
        detectionMode: "selected_tab_control"
      };
    }

    /*
     * Fallback: PCG does not expose a selected tab control clearly.
     *
     * In that case, use strong page-surface markers. If exactly one known surface
     * is readable, report that surface. If neither is readable, treat it as "other"
     * so Android disables both manual update buttons.
     */
    const inventoryGrid = findInventoryBallGrid();
    const inventoryFound = !!inventoryGrid;
    const pokedexFound = looksLikePokedexDomExists();

    if (inventoryFound && !pokedexFound) {
      return {
        activeTab: "inventory",
        inventoryVisible: true,
        pokedexVisible: false,
        anyLoadedSurface: true,
        selectedText: "",
        detectionMode: "inventory_grid_fallback"
      };
    }

    if (pokedexFound && !inventoryFound) {
      return {
        activeTab: "pokedex",
        inventoryVisible: false,
        pokedexVisible: true,
        anyLoadedSurface: true,
        selectedText: "",
        detectionMode: "pokedex_dom_fallback"
      };
    }

    if (inventoryFound && pokedexFound) {
      return {
        activeTab: "unknown",
        inventoryVisible: true,
        pokedexVisible: true,
        anyLoadedSurface: true,
        selectedText: "",
        detectionMode: "ambiguous_content_fallback"
      };
    }

    return {
      activeTab: "other",
      inventoryVisible: false,
      pokedexVisible: false,
      anyLoadedSurface: true,
      selectedText: "",
      detectionMode: "no_known_surface_found"
    };
  }

  function sendTabState(reason, force = false) {
    const state = detectTabState();

    const signature = [
      state.activeTab,
      state.inventoryVisible,
      state.pokedexVisible,
      state.anyLoadedSurface,
      state.selectedText || ""
    ].join("|");

    if (!force && signature === lastSentSignature && reason !== "heartbeat") {
      return;
    }

    lastSentSignature = signature;

    send("pcg_tab_state", {
      activeTab: state.activeTab,
      inventoryVisible: state.inventoryVisible,
      pokedexVisible: state.pokedexVisible,
      anyLoadedSurface: state.anyLoadedSurface,
      selectedText: state.selectedText || "",
      detectionMode: state.detectionMode || "",
      reason,
      href: location.href,
      host: location.host,
      readyState: document.readyState,
      capturedAtMs: Date.now()
    });
  }

  if (isRealPcgFrame()) {
    sendTabState("initial", true);

    window.setInterval(() => {
      sendTabState("heartbeat", true);
    }, TAB_STATE_HEARTBEAT_MS);

    const observer = new MutationObserver(() => {
      sendTabState("mutation", false);
    });

    observer.observe(document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: [
        "class",
        "aria-selected",
        "aria-current",
        "data-active",
        "style",
        "hidden"
      ]
    });
  }
})();
