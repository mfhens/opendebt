package dk.ufst.opendebt.common.timeline;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared URL-construction utilities for the timeline BFF controllers.
 *
 * <p>Extracted from the caseworker, citizen, and creditor portal timeline controllers to eliminate
 * identical private method duplication across all three.
 */
public final class TimelineUrlBuilder {

  private static final String PARAM_EVENT_CATEGORY = "eventCategory=";
  private static final String PARAM_FROM_DATE = "fromDate=";
  private static final String PARAM_TO_DATE = "toDate=";
  private static final String PARAM_DEBT_ID = "debtId=";

  private TimelineUrlBuilder() {}

  /** Builds the "load more" URL for the next page, preserving all active filter parameters. */
  public static String buildLoadMoreUrl(
      TimelineFilterDto filters, String entriesUrl, int page, int size) {
    StringBuilder sb = new StringBuilder(entriesUrl);
    sb.append("?page=").append(page + 1).append("&size=").append(size);
    for (EventCategory cat : filters.getEventCategories()) {
      sb.append("&").append(PARAM_EVENT_CATEGORY).append(cat.name());
    }
    if (filters.getFromDate() != null) {
      sb.append("&").append(PARAM_FROM_DATE).append(filters.getFromDate());
    }
    if (filters.getToDate() != null) {
      sb.append("&").append(PARAM_TO_DATE).append(filters.getToDate());
    }
    if (filters.getDebtId() != null) {
      sb.append("&").append(PARAM_DEBT_ID).append(filters.getDebtId());
    }
    return sb.toString();
  }

  /**
   * Builds per-filter removal URLs — each entry in the map is the URL that removes exactly that
   * filter while preserving all others. Keys: category enum names, "fromDate", "toDate", "debtId".
   * Ref: FIX-4.
   */
  public static Map<String, String> buildFilterRemoveLinks(
      TimelineFilterDto filters, String baseUrl) {
    Map<String, String> links = new HashMap<>();

    for (EventCategory removedCat : filters.getEventCategories()) {
      links.put(
          removedCat.name(), buildQueryString(baseUrl, filters, removedCat, true, true, true));
    }
    if (filters.getFromDate() != null) {
      links.put("fromDate", buildQueryString(baseUrl, filters, null, false, true, true));
    }
    if (filters.getToDate() != null) {
      links.put("toDate", buildQueryString(baseUrl, filters, null, true, false, true));
    }
    if (filters.getDebtId() != null) {
      links.put("debtId", buildQueryString(baseUrl, filters, null, true, true, false));
    }
    return links;
  }

  /**
   * Builds a query string from {@code baseUrl} including all active filter params, optionally
   * excluding one category, fromDate, toDate, or debtId.
   *
   * @param excludedCat category to omit (pass {@code null} to include all)
   * @param includeFrom whether to include fromDate
   * @param includeTo whether to include toDate
   * @param includeDebt whether to include debtId
   */
  private static String buildQueryString(
      String baseUrl,
      TimelineFilterDto filters,
      EventCategory excludedCat,
      boolean includeFrom,
      boolean includeTo,
      boolean includeDebt) {
    StringBuilder sb = new StringBuilder(baseUrl);
    boolean first = true;
    for (EventCategory cat : filters.getEventCategories()) {
      if (!cat.equals(excludedCat)) {
        sb.append(first ? "?" : "&").append(PARAM_EVENT_CATEGORY).append(cat.name());
        first = false;
      }
    }
    if (includeFrom && filters.getFromDate() != null) {
      sb.append(first ? "?" : "&").append(PARAM_FROM_DATE).append(filters.getFromDate());
      first = false;
    }
    if (includeTo && filters.getToDate() != null) {
      sb.append(first ? "?" : "&").append(PARAM_TO_DATE).append(filters.getToDate());
      first = false;
    }
    if (includeDebt && filters.getDebtId() != null) {
      sb.append(first ? "?" : "&").append(PARAM_DEBT_ID).append(filters.getDebtId());
    }
    return sb.toString();
  }
}
