/**
 * a11y.js — Accessibility utilities for HTMX-driven pages.
 *
 * Manages keyboard focus after HTMX content swaps so screen-reader users
 * always land on a meaningful element when partial page updates occur.
 *
 * Strategy:
 *   1. After every HTMX swap, look for an element with [autofocus] or
 *      [data-a11y-focus] inside the swapped target.
 *   2. Fall back to the first <h1> inside the target.
 *   3. Fall back to the target element itself (with tabindex=-1 so it
 *      receives focus without appearing in the tab order).
 */
(function () {
  "use strict";

  document.addEventListener("htmx:afterSwap", function (event) {
    var target = event.detail.target;
    if (!target) return;

    var focusable =
      target.querySelector("[autofocus], [data-a11y-focus]") ||
      target.querySelector("h1");

    if (focusable) {
      focusable.focus();
    } else if (target !== document.body) {
      if (!target.hasAttribute("tabindex")) {
        target.setAttribute("tabindex", "-1");
      }
      target.focus();
    }
  });

  /* Announce live-region updates to screen readers after HTMX settles. */
  document.addEventListener("htmx:afterSettle", function (event) {
    var alerts = document.querySelectorAll('[role="alert"][data-htmx-alert]');
    alerts.forEach(function (el) {
      var msg = el.textContent.trim();
      if (msg) {
        el.removeAttribute("data-htmx-alert");
        el.setAttribute("aria-live", "assertive");
      }
    });
  });
})();
