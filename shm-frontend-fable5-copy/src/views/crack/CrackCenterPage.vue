<template>
  <div class="crack-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">裂缝</h2>
        <p class="page-subtitle">计算结果类功能栏页面</p>
      </div>
      <div class="page-badge">CrackCenterPage</div>
    </div>

    <section class="section-card">
      <div class="section-header">
        <h3 class="section-title">页面状态</h3>
        <span class="section-tag">待接入</span>
      </div>

      <div class="empty-block">
        当前裂缝页面壳已建立，后续接入裂缝模块 latest / history 与核验链路。
      </div>
    </section>
  </div>
</template>

<script setup>
defineOptions({
  name: "CrackCenterPage",
});
</script>

<style scoped>
.crack-page {
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
  background: #fef3c7;
  color: #92400e;
  font-size: 12px;
}

.empty-block {
  min-height: 120px;
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
</style>
