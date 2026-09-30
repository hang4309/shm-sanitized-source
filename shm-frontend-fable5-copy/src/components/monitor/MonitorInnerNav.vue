<template>
  <section class="monitor-inner-nav">
    <div v-if="categories.length > 0" class="category-row">
      <button
        v-for="category in categories"
        :key="category.key"
        type="button"
        class="category-btn"
        :class="{ active: activeCategory === category.key }"
        @click="$emit('change-category', category.key)"
      >
        {{ category.label }}
      </button>
    </div>

    <div class="module-row">
      <button
        v-for="module in modules"
        :key="module.key"
        type="button"
        class="module-btn"
        :class="{ active: activeModule === module.key }"
        @click="$emit('change-module', module.key)"
      >
        {{ module.label }}
      </button>
    </div>
  </section>
</template>

<script setup>
defineOptions({
  name: "MonitorInnerNav",
});

defineProps({
  categories: {
    type: Array,
    default: () => [],
  },
  activeCategory: {
    type: String,
    default: "",
  },
  modules: {
    type: Array,
    required: true,
  },
  activeModule: {
    type: String,
    default: "",
  },
});

defineEmits(["change-category", "change-module"]);
</script>

<style scoped>
.monitor-inner-nav {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  box-sizing: border-box;
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.category-btn,
.module-btn {
  border: 0;
  cursor: pointer;
  transition: all 0.18s ease;
}

.category-btn {
  height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
}

.category-btn.active {
  background: #111827;
  color: #ffffff;
}

.module-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.module-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: #374151;
  font-size: 14px;
}

.module-btn.active {
  background: #eef2ff;
  border-color: #c7d2fe;
  color: #3730a3;
}

@media (max-width: 768px) {
  .monitor-inner-nav {
    padding: 16px;
  }
}
</style>
