import axios from "axios";

/**
 * Unified monitoring data API.
 *
 * The six business modules share the same endpoint shape:
 *   POST /api/data/{moduleKey}/latest
 *   POST /api/data/{moduleKey}/history
 * and the OS265/vendor source layer is queryable directly:
 *   POST /api/data/os265/latest
 *   POST /api/data/os265/history
 */

function getToken() {
  if (typeof window === "undefined") {
    return "";
  }
  return (
    window.localStorage.getItem("token") ||
    window.localStorage.getItem("accessToken") ||
    window.sessionStorage.getItem("token") ||
    window.sessionStorage.getItem("accessToken") ||
    ""
  );
}

function buildHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * options may carry { signal } from an AbortController so callers can
 * cancel in-flight requests on unmount.
 */
export function fetchModuleLatest(moduleKey, payload, options = {}) {
  return axios.post(`/api/data/${moduleKey}/latest`, payload, {
    headers: buildHeaders(),
    ...options,
  });
}

export function fetchModuleHistory(moduleKey, payload, options = {}) {
  return axios.post(`/api/data/${moduleKey}/history`, payload, {
    headers: buildHeaders(),
    ...options,
  });
}

/**
 * True for intentional cancellations (AbortController), which must not
 * be surfaced as user-facing errors or logged as failures.
 */
export function isRequestCanceled(error) {
  return (
    error?.code === "ERR_CANCELED" ||
    error?.name === "CanceledError" ||
    error?.name === "AbortError"
  );
}

export function fetchOs265Latest(payload) {
  return axios.post("/api/data/os265/latest", payload, {
    headers: buildHeaders(),
  });
}

export function fetchOs265History(payload) {
  return axios.post("/api/data/os265/history", payload, {
    headers: buildHeaders(),
  });
}

export function unwrapResultData(response) {
  const responseData = response?.data;
  if (responseData && typeof responseData === "object" && "data" in responseData) {
    return responseData.data;
  }
  return responseData;
}
