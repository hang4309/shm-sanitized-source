<template>
  <div class="dat-page">
    <div class="page-header"><div><h2 class="page-title">振动DAT监测</h2><p class="page-subtitle">仅做文件级接入与结构展示</p></div><div class="page-badge">VibrationDatMonitorPage</div></div>
    <QueryBar v-model="queryForm" :loading="loading" :error-message="errorMessage" @query="handleQuery" />
    <section class="section-card">
      <div class="section-header"><h3 class="section-title">最新数据</h3><span class="section-tag">latest</span></div>
      <div v-if="latestData" class="data-grid">
        <div class="data-item"><div class="data-label">传感器ID</div><div class="data-value">{{ latestData.sensorId || "-" }}</div></div>
        <div class="data-item"><div class="data-label">设备编号</div><div class="data-value">{{ latestData.deviceNo || "-" }}</div></div>
        <div class="data-item"><div class="data-label">文件名</div><div class="data-value">{{ latestData.fileName || "-" }}</div></div>
        <div class="data-item"><div class="data-label">文件路径</div><div class="data-value">{{ latestData.filePath || "-" }}</div></div>
        <div class="data-item"><div class="data-label">文件大小</div><div class="data-value">{{ formatDisplayValue(latestData.fileSize) }}</div></div>
        <div class="data-item"><div class="data-label">采样率</div><div class="data-value">{{ formatDisplayValue(latestData.sampleRate) }}</div></div>
        <div class="data-item"><div class="data-label">通道数</div><div class="data-value">{{ formatDisplayValue(latestData.channelCount) }}</div></div>
        <div class="data-item"><div class="data-label">点数</div><div class="data-value">{{ formatDisplayValue(latestData.pointCount) }}</div></div>
        <div class="data-item"><div class="data-label">时长(s)</div><div class="data-value">{{ formatDisplayValue(latestData.durationSeconds) }}</div></div>
        <div class="data-item data-item-wide"><div class="data-label">采集时间</div><div class="data-value">{{ latestData.collectTime || "-" }}</div></div>
      </div>
      <div v-else class="empty-block">{{ loading ? "加载中..." : "暂无数据" }}</div>
    </section>
    <section class="section-card">
      <div class="section-header"><h3 class="section-title">历史明细</h3><span class="section-tag">共{{ historyList.length }}条</span></div>
      <div v-if="historyList.length" class="table-wrapper">
        <table class="history-table"><thead><tr><th>传感器ID</th><th>设备编号</th><th>文件名</th><th>文件路径</th><th>文件大小</th><th>采样率</th><th>通道数</th><th>点数</th><th>时长(s)</th><th>采集时间</th></tr></thead>
        <tbody><tr v-for="(item,index) in historyList" :key="`${item.sensorId || 'row'}-${index}`"><td>{{ item.sensorId || "-" }}</td><td>{{ item.deviceNo || "-" }}</td><td>{{ item.fileName || "-" }}</td><td>{{ item.filePath || "-" }}</td><td>{{ formatDisplayValue(item.fileSize) }}</td><td>{{ formatDisplayValue(item.sampleRate) }}</td><td>{{ formatDisplayValue(item.channelCount) }}</td><td>{{ formatDisplayValue(item.pointCount) }}</td><td>{{ formatDisplayValue(item.durationSeconds) }}</td><td>{{ item.collectTime || "-" }}</td></tr></tbody></table>
      </div>
      <div v-else class="empty-block">{{ loading ? "加载中..." : "暂无历史数据" }}</div>
    </section>
  </div>
</template>
<script setup>
import { ref } from "vue"; import QueryBar from "@/components/monitor/QueryBar.vue"; import { postVibrationDatHistory, postVibrationDatLatest } from "@/api/vibrationDat";
defineOptions({ name: "VibrationDatMonitorPage" }); const loading=ref(false), errorMessage=ref(""), latestData=ref(null), historyList=ref([]);
const queryForm=ref({sensorId:"",startTime:getTodayStartTime(),endTime:getCurrentTime(),limit:100}); function pad(v){return String(v).padStart(2,"0");} function formatDateTime(d){return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;} function getTodayStartTime(){const n=new Date(); return `${n.getFullYear()}-${pad(n.getMonth()+1)}-${pad(n.getDate())} 00:00:00`;} function getCurrentTime(){return formatDateTime(new Date());} function unwrap(r){const d=r?.data; return d && typeof d==="object" && "data" in d ? d.data : d;} function formatDisplayValue(v){return v===null||v===undefined||v===""?"-":v;}
async function handleQuery(){ if(!queryForm.value.sensorId?.trim()){ errorMessage.value="传感器ID不能为空"; return; } loading.value=true; errorMessage.value=""; try { const sid=String(queryForm.value.sensorId).trim(); latestData.value=unwrap(await postVibrationDatLatest({sensorId:sid})); const hist=unwrap(await postVibrationDatHistory({sensorId:sid,startTime:queryForm.value.startTime,endTime:queryForm.value.endTime,limit:Number(queryForm.value.limit)})); historyList.value=Array.isArray(hist)?hist:[]; } catch(e){ errorMessage.value=e?.response?.data?.message||e?.message||"查询失败"; latestData.value=null; historyList.value=[]; } finally { loading.value=false; } }
</script>
<style scoped>
.dat-page{display:flex;flex-direction:column;gap:20px}.page-header{display:flex;justify-content:space-between;gap:16px}.page-title{margin:0;font-size:28px;color:#111827}.page-subtitle{margin:8px 0 0;color:#6b7280}.page-badge{height:34px;padding:0 14px;border-radius:999px;background:#111827;color:#fff;display:inline-flex;align-items:center}.section-card{padding:20px;border:1px solid #e5e7eb;border-radius:16px;background:#fff}.section-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}.section-title{margin:0;font-size:18px}.section-tag{background:#f3f4f6;padding:6px 12px;border-radius:999px}.data-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.data-item{padding:14px;border:1px solid #e5e7eb;border-radius:12px;background:#fafafa}.data-item-wide{grid-column:span 4}.data-label{font-size:12px;color:#6b7280;margin-bottom:8px}.empty-block{min-height:92px;display:flex;align-items:center;justify-content:center;border:1px dashed #d1d5db;border-radius:12px;background:#fafafa;color:#6b7280}.table-wrapper{overflow-x:auto}.history-table{width:100%;border-collapse:collapse}.history-table th,.history-table td{padding:12px 10px;border-bottom:1px solid #e5e7eb;text-align:left}.history-table th{background:#fafafa}@media (max-width:1280px){.data-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.data-item-wide{grid-column:span 2}}@media (max-width:900px){.page-header,.section-header{flex-direction:column;align-items:flex-start}.data-grid{grid-template-columns:1fr}.data-item-wide{grid-column:span 1}}
</style>
