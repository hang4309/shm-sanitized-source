import axios from "axios";

function getToken() {
  if (typeof window === "undefined") return "";
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

export function postFiberRawUpload(data) {
  return axios.post("/api/sensor/fiber/raw/upload", data, { headers: buildHeaders() });
}

export function postFiberLatest(data) {
  return axios.post("/api/data/fiber/latest", data, { headers: buildHeaders() });
}

export function postFiberHistory(data) {
  return axios.post("/api/data/fiber/history", data, { headers: buildHeaders() });
}
