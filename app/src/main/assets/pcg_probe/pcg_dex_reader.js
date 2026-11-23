(function () {
  const NATIVE_APP = "pcgprobe";

  if (window.__pcgDexReaderLoaded) {
    return;
  }
  window.__pcgDexReaderLoaded = true;

  /*
   * Passive reader of the PCG Pokédex page.
   *
   * It reads the page as the user has set it, and does nothing else: it never clicks,
   * scrolls, types, focuses or changes a filter, and it sends nothing to PCG or Twitch.
   * Once a second, while the Pokédex markup exists, it reports to Android:
   *
   * - which dex the page shows, from label.alt-dex-label > span, and whether it is active;
   * - the live state of every filter checkbox, and whether the search box is empty;
   * - the counter's text and the number of entries;
   * - in the alt dex only, each entry's name, sprite number and grayscale flag.
   *
   * Android decides everything else: whether a reading is accepted, and when. It accepts
   * one only after the user taps "Update Alt Dex". content.js, pcg_tab_state_scout.js and
   * pcg_probe_inventory.js do not depend on this script, and it does not change them.
   */
  const READ_INTERVAL_MS = 1000;

  /** More than the alt dex's 371 forms; a larger list is not sent at all. */
  const MAX_ENTRIES = 2000;

  function isRealPcgFrame() {
    const host = (location.host || "").toLowerCase();
    return host.endsWith(".ext-twitch.tv") && !host.startsWith("supervisor.");
  }

  function send(type, payload) {
    try {
      browser.runtime.sendNativeMessage(NATIVE_APP, {
        type,
        payload
      });
    } catch {
      console.error("PCG dex reader send failed");
    }
  }

  function compactText(value, maxLen) {
    return String(value || "")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, maxLen);
  }

  function readDexLabel() {
    const span = document.querySelector("label.alt-dex-label > span");
    if (!span) {
      return { found: false, text: "", active: false };
    }

    return {
      found: true,
      text: compactText(span.textContent, 40),
      active: span.classList.contains("active")
    };
  }

  /** Each filter's label text and its checkbox's live "checked" property. */
  function readFilters() {
    return Array.from(document.querySelectorAll(".pokedex__filter-row label")).map(label => {
      const input = label.querySelector('input[type="checkbox"]');
      return {
        name: compactText(label.textContent, 60),
        hasCheckbox: !!input,
        checked: !!(input && input.checked)
      };
    });
  }

  /** Only whether the search box holds any text; the text itself is not sent. */
  function readSearch() {
    const input = document.querySelector(".pokedex__search");
    if (!input) {
      return { found: false, empty: true };
    }

    return { found: true, empty: String(input.value || "").length === 0 };
  }

  function readCounterText() {
    const counter = document.querySelector(".pokedex__counter");
    return counter ? compactText(counter.textContent, 40) : "";
  }

  function readEntries(entryElements) {
    return entryElements.map(entry => {
      const nameElement = entry.querySelector(".pokedex__entry-name");
      const sprite = entry.querySelector(".pokemon-entry-button__sprite");
      const style = sprite ? sprite.getAttribute("style") || "" : "";
      const number = style.match(/\/pokemon\/(\d+)\.png/);

      return {
        name: nameElement ? compactText(nameElement.textContent, 80) : "",
        number: number ? Number(number[1]) : -1,
        grayscale: !!(sprite && sprite.classList.contains("grayscale"))
      };
    });
  }

  function readPage() {
    if (!document.querySelector(".pokedex__container")) {
      return null;
    }

    const label = readDexLabel();
    const search = readSearch();
    const counterText = readCounterText();
    const entryElements = Array.from(document.querySelectorAll(".pokedex__entry"));
    const altDexShown = label.found && label.active && label.text.toLowerCase() === "alt dex";
    const entriesIncluded = altDexShown && entryElements.length <= MAX_ENTRIES;

    return {
      dexLabelFound: label.found,
      dexLabel: label.text,
      dexLabelActive: label.active,
      filters: readFilters(),
      searchFound: search.found,
      searchEmpty: search.empty,
      counterFound: counterText.length > 0,
      counterText,
      entryCount: entryElements.length,
      entriesIncluded,
      entries: entriesIncluded ? readEntries(entryElements) : [],
      readAtMs: Date.now()
    };
  }

  /*
   * One read, then the next one second after it ends, so two consecutive reads are always
   * at least a second apart. A read that throws is skipped, never retried at once.
   */
  function tick() {
    try {
      const page = readPage();
      if (page) {
        send("pcg_dex_page_read", page);
      }
    } catch {
      console.error("PCG dex reader read failed");
    }

    window.setTimeout(tick, READ_INTERVAL_MS);
  }

  if (isRealPcgFrame()) {
    window.setTimeout(tick, READ_INTERVAL_MS);
  }
})();
