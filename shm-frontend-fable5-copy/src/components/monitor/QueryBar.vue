<template>
  <section class="query-bar">
    <div class="query-header">
      <h3 class="query-title">查询条件</h3>
      <span class="query-tip">时间格式：yyyy-MM-dd HH:mm:ss</span>
    </div>

    <div class="query-grid">
      <div class="form-item">
        <label class="form-label">传感器ID</label>
        <input
          class="form-input"
          type="text"
          :value="modelValue.sensorId"
          placeholder="请输入 sensorId"
          @input="updateField('sensorId', $event.target.value)"
          @keyup.enter="$emit('query')"
        />
      </div>

      <div class="form-item">
        <label class="form-label">开始时间</label>
        <input
          class="form-input"
          type="text"
          :value="modelValue.startTime"
          placeholder="yyyy-MM-dd HH:mm:ss"
          @input="updateField('startTime', $event.target.value)"
          @keyup.enter="$emit('query')"
        />
      </div>

      <div class="form-item">
        <label class="form-label">结束时间</label>
        <input
          class="form-input"
          type="text"
          :value="modelValue.endTime"
          placeholder="yyyy-MM-dd HH:mm:ss"
          @input="updateField('endTime', $event.target.value)"
          @keyup.enter="$emit('query')"
        />
      </div>

      <div class="form-item">
        <label class="form-label">查询条数</label>
        <input
          class="form-input"
          type="number"
          min="1"
          step="1"
          :value="modelValue.limit"
          placeholder="请输入 limit"
          @input="updateField('limit', normalizeLimit($event.target.value))"
          @keyup.enter="$emit('query')"
        />
      </div>
    </div>

    <div class="action-row">
      <button type="button" class="action-btn primary" :disabled="loading" @click="$emit('query')">
        {{ loading ? "查询中..." : "查询" }}
      </button>
    </div>

    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>
  </section>
</template>

<script setup>
defineOptions({
  name: "QueryBar",
});

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["update:modelValue", "query"]);

function updateField(field, value) {
  emit("update:modelValue", {
    ...props.modelValue,
    [field]: value,
  });
}

function normalizeLimit(value) {
  if (value === "" || value === null || value === undefined) {
    return "";
  }

  const parsedValue = Number(value);
  return Number.isNaN(parsedValue) ? "" : parsedValue;
}
</script>

<style scoped>
.query-bar {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  box-sizing: border-box;
}

.query-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.query-title {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.query-tip {
  font-size: 12px;
  color: #6b7280;
}

.query-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #4b5563;
}

.form-input {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  outline: none;
  box-sizing: border-box;
  background: #ffffff;
  color: #111827;
  font-size: 14px;
}

.form-input:focus {
  border-color: #2563eb;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.action-btn {
  min-width: 120px;
  height: 42px;
  padding: 0 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #ffffff;
  color: #111827;
  cursor: pointer;
}

.action-btn.primary {
  background: #111827;
  border-color: #111827;
  color: #ffffff;
}

.action-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  margin-top: 12px;
  font-size: 13px;
  color: #dc2626;
}

@media (max-width: 1280px) {
  .query-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .query-bar {
    padding: 16px;
  }

  .query-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .query-grid {
    grid-template-columns: 1fr;
  }

  .action-row {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }
}
</style>
