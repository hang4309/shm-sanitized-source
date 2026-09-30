<template>
  <div class="module-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ config.label }}</h2>
        <p class="page-subtitle">{{ config.desc }}</p>
      </div>
      <div class="page-badge">{{ config.key }}</div>
    </div>

    <QueryBar
      v-model="queryForm"
      :loading="loading"
      :error-message="errorMessage"
      @query="handleManualQuery"
    />

    <div class="mode-bar">
      <span class="mode-tag" :class="{ live: liveMode }">
        {{ liveMode
          ? `实时模式：每 ${(refreshIntervalMs / 1000).toFixed(1)}s 自动刷新，取最近 ${LIVE_LIMIT} 条`
          : "手动时间窗口模式（已暂停自动跟随最新数据）" }}
      </span>
      <button v-if="!liveMode" type="button" class="mode-btn" @click="resumeLiveMode">
        恢复实时模式
      </button>
    </div>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">最新数据</h3>
        <div class="section-tags">
          <span v-if="isRefreshing" class="section-tag section-tag-refreshing">刷新中...</span>
          <span v-if="lastRefreshTime" class="section-tag">前端刷新于 {{ lastRefreshTime }}</span>
          <span v-if="displayLagText" class="section-tag">显示延迟 {{ displayLagText }}</span>
          <span class="section-tag">latest</span>
        </div>
      </div>

      <div v-if="latestData" class="data-grid">
        <div class="data-item">
          <div class="data-label">传感器ID</div>
          <div class="data-value">{{ latestData.sensorId || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">测值（主值）</div>
          <div class="data-value data-value-primary">{{ formatOs265Value(latestPrimaryValue) }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">波长（辅助参考）</div>
          <div class="data-value">{{ formatOs265Value(latestWavelength) }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">通道</div>
          <div class="data-value">{{ latestData.channelNo || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">采集时间</div>
          <div class="data-value">{{ latestData.collectTime || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">显示延迟（前端当前时间 − 采集时间）</div>
          <div class="data-value">{{ displayLagText || "-" }}</div>
        </div>
        <div class="data-item data-item-wide">
          <div class="data-label">说明</div>
          <div class="data-value data-value-note">
            延迟包含厂商软件写盘节奏；OS265 按批次落盘时，该值会周期性回落。
          </div>
        </div>
      </div>

      <div v-else class="empty-block">
        {{ loading ? "最新数据加载中..." : "该模块暂无已映射通道数据" }}
      </div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">历史趋势图（测值）</h3>
        <span class="section-tag">{{ chartPoints.length }} 点</span>
      </div>

      <div v-if="chartPoints.length > 0" class="chart-box">
        <svg class="trend-chart" viewBox="0 0 1000 320" preserveAspectRatio="none">
          <line x1="60" y1="24" x2="60" y2="270" class="axis-line" />
          <line x1="60" y1="270" x2="960" y2="270" class="axis-line" />

          <polyline v-if="chartPoints.length > 1" :points="chartPolyline" class="chart-polyline" />

          <circle
            v-for="point in chartPoints"
            :key="point.key"
            :cx="point.x"
            :cy="point.y"
            r="4"
            class="chart-point"
          >
            <title>{{ point.collectTime }} 测值 {{ formatOs265Value(point.value) }}（辅助波长 {{ formatOs265Value(point.wavelength) }}）</title>
          </circle>

          <text x="20" y="20" class="chart-title-text">测值（OS265 主值）</text>
          <text x="952" y="308" class="chart-title-text" text-anchor="end">采集时间</text>
          <text x="64" y="290" class="chart-x-label">{{ firstPointTime }}</text>
          <text x="956" y="290" class="chart-x-label" text-anchor="end">{{ lastPointTime }}</text>
          <text x="6" y="44" class="chart-x-label">{{ formatOs265Value(chartBounds.max) }}</text>
          <text x="6" y="254" class="chart-x-label">{{ formatOs265Value(chartBounds.min) }}</text>
        </svg>

        <div class="chart-meta">
          <span>点数：{{ chartPoints.length }}</span>
          <span>最小值：{{ chartStats.min }}</span>
          <span>最大值：{{ chartStats.max }}</span>
          <span>Y 轴最小可视跨度 0.05，避免视觉塌缩（不改变数据）</span>
          <span v-if="sourceIsConstant">当前窗口内源数据恒定，平线为真实数据</span>
          <span v-if="sourcePrecisionNote">{{ sourcePrecisionNote }}</span>
          <span>波长仅作辅助参考，不参与主图</span>
        </div>
      </div>

      <div v-else class="empty-block">
        {{ loading ? "历史趋势图加载中..." : "该模块暂无历史趋势数据" }}
      </div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">历史明细</h3>
        <span class="section-tag">共 {{ historySeries.length }} 条</span>
      </div>

      <div v-if="historySeries.length > 0" class="table-wrapper">
        <table class="history-table">
          <thead>
            <tr>
              <th>传感器ID</th>
              <th>测值（主值）</th>
              <th>波长（辅助）</th>
              <th>采集时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in historySeries" :key="item.key">
              <td>{{ item.sensorId || "-" }}</td>
              <td>{{ formatOs265Value(item.value) }}</td>
              <td>{{ formatOs265Value(item.wavelength) }}</td>
              <td>{{ item.collectTime || "-" }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-block">
        {{ loading ? "历史明细加载中..." : "该模块暂无历史明细" }}
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import QueryBar from "@/components/monitor/QueryBar.vue";
import {
  fetchModuleHistory,
  fetchModuleLatest,
  isRequestCanceled,
  unwrapResultData,
} from "@/api/monitorData";
import { getModuleConfig } from "@/utils/monitor/moduleConfig";
import {
  buildOs265NumericSeries,
  countSourceDecimals,
  formatOs265Value,
  pickOs265AuxiliaryWavelength,
  pickOs265PrimaryValue,
} from "@/utils/os265Value";
import {
  buildChartPoints,
  buildPolyline,
  formatShortTime,
  getChartBounds,
} from "@/utils/monitor/chartGeometry";

defineOptions({
  name: "MonitorModulePage",
});

// Live-mode defaults: 1 s auto refresh over the newest 200 rows. The
// interval is configurable per module via config.refreshIntervalMs.
const LIVE_REFRESH_INTERVAL_MS = 1000;
const LIVE_LIMIT = 200;

const props = defineProps({
  moduleKey: {
    type: String,
    required: true,
  },
});

const config = computed(
  () => getModuleConfig(props.moduleKey) || { key: props.moduleKey, label: props.moduleKey, desc: "" },
);

const refreshIntervalMs = computed(
  () => Number(config.value.refreshIntervalMs) || LIVE_REFRESH_INTERVAL_MS,
);

const loading = ref(false);
const errorMessage = ref("");
const latestData = ref(null);
const historyList = ref([]);
const lastRefreshTime = ref("");
const displayLagSeconds = ref(null);
// Live mode polls the newest N rows without a time window; the manual
// time-window query is a fallback path, never the live default.
const liveMode = ref(true);
let refreshTimer = null;

// Independent in-flight protection for latest and history: a request
// type is skipped (never queued) while its previous request is still
// in flight. Reactive so the UI can show a subtle refreshing hint.
const latestInFlight = ref(false);
const historyInFlight = ref(false);
const isRefreshing = computed(() => latestInFlight.value || historyInFlight.value);

// AbortControllers cancel in-flight requests on unmount; sequence
// numbers and the mounted flag drop stale or post-unmount responses.
let latestAbort = null;
let historyAbort = null;
let latestSeq = 0;
let historySeq = 0;
let isMounted = false;

const queryForm = ref({
  sensorId: config.value.defaultSensorId || "",
  startTime: getMinutesAgoTime(10),
  endTime: getCurrentTime(),
  limit: LIVE_LIMIT,
});

const timePattern = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/;

const latestPrimaryValue = computed(() => pickOs265PrimaryValue(latestData.value));
const latestWavelength = computed(() => pickOs265AuxiliaryWavelength(latestData.value));

const historySeries = computed(() => buildOs265NumericSeries(historyList.value));

const chartPoints = computed(() =>
  buildChartPoints(historySeries.value, { startX: 90, endX: 930, startY: 40, endY: 250 }),
);

const chartPolyline = computed(() => buildPolyline(chartPoints.value));

const chartStats = computed(() => {
  if (historySeries.value.length === 0) {
    return { min: "-", max: "-" };
  }
  const values = historySeries.value.map((item) => item.value);
  return {
    min: formatOs265Value(Math.min(...values)),
    max: formatOs265Value(Math.max(...values)),
  };
});

const chartBounds = computed(() =>
  getChartBounds(historySeries.value.map((item) => item.value)),
);

// Honest flat-line reporting: when every visible value is identical the
// chart stays flat and we say so instead of inventing movement.
const sourceIsConstant = computed(() => {
  if (historySeries.value.length < 2) {
    return false;
  }
  const values = historySeries.value.map((item) => item.value);
  return Math.min(...values) === Math.max(...values);
});

// True source precision: the OS265 channel txt often carries only one
// decimal for the intensity column even when the vendor UI shows more.
const sourcePrecisionNote = computed(() => {
  if (historySeries.value.length === 0) {
    return "";
  }
  const maxDecimals = Math.max(
    ...historySeries.value.map((item) => countSourceDecimals(item.value)),
  );
  if (maxDecimals <= 1) {
    return `源文件测值精度约 0.1（仅 ${maxDecimals} 位小数，厂商软件内部显示更高精度但未写入文件）`;
  }
  return "";
});

const displayLagText = computed(() => {
  if (displayLagSeconds.value === null) {
    return "";
  }
  return `${displayLagSeconds.value.toFixed(0)} s`;
});

const firstPointTime = computed(() => formatShortTime(historySeries.value[0]?.collectTime));
const lastPointTime = computed(() =>
  formatShortTime(historySeries.value[historySeries.value.length - 1]?.collectTime),
);

onMounted(() => {
  isMounted = true;
  loadModuleData({ silent: false });
  startAutoRefresh();
});

onBeforeUnmount(() => {
  // Abort in-flight requests and invalidate their sequence numbers so
  // nothing updates state after unmount; intentional aborts are quiet.
  isMounted = false;
  stopAutoRefresh();
  latestSeq += 1;
  historySeq += 1;
  if (latestAbort) {
    latestAbort.abort();
    latestAbort = null;
  }
  if (historyAbort) {
    historyAbort.abort();
    historyAbort = null;
  }
});

function pad(value) {
  return String(value).padStart(2, "0");
}

function formatDateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function getMinutesAgoTime(minutesAgo) {
  const now = new Date();
  now.setMinutes(now.getMinutes() - minutesAgo);
  return formatDateTime(now);
}

function getCurrentTime() {
  return formatDateTime(new Date());
}

function getCurrentClockTime() {
  const now = new Date();
  return `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

/**
 * Parse "yyyy-MM-dd HH:mm:ss" explicitly as LOCAL time. The whole
 * chain (vendor file, MySQL, Spring Boot JSON) uses local time; letting
 * the Date constructor guess could introduce false multi-hour lag.
 */
function parseLocalDateTime(text) {
  const match = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.exec(String(text || ""));
  if (!match) {
    return null;
  }
  return new Date(
    Number(match[1]),
    Number(match[2]) - 1,
    Number(match[3]),
    Number(match[4]),
    Number(match[5]),
    Number(match[6]),
  );
}

function updateDisplayLag() {
  const collectDate = parseLocalDateTime(latestData.value?.collectTime);
  displayLagSeconds.value =
    collectDate === null ? null : Math.max(0, (Date.now() - collectDate.getTime()) / 1000);
}

function validateQuery() {
  const form = queryForm.value;

  if (!timePattern.test(form.startTime)) {
    errorMessage.value = "开始时间格式必须为 yyyy-MM-dd HH:mm:ss";
    return false;
  }
  if (!timePattern.test(form.endTime)) {
    errorMessage.value = "结束时间格式必须为 yyyy-MM-dd HH:mm:ss";
    return false;
  }
  if (!Number.isInteger(Number(form.limit)) || Number(form.limit) <= 0) {
    errorMessage.value = "查询条数必须为正整数";
    return false;
  }
  if (form.startTime > form.endTime) {
    errorMessage.value = "开始时间不能大于结束时间";
    return false;
  }

  errorMessage.value = "";
  return true;
}

async function handleManualQuery() {
  // The time-window query is a manual fallback mode, never the live path.
  liveMode.value = false;
  await loadModuleData({ silent: false });
}

async function resumeLiveMode() {
  liveMode.value = true;
  queryForm.value = { ...queryForm.value, limit: LIVE_LIMIT };
  await loadModuleData({ silent: false });
}

function buildPayloads() {
  const form = queryForm.value;
  // sensorId is an optional filter: empty means the whole module.
  const sensorId = String(form.sensorId || "").trim();
  const basePayload = sensorId ? { sensorId } : {};

  // Live mode: newest N rows, no start/end time, so fresh rows are
  // never filtered away by a stale client clock window.
  const historyPayload = liveMode.value
    ? { ...basePayload, limit: LIVE_LIMIT }
    : {
        ...basePayload,
        startTime: form.startTime,
        endTime: form.endTime,
        limit: Number(form.limit),
      };

  return { basePayload, historyPayload };
}

function reportRequestError(error) {
  if (isRequestCanceled(error)) {
    return;
  }
  errorMessage.value =
    error?.response?.data?.message || error?.message || "监测模块数据查询失败";
}

async function refreshLatest(payload) {
  if (latestInFlight.value) {
    console.log("Skipped latest request because previous latest request is still in flight.");
    return false;
  }
  latestInFlight.value = true;
  const seq = ++latestSeq;
  latestAbort = new AbortController();
  try {
    const response = await fetchModuleLatest(props.moduleKey, payload, {
      signal: latestAbort.signal,
    });
    // Drop stale or post-unmount responses: only the newest latest
    // request of this component instance may update the UI.
    if (!isMounted || seq !== latestSeq) {
      return false;
    }
    latestData.value = normalizeObject(unwrapResultData(response));
    updateDisplayLag();
    return true;
  } catch (error) {
    if (isMounted && seq === latestSeq) {
      reportRequestError(error);
    }
    return false;
  } finally {
    latestInFlight.value = false;
  }
}

async function refreshHistory(payload) {
  if (historyInFlight.value) {
    console.log("Skipped history request because previous history request is still in flight.");
    return false;
  }
  historyInFlight.value = true;
  const seq = ++historySeq;
  historyAbort = new AbortController();
  try {
    const response = await fetchModuleHistory(props.moduleKey, payload, {
      signal: historyAbort.signal,
    });
    if (!isMounted || seq !== historySeq) {
      return false;
    }
    historyList.value = normalizeArray(unwrapResultData(response));
    return true;
  } catch (error) {
    if (isMounted && seq === historySeq) {
      reportRequestError(error);
    }
    return false;
  } finally {
    historyInFlight.value = false;
  }
}

async function loadModuleData({ silent }) {
  if (!liveMode.value && !validateQuery()) {
    return;
  }

  if (!silent) {
    loading.value = true;
  }

  try {
    const { basePayload, historyPayload } = buildPayloads();

    // Latest and history are protected independently and never block
    // each other; a skipped request is dropped, not queued.
    const [latestOk, historyOk] = await Promise.all([
      refreshLatest(basePayload),
      refreshHistory(historyPayload),
    ]);

    if (isMounted && (latestOk || historyOk)) {
      lastRefreshTime.value = getCurrentClockTime();
      errorMessage.value = "";
    }
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}

function startAutoRefresh() {
  // Always clear first so module switches never stack multiple timers.
  stopAutoRefresh();
  refreshTimer = window.setInterval(() => {
    if (liveMode.value) {
      loadModuleData({ silent: true });
    }
  }, refreshIntervalMs.value);
}

function stopAutoRefresh() {
  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer);
    refreshTimer = null;
  }
}

function normalizeObject(data) {
  if (Array.isArray(data)) {
    return data.length > 0 ? data[0] : null;
  }
  if (data && typeof data === "object") {
    return data;
  }
  return null;
}

function normalizeArray(data) {
  return Array.isArray(data) ? data : [];
}
</script>

<style scoped>
.module-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  color: #111827;
}

.page-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.page-badge {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #111827;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.mode-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.mode-tag {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: #fef3c7;
  color: #92400e;
  font-size: 13px;
  font-weight: 600;
}

.mode-tag.live {
  background: #dcfce7;
  color: #166534;
}

.mode-btn {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 999px;
  background: #ffffff;
  color: #374151;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.mode-btn:hover {
  background: #f3f4f6;
}

.data-value-note {
  font-size: 13px;
  color: #6b7280;
}

.section-card {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  box-sizing: border-box;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.section-tag {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
}

.section-tag-refreshing {
  background: #dbeafe;
  color: #1d4ed8;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.data-item {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
}

.data-item-wide {
  grid-column: span 4;
}

.data-label {
  margin-bottom: 8px;
  font-size: 12px;
  color: #6b7280;
}

.data-value {
  font-size: 16px;
  color: #111827;
  word-break: break-all;
}

.data-value-primary {
  font-weight: 700;
}

.empty-block {
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d1d5db;
  border-radius: 12px;
  background: #fafafa;
  color: #6b7280;
  font-size: 14px;
  text-align: center;
}

.chart-box {
  width: 100%;
}

.trend-chart {
  display: block;
  width: 100%;
  height: 320px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fcfcfd;
}

.axis-line {
  stroke: #c9cdd4;
  stroke-width: 1;
}

.chart-polyline {
  fill: none;
  stroke: #2563eb;
  stroke-width: 2.5;
}

.chart-point {
  fill: #111827;
}

.chart-x-label {
  font-size: 11px;
  fill: #4b5563;
}

.chart-title-text {
  font-size: 12px;
  fill: #6b7280;
}

.chart-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
  font-size: 13px;
  color: #4b5563;
}

.table-wrapper {
  overflow-x: auto;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.history-table th,
.history-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  font-size: 14px;
  color: #111827;
  word-break: break-all;
}

.history-table th {
  background: #fafafa;
  color: #374151;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .data-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .data-item-wide {
    grid-column: span 2;
  }
}

@media (max-width: 900px) {
  .page-header,
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .section-tags {
    justify-content: flex-start;
  }

  .data-grid {
    grid-template-columns: 1fr;
  }

  .data-item-wide {
    grid-column: span 1;
  }
}
</style>
