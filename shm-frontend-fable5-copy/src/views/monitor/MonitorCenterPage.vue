<template>
  <div class="monitor-center-page">
    <div v-if="!activeModule" class="overview-page">
      <div class="overview-header">
        <h2 class="overview-title">吊车梁监测系统</h2>
        <p class="overview-subtitle">六大业务监测模块 · 数据来源：OS265 统一数据源</p>
      </div>

      <section class="overview-query">
        <div class="query-item">
          <label class="query-label">传感器ID（可留空查询全部）</label>
          <input
            v-model="overviewQuery.sensorId"
            class="query-input"
            type="text"
            placeholder="可留空"
            @keyup.enter="loadOverviewData"
          />
        </div>

        <div class="query-item">
          <label class="query-label">开始时间</label>
          <input
            v-model="overviewQuery.startTime"
            class="query-input"
            type="text"
            placeholder="yyyy-MM-dd HH:mm:ss"
            @keyup.enter="loadOverviewData"
          />
        </div>

        <div class="query-item">
          <label class="query-label">结束时间</label>
          <input
            v-model="overviewQuery.endTime"
            class="query-input"
            type="text"
            placeholder="yyyy-MM-dd HH:mm:ss"
            @keyup.enter="loadOverviewData"
          />
        </div>

        <button
          type="button"
          class="query-btn"
          :disabled="overviewLoading"
          @click="loadOverviewData"
        >
          {{ overviewLoading ? "加载中..." : "刷新总览数据" }}
        </button>
      </section>

      <div v-if="overviewError" class="overview-error">
        {{ overviewError }}
      </div>

      <div class="module-card-grid">
        <button
          v-for="module in modules"
          :key="module.key"
          type="button"
          class="module-card"
          @click="openModule(module.key)"
        >
          <div class="card-header">
            <div>
              <div class="card-title">{{ module.label }}</div>
              <div class="card-subtitle">Y轴：测值（OS265 主值）</div>
            </div>
            <span class="card-count">{{ getSeries(module.key).length }} 点</span>
          </div>

          <div class="chart-wrap">
            <svg class="mini-chart" viewBox="0 0 300 170" preserveAspectRatio="none">
              <line x1="52" y1="20" x2="52" y2="130" class="axis-line" />
              <line x1="52" y1="130" x2="280" y2="130" class="axis-line" />

              <text x="8" y="25" class="axis-value">{{ getCardStats(module.key).max }}</text>
              <text x="8" y="132" class="axis-value">{{ getCardStats(module.key).min }}</text>
              <text x="148" y="160" class="axis-title">X轴：采集时间</text>

              <polyline
                v-if="getCardPoints(module.key).length > 1"
                :points="getCardPolyline(module.key)"
                class="chart-polyline"
              />

              <circle
                v-for="point in getCardPoints(module.key)"
                :key="point.key"
                :cx="point.x"
                :cy="point.y"
                r="4"
                class="chart-point"
              />

              <text v-if="getCardPoints(module.key).length > 0" x="52" y="146" class="time-label">
                {{ getFirstTime(module.key) }}
              </text>

              <text
                v-if="getCardPoints(module.key).length > 1"
                x="280"
                y="146"
                text-anchor="end"
                class="time-label"
              >
                {{ getLastTime(module.key) }}
              </text>

              <text
                v-if="getCardPoints(module.key).length === 0"
                x="166"
                y="82"
                text-anchor="middle"
                class="empty-chart-text"
              >
                {{ overviewLoading ? "数据加载中..." : "暂无已映射通道数据" }}
              </text>
            </svg>
          </div>

          <div class="card-footer">
            <span>最新值：{{ getLatestValue(module.key) }}</span>
            <span>{{ getLatestTime(module.key) }}</span>
          </div>
        </button>
      </div>
    </div>

    <div v-else class="detail-page">
      <div class="page-header">
        <div>
          <h2 class="page-title">{{ activeModuleConfig.label }}</h2>
          <p class="page-subtitle">{{ activeModuleConfig.desc }}</p>
        </div>

        <div class="page-actions">
          <button type="button" class="back-btn" @click="backToOverview">返回模块总览</button>
          <span class="page-badge">统一数据源</span>
        </div>
      </div>

      <MonitorInnerNav
        :modules="navModules"
        :active-module="activeModule"
        @change-module="handleModuleChange"
      />

      <section class="module-content">
        <transition name="fade" mode="out-in">
          <MonitorModulePage :key="activeModule" :module-key="activeModule" />
        </transition>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import MonitorInnerNav from "@/components/monitor/MonitorInnerNav.vue";
import MonitorModulePage from "@/components/monitor/MonitorModulePage.vue";
import { fetchModuleHistory, unwrapResultData } from "@/api/monitorData";
import { MONITOR_MODULES, getModuleConfig } from "@/utils/monitor/moduleConfig";
import { buildOs265NumericSeries, formatOs265Value } from "@/utils/os265Value";
import {
  buildChartPoints,
  buildPolyline,
  formatShortTime,
  getChartBounds,
} from "@/utils/monitor/chartGeometry";

defineOptions({
  name: "MonitorCenterPage",
});

// The exact six business modules; no fiber/vibratingWire/vibrationDat cards.
const modules = MONITOR_MODULES;

const activeModule = ref("");
const overviewLoading = ref(false);
const overviewError = ref("");

const overviewQuery = reactive({
  sensorId: "",
  startTime: getYearStartTime(),
  endTime: getCurrentTime(),
  limit: 50,
});

const overviewData = reactive(
  Object.fromEntries(modules.map((module) => [module.key, []])),
);

const timePattern = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/;
const cardBox = { startX: 58, endX: 276, startY: 26, endY: 124 };

