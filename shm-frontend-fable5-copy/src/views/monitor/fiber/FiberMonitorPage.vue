<template>
  <div class="fiber-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">光纤原始监测</h2>
        <p class="page-subtitle">OS265 通道数据源</p>
      </div>
      <div class="page-badge">FiberMonitorPage</div>
    </div>

    <QueryBar v-model="queryForm" :loading="loading" :error-message="errorMessage" @query="handleQuery" />

    <section class="section-card">
      <div class="section-header">
        <div>
          <h3 class="section-title">OS265 实时测值曲线</h3>
          <p class="section-subtitle">最近 5 分钟，2 秒自动刷新；主曲线显示 OS265 软件测得的当前传感器值，波长保留为辅助参考。</p>
        </div>
        <span class="section-tag">realtime</span>
      </div>

      <div class="realtime-summary">
        <div class="data-item">
          <div class="data-label">当前 sensorId</div>
          <div class="data-value">{{ realtimeSensorId }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">当前测值</div>
          <div class="data-value">{{ latestPrimaryDisplay }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">最新采集时间</div>
          <div class="data-value">{{ latestData?.collectTime || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">原始点数</div>
          <div class="data-value">{{ chartRawSeries.length }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">曲线有效点数</div>
          <div class="data-value">{{ chartSeries.length }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">已过滤异常点数</div>
          <div class="data-value">{{ chartFilteredCount }}</div>
        </div>
      </div>

      <div v-if="realtimeErrorMessage" class="inline-error">{{ realtimeErrorMessage }}</div>

      <div v-if="chartPoints.length > 0" class="chart-box">
        <svg class="trend-chart" viewBox="0 0 1000 320" preserveAspectRatio="none" role="img" aria-label="OS265 实时测值曲线">
          <line x1="70" y1="28" x2="70" y2="270" class="axis-line" />
          <line x1="70" y1="270" x2="960" y2="270" class="axis-line" />

          <line
            v-for="label in chartYLabels"
            :key="label.key"
            x1="70"
            x2="960"
            :y1="label.y"
            :y2="label.y"
            class="grid-line"
          />
          <text
            v-for="label in chartYLabels"
            :key="`${label.key}-text`"
            x="62"
            :y="label.y + 4"
            class="chart-y-label"
            text-anchor="end"
          >
            {{ label.text }}
          </text>

          <polyline v-if="chartPoints.length > 1" :points="chartPolyline" class="chart-polyline" />

          <circle
            v-for="point in chartPoints"
            :key="point.key"
            :cx="point.x"
            :cy="point.y"
            r="2.5"
            class="chart-point"
          />

          <text
            v-for="point in chartLabelPoints"
            :key="`${point.key}-x`"
            :x="point.x"
            y="296"
            class="chart-x-label"
            text-anchor="middle"
          >
            {{ point.shortTime }}
          </text>

          <text x="18" y="20" class="chart-title-text">OS265 测值</text>
          <text x="954" y="312" class="chart-title-text" text-anchor="end">采集时间</text>
        </svg>

        <div class="chart-meta">
          <span>sensorId：{{ realtimeSensorId }}</span>
          <span>原始点数：{{ chartRawSeries.length }}</span>
          <span>有效点数：{{ chartSeries.length }}</span>
          <span>已过滤异常点：{{ chartFilteredCount }}</span>
          <span>中位数：{{ chartMedianDisplay }}</span>
          <span>最小值：{{ chartStats.min }}</span>
          <span>最大值：{{ chartStats.max }}</span>
          <span>刷新时间：{{ lastRefreshTime || "-" }}</span>
        </div>
      </div>

      <div v-else class="empty-block">
        {{ realtimeLoading ? "实时曲线加载中..." : "暂无实时测值曲线数据" }}
      </div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">最新数据</h3>
        <span class="section-tag">latest</span>
      </div>
      <div v-if="latestIsOutlier" class="warning-message">最新值疑似异常，请等待下一条正常采样。</div>
      <div v-if="latestData" class="data-grid">
        <div class="data-item">
          <div class="data-label">传感器ID</div>
          <div class="data-value">{{ latestData.sensorId || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">设备编号</div>
          <div class="data-value">{{ latestData.deviceNo || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">光纤编号</div>
          <div class="data-value">{{ latestData.fiberNo || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">当前测值</div>
          <div class="data-value">{{ formatDisplayValue(latestData.rawValue) }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">辅助波长</div>
          <div class="data-value">{{ formatDisplayValue(latestData.wavelength) }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">波长变化</div>
          <div class="data-value">{{ formatDisplayValue(latestData.wavelengthShift) }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">能量/测值</div>
          <div class="data-value">{{ formatDisplayValue(latestData.intensity) }}</div>
        </div>
        <div class="data-item data-item-wide">
          <div class="data-label">采集时间</div>
          <div class="data-value">{{ latestData.collectTime || "-" }}</div>
        </div>
      </div>
      <div v-else class="empty-block">{{ loading ? "加载中..." : "暂无数据" }}</div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">历史明细</h3>
        <span class="section-tag">共 {{ historyList.length }} 条</span>
      </div>
      <div v-if="historyList.length" class="table-wrapper">
        <table class="history-table">
          <thead>
            <tr>
              <th>传感器ID</th>
              <th>设备编号</th>
              <th>光纤编号</th>
              <th>当前测值</th>
              <th>辅助波长</th>
              <th>波长变化</th>
              <th>能量</th>
              <th>采集时间</th>
              <th>标记</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(item, index) in historyList"
              :key="`${item.sensorId || 'row'}-${index}`"
              :class="{ 'outlier-row': isHistoryOutlier(item) }"
            >
              <td>{{ item.sensorId || "-" }}</td>
              <td>{{ item.deviceNo || "-" }}</td>
              <td>{{ item.fiberNo || "-" }}</td>
              <td>{{ formatDisplayValue(item.rawValue) }}</td>
              <td>{{ formatDisplayValue(item.wavelength) }}</td>
              <td>{{ formatDisplayValue(item.wavelengthShift) }}</td>
              <td>{{ formatDisplayValue(item.intensity) }}</td>
              <td>{{ item.collectTime || "-" }}</td>
              <td>
                <span v-if="isHistoryOutlier(item)" class="outlier-badge">异常跳点</span>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty-block">{{ loading ? "加载中..." : "暂无历史数据" }}</div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import QueryBar from "@/components/monitor/QueryBar.vue";
import { postFiberHistory, postFiberLatest } from "@/api/fiber";

defineOptions({
  name: "FiberMonitorPage",
});

const DEFAULT_SENSOR_ID = "FBG-STRAIN-CH2";
const REALTIME_REFRESH_INTERVAL = 2000;
const REALTIME_WINDOW_MINUTES = 5;
const OUTLIER_THRESHOLD_NM = 1.0;
const MIN_CHART_RANGE_NM = 0.02;

const loading = ref(false);
const realtimeLoading = ref(false);
const errorMessage = ref("");
const realtimeErrorMessage = ref("");
const latestData = ref(null);
const historyList = ref([]);
const realtimeHistoryList = ref([]);
const lastRefreshTime = ref("");
let realtimeTimer = null;

const queryForm = ref({
  sensorId: DEFAULT_SENSOR_ID,
  startTime: getTodayStartTime(),
  endTime: getCurrentTime(),
  limit: 100,
});

const realtimeSensorId = computed(() => {
  return String(queryForm.value.sensorId || DEFAULT_SENSOR_ID).trim() || DEFAULT_SENSOR_ID;
});

const latestPrimaryDisplay = computed(() => {
  if (!latestData.value) {
    return "-";
  }

  const value = pickPrimaryValue(latestData.value);
  return formatDisplayValue(value);
});

const chartRawSeries = computed(() => {
  return buildNumericSeries(realtimeHistoryList.value);
});

const chartMedian = computed(() => {
  return calculateMedian(chartRawSeries.value.map((item) => item.value));
});

const chartMedianDisplay = computed(() => {
  return chartMedian.value === null ? "-" : formatNumber(chartMedian.value);
});

const chartSeries = computed(() => {
  if (chartMedian.value === null) {
    return chartRawSeries.value;
  }

  return chartRawSeries.value.filter((item) => !isOutlierValue(item.value, chartMedian.value));
});

const chartFilteredCount = computed(() => {
  return Math.max(chartRawSeries.value.length - chartSeries.value.length, 0);
});

const historyMedian = computed(() => {
  const series = buildNumericSeries(historyList.value);
  return calculateMedian(series.map((item) => item.value));
});

const latestIsOutlier = computed(() => {
  const value = pickPrimaryValue(latestData.value);

  if (value === null || chartMedian.value === null) {
    return false;
  }

  return isOutlierValue(value, chartMedian.value);
});

const chartBounds = computed(() => {
  if (chartSeries.value.length === 0) {
    return {
      min: 0,
      max: 1,
    };
  }

  const values = chartSeries.value.map((item) => item.value);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const range = maxValue - minValue;

  if (range < MIN_CHART_RANGE_NM) {
    const center = (minValue + maxValue) / 2;
    return {
      min: center - MIN_CHART_RANGE_NM / 2,
      max: center + MIN_CHART_RANGE_NM / 2,
    };
  }

  const padding = Math.max(range * 0.1, MIN_CHART_RANGE_NM / 2);

  return {
    min: minValue - padding,
    max: maxValue + padding,
  };
});

const chartStats = computed(() => {
  if (chartSeries.value.length === 0) {
    return {
      min: "-",
      max: "-",
    };
  }

  const values = chartSeries.value.map((item) => item.value);

  return {
    min: formatNumber(Math.min(...values)),
    max: formatNumber(Math.max(...values)),
  };
});

const chartPoints = computed(() => {
  if (chartSeries.value.length === 0) {
    return [];
  }

  const startX = 90;
  const endX = 930;
  const startY = 42;
  const endY = 252;
  const minValue = chartBounds.value.min;
  const maxValue = chartBounds.value.max;
  const range = maxValue - minValue || 1;
  const stepX = chartSeries.value.length > 1 ? (endX - startX) / (chartSeries.value.length - 1) : 0;

  return chartSeries.value.map((item, index) => {
    const x = chartSeries.value.length === 1 ? (startX + endX) / 2 : startX + index * stepX;
    const y = endY - ((item.value - minValue) / range) * (endY - startY);

    return {
      ...item,
      x,
      y,
    };
  });
});

const chartLabelPoints = computed(() => {
  const points = chartPoints.value;

  if (points.length <= 6) {
    return points;
  }

  const lastIndex = points.length - 1;
  const indexes = new Set([0, lastIndex]);

  for (let i = 1; i < 5; i += 1) {
    indexes.add(Math.round((lastIndex * i) / 5));
  }

  return points.filter((_, index) => indexes.has(index));
});

const chartYLabels = computed(() => {
  if (chartPoints.value.length === 0 || chartSeries.value.length === 0) {
    return [];
  }

  const minValue = chartBounds.value.min;
  const maxValue = chartBounds.value.max;
  const midValue = (minValue + maxValue) / 2;

  return [
    { key: "max", text: formatNumber(maxValue), y: 42 },
    { key: "mid", text: formatNumber(midValue), y: 147 },
    { key: "min", text: formatNumber(minValue), y: 252 },
  ];
});

const chartPolyline = computed(() => {
  return chartPoints.value.map((point) => `${point.x},${point.y}`).join(" ");
});

function pad(value) {
  return String(value).padStart(2, "0");
}

function formatDateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes(),
  )}:${pad(date.getSeconds())}`;
}

function getTodayStartTime() {
  const now = new Date();
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} 00:00:00`;
}

function getCurrentTime() {
  return formatDateTime(new Date());
}

function getMinutesAgoTime(minutes) {
  return formatDateTime(new Date(Date.now() - minutes * 60 * 1000));
}

function unwrap(response) {
  const data = response?.data;
  return data && typeof data === "object" && "data" in data ? data.data : data;
}

function buildNumericSeries(records) {
  return records
    .map((item, index) => {
      const value = pickPrimaryValue(item);

      if (value === null) {
        return null;
      }

      return {
        key: `${item.sensorId || "fiber"}-${item.collectTime || index}-${index}`,
        value,
        collectTime: item.collectTime || "",
        shortTime: formatShortTime(item.collectTime),
      };
    })
    .filter(Boolean)
    .sort((a, b) => String(a.collectTime).localeCompare(String(b.collectTime)));
}

function pickPrimaryValue(item) {
  const rawValue = Number(item?.rawValue);
  const intensity = Number(item?.intensity);
  const wavelength = Number(item?.wavelength);

  const hasRawValue = Number.isFinite(rawValue);
  const hasIntensity = Number.isFinite(intensity);
  const hasWavelength = Number.isFinite(wavelength);

  if (hasRawValue) {
    if (hasIntensity && hasWavelength && Math.abs(rawValue - wavelength) < 0.000001 && Math.abs(intensity - wavelength) > 0.1) {
      return intensity;
    }
    return rawValue;
  }

  if (hasIntensity) {
    return intensity;
  }

  return hasWavelength ? wavelength : null;
}

function calculateMedian(values) {
  const sortedValues = values.filter((value) => Number.isFinite(value)).sort((a, b) => a - b);

  if (sortedValues.length === 0) {
    return null;
  }

  const midIndex = Math.floor(sortedValues.length / 2);

  if (sortedValues.length % 2 === 0) {
    return (sortedValues[midIndex - 1] + sortedValues[midIndex]) / 2;
  }

  return sortedValues[midIndex];
}

function isOutlierValue(value, median) {
  return Number.isFinite(value) && Number.isFinite(median) && Math.abs(value - median) > OUTLIER_THRESHOLD_NM;
}

function isHistoryOutlier(item) {
  const value = pickPrimaryValue(item);

  if (value === null || historyMedian.value === null) {
    return false;
  }

  return isOutlierValue(value, historyMedian.value);
}

function formatNumber(value) {
  if (!Number.isFinite(value)) {
    return "-";
  }

  return Number(value).toFixed(4);
}

function formatDisplayValue(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return value;
}

function formatShortTime(value) {
  if (!value) {
    return "-";
  }

  const parts = String(value).split(" ");
  return parts.length === 2 ? parts[1] : String(value);
}

async function handleQuery() {
  if (!queryForm.value.sensorId?.trim()) {
    errorMessage.value = "传感器ID不能为空";
    return;
  }

  loading.value = true;
  errorMessage.value = "";

  try {
    const sensorId = String(queryForm.value.sensorId).trim();
    latestData.value = unwrap(await postFiberLatest({ sensorId }));

    const history = unwrap(
      await postFiberHistory({
        sensorId,
        startTime: queryForm.value.startTime,
        endTime: queryForm.value.endTime,
        limit: Number(queryForm.value.limit),
      }),
    );

    historyList.value = Array.isArray(history) ? history : [];
    await refreshRealtimeData();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "查询失败";
    latestData.value = null;
    historyList.value = [];
  } finally {
    loading.value = false;
  }
}

async function refreshRealtimeData() {
  const sensorId = realtimeSensorId.value;
  realtimeLoading.value = true;

  try {
    const [latestResponse, historyResponse] = await Promise.all([
      postFiberLatest({ sensorId }),
      postFiberHistory({
        sensorId,
        startTime: getMinutesAgoTime(REALTIME_WINDOW_MINUTES),
        endTime: getCurrentTime(),
        limit: 300,
      }),
    ]);

    latestData.value = unwrap(latestResponse);
    const history = unwrap(historyResponse);
    realtimeHistoryList.value = Array.isArray(history) ? history : [];
    realtimeErrorMessage.value = "";
    lastRefreshTime.value = getCurrentTime();
  } catch (error) {
    realtimeErrorMessage.value = error?.response?.data?.message || error?.message || "实时曲线刷新失败";
  } finally {
    realtimeLoading.value = false;
  }
}

function startRealtimeRefresh() {
  stopRealtimeRefresh();
  refreshRealtimeData();
  realtimeTimer = window.setInterval(refreshRealtimeData, REALTIME_REFRESH_INTERVAL);
}

function stopRealtimeRefresh() {
  if (realtimeTimer) {
    window.clearInterval(realtimeTimer);
    realtimeTimer = null;
  }
}

onMounted(() => {
  handleQuery();
  startRealtimeRefresh();
});

onUnmounted(() => {
  stopRealtimeRefresh();
});
</script>

<style scoped>
.fiber-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #111827;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
}

.page-badge {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #111827;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
}

.section-card {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
}

.section-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.section-tag {
  background: #f3f4f6;
  padding: 6px 12px;
  border-radius: 999px;
  white-space: nowrap;
}

.data-grid,
.realtime-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.realtime-summary {
  margin-bottom: 16px;
}

.data-item {
  min-width: 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
}

.data-item-wide {
  grid-column: span 4;
}

.data-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.data-value {
  overflow-wrap: anywhere;
}

.inline-error {
  margin-bottom: 12px;
  font-size: 13px;
  color: #dc2626;
}

.warning-message {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #f59e0b;
  border-radius: 10px;
  background: #fffbeb;
  color: #92400e;
  font-size: 13px;
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

.axis-line,
.grid-line {
  stroke: #d1d5db;
  stroke-width: 1;
}

.grid-line {
  stroke-dasharray: 4 8;
}

.chart-polyline {
  fill: none;
  stroke: #0f766e;
  stroke-width: 2.5;
}

.chart-point {
  fill: #0f766e;
}

.chart-x-label,
.chart-y-label {
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
}

.history-table th,
.history-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
}

.history-table th {
  background: #fafafa;
}

.outlier-row {
  background: #fffbeb;
}

.outlier-badge {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  padding: 3px 8px;
  border-radius: 999px;
  background: #fef3c7;
  color: #92400e;
  font-size: 12px;
}

@media (max-width: 1280px) {
  .data-grid,
  .realtime-summary {
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

  .data-grid,
  .realtime-summary {
    grid-template-columns: 1fr;
  }

  .data-item-wide {
    grid-column: span 1;
  }
}
</style>
