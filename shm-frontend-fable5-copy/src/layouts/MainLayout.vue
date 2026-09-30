<template>
  <div class="main-layout">
    <SidebarNav :groups="navGroups" :active-key="activeKey" @select="handleSelect" />

    <div class="layout-content">
      <header class="top-bar">
        <div class="top-bar-left">
          <h1 class="system-title">健康监测系统</h1>
          <p class="system-subtitle">内网版后台管理平台</p>
        </div>
        <div class="top-bar-right">
          <span class="top-badge">{{ activeLabel }}</span>
        </div>
      </header>

      <main class="content-body">
        <MonitorCenterPage v-if="activeKey === 'monitor'" />
        <PredictionCenterPage v-else-if="activeKey === 'prediction'" />
        <CrackCenterPage v-else-if="activeKey === 'crack'" />

        <section v-else class="module-placeholder">
          <div class="placeholder-card">
            <h2 class="placeholder-title">{{ activeLabel }}</h2>
            <p class="placeholder-text">当前模块页面暂未铺设，后续按统一后台壳结构继续扩展。</p>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import SidebarNav from "@/components/layout/SidebarNav.vue";
import MonitorCenterPage from "@/views/monitor/MonitorCenterPage.vue";
import PredictionCenterPage from "@/views/prediction/PredictionCenterPage.vue";
import CrackCenterPage from "@/views/crack/CrackCenterPage.vue";

const navGroups = [
  {
    title: "功能导航",
    items: [
      { key: "monitor", label: "监测数据" },
      { key: "prediction", label: "预测" },
      { key: "crack", label: "裂缝" },
    ],
  },
];

const activeKey = ref("monitor");

const activeLabel = computed(() => {
  for (const group of navGroups) {
    const matched = group.items.find((item) => item.key === activeKey.value);
    if (matched) {
      return matched.label;
    }
  }
  return "监测数据";
});

function handleSelect(key) {
  activeKey.value = key;
}
</script>

<style scoped>
.main-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: #f7f7f8;
}

.layout-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f7f7f8;
}

.top-bar {
  height: 72px;
  padding: 0 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}

.top-bar-left {
  min-width: 0;
}

.system-title {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
  color: #111827;
}

.system-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.top-bar-right {
  display: flex;
  align-items: center;
}

.top-badge {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 13px;
  font-weight: 600;
}

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  box-sizing: border-box;
}

.module-placeholder {
  height: 100%;
}

.placeholder-card {
  padding: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
}

.placeholder-title {
  margin: 0 0 12px;
  font-size: 22px;
  color: #111827;
}

.placeholder-text {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

@media (max-width: 960px) {
  .top-bar {
    padding: 0 16px;
  }

  .content-body {
    padding: 16px;
  }

  .system-title {
    font-size: 18px;
  }
}
</style>
