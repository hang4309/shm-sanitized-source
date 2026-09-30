function createPendingError(message) {
  const error = new Error(message);
  error.code = "API_NOT_READY";
  return Promise.reject(error);
}

export function getPredictionLatest() {
  return createPendingError("预测模块 latest 接口未定稿，当前仅保留前端页面壳。");
}

export function getPredictionHistory() {
  return createPendingError("预测模块 history 接口未定稿，当前仅保留前端页面壳。");
}

export function postPredictionVerify() {
  return createPendingError("预测模块 verify 接口未定稿，当前仅保留前端页面壳。");
}
