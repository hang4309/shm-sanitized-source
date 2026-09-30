<template>
  <div class="fatigue-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">疲劳监测</h2>
        <p class="page-subtitle">计算结果类疲劳数据实时监控</p>
      </div>
      <div class="page-badge">FatigueMonitorPage</div>
    </div>

    <QueryBar
      v-model="queryForm"
      :loading="loading"
      :error-message="errorMessage"
      @query="handleQuery"
    />

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">最新疲劳数据</h3>
        <span class="section-tag">latest</span>
      </div>

      <div v-if="latestData" class="data-grid">
        <div class="data-item">
          <div class="data-label">传感器ID</div>
          <div class="data-value">{{ latestData.sensorId || "-" }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">损伤指标</div>
          <div class="data-value">{{ formatDisplayValue(latestData.damageIndex) }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">雨流计数</div>
          <div class="data-value">{{ formatDisplayValue(latestData.rainflowCount) }}</div>
        </div>
        <div class="data-item">
          <div class="data-label">剩余寿命</div>
          <div class="data-value">{{ formatDisplayValue(latestData.remainingLife) }}</div>
        </div>
        <div class="data-item data-item-wide">
          <div class="data-label">采集时间</div>
          <div class="data-value">{{ latestData.collectTime || "-" }}</div>
        </div>
      </div>

      <div v-else class="empty-block">
        {{ loading ? "最新疲劳数据加载中..." : "暂无最新疲劳数据" }}
      </div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">历史趋势图</h3>
        <div class="metric-switch">
          <button
            type="button"
            class="metric-btn"
            :class="{ active: chartMetric === 'damageIndex' }"
            @click="chartMetric = 'damageIndex'"
          >
            损伤指标
          </button>
          <button
            type="button"
            class="metric-btn"
            :class="{ active: chartMetric === 'rainflowCount' }"
            @click="chartMetric = 'rainflowCount'"
          >
            雨流计数
          </button>
          <button
            type="button"
            class="metric-btn"
            :class="{ active: chartMetric === 'remainingLife' }"
            @click="chartMetric = 'remainingLife'"
          >
            剩余寿命
          </button>
        </div>
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
          />

          <text
            v-for="point in chartPoints"
            :key="`${point.key}-x`"
            :x="point.x"
            y="294"
            class="chart-x-label"
            text-anchor="middle"
          >
            {{ point.shortTime }}
          </text>

          <text x="20" y="20" class="chart-title-text">{{ chartMetricLabel }}</text>
          <text x="952" y="308" class="chart-title-text" text-anchor="end">采集时间</text>
        </svg>

        <div class="chart-meta">
          <span>当前指标：{{ chartMetricLabel }}</span>
          <span>点数：{{ chartPoints.length }}</span>
          <span>最小值：{{ chartStats.min }}</span>
          <span>最大值：{{ chartStats.max }}</span>
        </div>
      </div>

      <div v-else class="empty-block">
        {{ loading ? "历史趋势图加载中..." : "暂无历史趋势图数据" }}
      </div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">历史疲劳明细</h3>
        <span class="section-tag">共 {{ historyList.length }} 条</span>
      </div>

      <div v-if="historyList.length > 0" class="table-wrapper">
        <table class="history-table">
          <thead>
            <tr>
              <th>传感器ID</th>
              <th>损伤指标</th>
              <th>雨流计数</th>
              <th>剩余寿命</th>
              <th>采集时间</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(item, index) in historyList"
              :key="`${item.sensorId || 'row'}-${item.collectTime || index}-${index}`"
            >
              <td>{{ item.sensorId || "-" }}</td>
              <td>{{ formatDisplayValue(item.damageIndex) }}</td>
              <td>{{ formatDisplayValue(item.rainflowCount) }}</td>
              <td>{{ formatDisplayValue(item.remainingLife) }}</td>
              <td>{{ item.collectTime || "-" }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-block">
        {{ loading ? "历史疲劳明细加载中..." : "暂无历史疲劳明细" }}
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import QueryBar from "@/components/monitor/QueryBar.vue";
import { getFatigueHistory, getFatigueLatest } from "@/api/fatigue";

defineOptions({
  name: "FatigueMonitorPage",
});

const loading = ref(false);
const errorMessage = ref("");
const latestData = ref(null);
const historyList = ref([]);
const chartMetric = ref("damageIndex");

const queryForm = ref({
  sensorId: "",
  startTime: getTodayStartTime(),
  endTime: getCurrentTime(),
  limit: 100,
});

const timePattern = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/;

const chartMetricLabel = computed(() => {
  if (chartMetric.value === "damageIndex") {
    return "损伤指标";
  }

  if (chartMetric.value === "rainflowCount") {
    return "雨流计数";
  }

  return "剩余寿命";
});

function pad(value) {
  return String(value).padStart(2, "0");
}

function formatDateTime(date) {
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  const second = pad(date.getSeconds());

  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

function getTodayStartTime() {
  const now = new Date();
  const year = now.getFullYear();
  const month = pad(now.getMonth() + 1);
  const day = pad(now.getDate());
  return `${year}-${month}-${day} 00:00:00`;
}

function getCurrentTime() {
  return formatDateTime(new Date());
}

function unwrapResultData(response) {
  const responseData = response?.data;

  if (responseData && typeof responseData === "object" && "data" in responseData) {
    return responseData.data;
  }

  return responseData;
}

function formatDisplayValue(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  return value;
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

function validateQuery() {
  const form = queryForm.value;

  if (!form.sensorId || !String(form.sensorId).trim()) {
    errorMessage.value = "传感器ID不能为空";
    return false;
  }

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

async function handleQuery() {
  if (!validateQuery()) {
    return;
  }

  loading.value = true;
  latestData.value = null;
  historyList.value = [];

  try {
    const form = queryForm.value;
    const sensorId = String(form.sensorId).trim();

    const latestResponse = await getFatigueLatest({
      sensorId,
    });

    latestData.value = normalizeObject(unwrapResultData(latestResponse));

    const historyResponse = await getFatigueHistory({
      sensorId,
      startTime: form.startTime,
      endTime: form.endTime,
      page: 1,
      size: Number(form.limit),
    });

    historyList.value = normalizeArray(unwrapResultData(historyResponse));
  } catch (error) {
    latestData.value = null;
    historyList.value = [];
    errorMessage.value = error?.response?.data?.message || error?.message || "疲劳模块查询失败";
  } finally {
    loading.value = false;
  }
}

function formatShortTime(value) {
  if (!value || typeof value !== "string") {
    return "--";
  }

  const parts = value.split(" ");
  return parts.length === 2 ? parts[1] : value;
}

const chartSeries = computed(() => {
  return historyList.value
    .map((item, index) => {
      const rawValue = item?.[chartMetric.value];
      const numericValue = Number(rawValue);

      if (Number.isNaN(numericValue)) {
        return null;
      }

      return {
        key: `${item.sensorId || "sensor"}-${item.collectTime || index}-${index}`,
        shortTime: formatShortTime(item.collectTime),
        value: numericValue,
      };
    })
    .filter(Boolean);
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
    min: Math.min(...values),
    max: Math.max(...values),
  };
});

const chartPoints = computed(() => {
  if (chartSeries.value.length === 0) {
    return [];
  }

  const startX = 90;
  const endX = 930;
  const startY = 40;
  const endY = 250;

  const values = chartSeries.value.map((item) => item.value);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
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

const chartPolyline = computed(() => {
  return chartPoints.value.map((point) => `${point.x},${point.y}`).join(" ");
});
</script>

<style scoped>
.fatigue-page {
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

.metric-switch {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.metric-btn {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
}

.metric-btn.active {
  background: #111827;
  border-color: #111827;
  color: #ffffff;
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
  stroke: #111827;
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

  .data-grid {
    grid-template-columns: 1fr;
  }

  .data-item-wide {
    grid-column: span 1;
  }
}
</style>
