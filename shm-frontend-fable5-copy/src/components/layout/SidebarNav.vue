<template>
  <aside class="sidebar-nav">
    <div class="sidebar-header">
      <div class="brand-mark">A</div>
      <div class="brand-text">
        <div class="brand-title">监测平台</div>
        <div class="brand-subtitle">功能导航</div>
      </div>
    </div>

    <div class="nav-scroll">
      <section v-for="group in groups" :key="group.title" class="nav-group">
        <div class="group-title">{{ group.title }}</div>

        <button
          v-for="item in group.items"
          :key="item.key"
          type="button"
          class="nav-item"
          :class="{ active: activeKey === item.key }"
          @click="$emit('select', item.key)"
        >
          <span class="nav-item-label">{{ item.label }}</span>
        </button>
      </section>
    </div>
  </aside>
</template>

<script setup>
defineOptions({
  name: "SidebarNav",
});

defineProps({
  groups: {
    type: Array,
    required: true,
  },
  activeKey: {
    type: String,
    default: "",
  },
});

defineEmits(["select"]);
</script>

<style scoped>
.sidebar-nav {
  width: 260px;
  height: 100vh;
  background: #202123;
  color: #ececf1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #2f3136;
  box-sizing: border-box;
}

.sidebar-header {
  height: 72px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #2f3136;
  box-sizing: border-box;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #10a37f;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.brand-text {
  min-width: 0;
}

.brand-title {
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.nav-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px 20px;
  box-sizing: border-box;
}

.nav-group + .nav-group {
  margin-top: 18px;
}

.group-title {
  margin: 0 8px 8px;
  font-size: 12px;
  color: #9ca3af;
  letter-spacing: 0.04em;
}

.nav-item {
  width: 100%;
  height: 42px;
  margin-bottom: 6px;
  padding: 0 12px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #ececf1;
  text-align: left;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background 0.18s ease;
}

.nav-item:hover {
  background: #2a2b32;
}

.nav-item.active {
  background: #343541;
  color: #ffffff;
}

.nav-item-label {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
