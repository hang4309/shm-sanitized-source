function createPendingError(message) {
  const error = new Error(message);
  error.code = "API_NOT_READY";
  return Promise.reject(error);
}

export function getCrackLatest() {
  return createPendingError("裂缝模块 latest 接口未定稿，当前仅保留前端页面壳。");
}

export function getCrackHistory() {
  return createPendingError("裂缝模块 history 接口未定稿，当前仅保留前端页面壳。");
}

export function postCrackVerify() {
  return createPendingError("裂缝模块 verify 接口未定稿，当前仅保留前端页面壳。");
}
