/**
 * The exact six business monitoring modules.
 *
 * Device-specific concepts (fiber, vibratingWire, vibrationDat) are not
 * business modules and must not appear here; they only survive as
 * hidden legacy/adapter code. Every module reads the same unified
 * OS265/vendor source readings, selected by its moduleKey.
 */

export const MONITOR_MODULES = [
  {
    key: "displacement",
    label: "位移监测",
    shortLabel: "位移",
    desc: "统一数据源（OS265 测值）按 displacement 模块映射展示",
    defaultSensorId: "",
  },
  {
    key: "acceleration",
    label: "加速度监测",
    shortLabel: "加速度",
    desc: "统一数据源（OS265 测值）按 acceleration 模块映射展示",
    defaultSensorId: "",
  },
  {
    key: "strain",
    label: "应变监测",
    shortLabel: "应变",
    desc: "统一数据源（OS265 测值）按 strain 模块映射展示",
    // Validated demo mapping: OS-265 CH2 -> strain.
    defaultSensorId: "FBG-STRAIN-CH2",
  },
  {
    key: "vibration",
    label: "振动监测",
    shortLabel: "振动",
    desc: "统一数据源（OS265 测值）按 vibration 模块映射展示",
    defaultSensorId: "",
  },
  {
    key: "stress",
    label: "应力监测",
    shortLabel: "应力",
    desc: "统一数据源（OS265 测值）按 stress 模块映射展示",
    defaultSensorId: "",
  },
  {
    key: "deflection",
    label: "挠度监测",
    shortLabel: "挠度",
    desc: "统一数据源（OS265 测值）按 deflection 模块映射展示",
    defaultSensorId: "",
  },
];

export function getModuleConfig(moduleKey) {
  return MONITOR_MODULES.find((module) => module.key === moduleKey) || null;
}