const navModules = computed(() =>
  modules.map((module) => ({ key: module.key, label: module.shortLabel })),
);

const activeModuleConfig = computed(() => getModuleConfig(activeModule.value) || {});

onMounted(() => {
  loadOverviewData();
});

function pad(value) {
  return String(value).padStart(2, "0");
}

function formatDateTime(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function getYearStartTime() {
  return `${new Date().getFullYear()}-01-01 00:00:00`;
}

function getCurrentTime() {
  return formatDateTime(new Date());
}

function validateOverviewQuery() {
  if (!timePattern.test(overviewQuery.startTime)) {
    overviewError.value = "开始时间格式必须为 yyyy-MM-dd HH:mm:ss";
    return false;
  }
  if (!timePattern.test(overviewQuery.endTime)) {
    overviewError.value = "结束时间格式必须为 yyyy-MM-dd HH:mm:ss";
    return false;
  }
  if (overviewQuery.startTime > overviewQuery.endTime) {
    overviewError.value = "开始时间不能大于结束时间";
    return false;
  }

  overviewError.value = "";
  return true;
}

async function loadOverviewData() {
  if (!validateOverviewQuery()) {
    return;
  }

  overviewLoading.value = true;

  try {
    await Promise.all(
      modules.map(async (module) => {
        overviewData[module.key] = await requestModuleHistory(module.key);
      }),
    );
  } catch (error) {
    overviewError.value = error?.message || "模块总览数据加载失败";
  } finally {
    overviewLoading.value = false;
  }
}

async function requestModuleHistory(moduleKey) {
  const sensorId = String(overviewQuery.sensorId || "").trim();
  const payload = {
    ...(sensorId ? { sensorId } : {}),
    startTime: overviewQuery.startTime,
    endTime: overviewQuery.endTime,
    limit: Number(overviewQuery.limit),
  };

  try {
    const response = await fetchModuleHistory(moduleKey, payload);
    const data = unwrapResultData(response);
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

function getSeries(moduleKey) {
  return buildOs265NumericSeries(overviewData[moduleKey] || []);
}

function getCardPoints(moduleKey) {
  return buildChartPoints(getSeries(moduleKey), cardBox);
}

function getCardPolyline(moduleKey) {
  return buildPolyline(getCardPoints(moduleKey));
}

function getCardStats(moduleKey) {
  const series = getSeries(moduleKey);
  if (series.length === 0) {
    return { min: "-", max: "-" };
  }
  const bounds = getChartBounds(series.map((item) => item.value));
  return {
    min: formatOs265Value(bounds.min),
    max: formatOs265Value(bounds.max),
  };
}

function getFirstTime(moduleKey) {
  return formatShortTime(getSeries(moduleKey)[0]?.collectTime);
}

function getLastTime(moduleKey) {
  const series = getSeries(moduleKey);
  return formatShortTime(series[series.length - 1]?.collectTime);
}

function getLatestValue(moduleKey) {
  const series = getSeries(moduleKey);
  const latest = series[series.length - 1];
  return latest ? formatOs265Value(latest.value) : "-";
}

function getLatestTime(moduleKey) {
  const series = getSeries(moduleKey);
  const latest = series[series.length - 1];
  return latest ? formatShortTime(latest.collectTime) : "暂无时间";
}

function openModule(moduleKey) {
  activeModule.value = moduleKey;
}

function backToOverview() {
  activeModule.value = "";
}

function handleModuleChange(moduleKey) {
  activeModule.value = moduleKey;
}
</script>

<style scoped>
.monitor-center-page {
  min-height: 100%;
}

.overview-page,
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.overview-header {
  text-align: center;
  padding-top: 8px;
}

.overview-title {
  margin: 0;
  font-size: 30px;
  color: #111827;
  font-weight: 800;
}

.overview-subtitle {
  margin: 10px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.overview-query {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 12px;
  align-items: end;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
}

.query-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.query-label {
  font-size: 13px;
  color: #4b5563;
}

.query-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  outline: none;
  color: #111827;
}

.query-input:focus {
  border-color: #2563eb;
}

.query-btn {
  height: 40px;
  padding: 0 18px;
  border: 0;
  border-radius: 10px;
  background: #111827;
  color: #ffffff;
  cursor: pointer;
  font-weight: 600;
}

.query-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.overview-error {
  padding: 12px 16px;
  border: 1px solid #fecaca;
  border-radius: 12px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 14px;
}

.module-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.module-card {
  min-height: 230px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  text-align: left;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.module-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.12);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.card-title {
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.card-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}

.card-count {
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
}

.chart-wrap {
  width: 100%;
}

.mini-chart {
  display: block;
  width: 100%;
  height: 160px;
  border-radius: 12px;
  background: #fcfcfd;
  border: 1px solid #eef2f7;
}

.axis-line {
  stroke: #9ca3af;
  stroke-width: 1.2;
}

.axis-value {
  font-size: 11px;
  fill: #6b7280;
}

.axis-title {
  font-size: 11px;
  fill: #4b5563;
}

.chart-polyline {
  fill: none;
  stroke: #2563eb;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chart-point {
  fill: #111827;
}

.time-label {
  font-size: 10px;
  fill: #6b7280;
}

.empty-chart-text {
  font-size: 13px;
  fill: #9ca3af;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  font-size: 12px;
  color: #4b5563;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 26px;
  color: #111827;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.page-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
}

.back-btn {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 999px;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.back-btn:hover {
  background: #f3f4f6;
}

.page-badge {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  font-weight: 600;
}

.module-content {
  min-height: 300px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1200px) {
  .overview-query {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .module-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .overview-query {
    grid-template-columns: 1fr;
  }

  .module-card-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-actions {
    justify-content: flex-start;
  }
}
</style>
