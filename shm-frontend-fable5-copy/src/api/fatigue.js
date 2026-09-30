import axios from "axios";

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

  return (token ? { Authorization: `Bearer ${token}` } : {});
}

export function getFatigueLatest(params) {
  return axios.get("/api/data/fatigue/latest", {
    params,
    headers: buildHeaders(),
  });
}

export function getFatigueHistory(params) {
  return axios.get("/api/data/fatigue/history", {
    params,
    headers: buildHeaders(),
  });
}
