-- ============================================================
-- Unified vendor-source schema (additive migration, manual run only)
--
-- Target architecture:
--   * vendor_source_reading      one unified vendor-source reading table
--   * vendor_channel_mapping     channel-to-business-module mapping
--   * monitor_module_definition  metadata for the six business modules
--
-- Rules honored by this file:
--   * additive only: creates missing unified tables and seed rows;
--     legacy tables are not changed and no data is removed
--   * legacy tables (fiber_raw_data, strain_data, ...) stay untouched;
--     compatibility is handled in the service/mapper layer
--   * do NOT execute automatically; run manually after review
--   * runtime ingestion does not depend on this file once the tables
--     already exist in the target database
-- ============================================================

-- ------------------------------------------------------------
-- Table 1: vendor_source_reading
-- One row per normalized vendor reading. measured_value/raw_value/
-- intensity carry the OS265 intensity/energy measured value (the
-- primary business value, typically around -10.x); wavelength is an
-- auxiliary reference only (around 1563.x).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vendor_source_reading (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_type VARCHAR(32) NOT NULL DEFAULT 'OS265',
  device_no VARCHAR(64) NOT NULL,
  channel_no VARCHAR(64) NOT NULL,
  sensor_id VARCHAR(64) NOT NULL,
  module_key VARCHAR(32) NULL,
  measured_value DECIMAL(18,6) NOT NULL,
  raw_value DECIMAL(18,6) NULL,
  intensity DECIMAL(18,6) NULL,
  wavelength DECIMAL(18,6) NULL,
  wavelength_shift DECIMAL(18,6) NULL,
  collect_time DATETIME NOT NULL,
  source_file VARCHAR(512) NOT NULL,
  -- source_file_hash: MD5 of source_file path, stored as CHAR(32)
  source_file_hash CHAR(32) NOT NULL,
  source_offset BIGINT NULL,
  source_line BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vendor_reading_origin (source_type, device_no, channel_no, collect_time, source_file_hash, source_line),
  KEY idx_vsr_source_type (source_type),
  KEY idx_vsr_device_no (device_no),
  KEY idx_vsr_channel_no (channel_no),
  KEY idx_vsr_sensor_id (sensor_id),
  KEY idx_vsr_module_key (module_key),
  KEY idx_vsr_collect_time (collect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- Table 2: vendor_channel_mapping
-- Resolves sourceType + deviceNo + channelNo (or sensorId) to a
-- business module. Channels without a row here stay unassigned
-- (module_key NULL) - module keys are never fabricated.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vendor_channel_mapping (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_type VARCHAR(32) NOT NULL DEFAULT 'OS265',
  device_no VARCHAR(64) NOT NULL,
  channel_no VARCHAR(64) NOT NULL,
  sensor_id VARCHAR(64) NOT NULL,
  module_key VARCHAR(32) NULL,
  sensor_name VARCHAR(128) NULL,
  unit VARCHAR(64) NULL,
  location VARCHAR(128) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_vendor_channel (source_type, device_no, channel_no),
  KEY idx_vcm_sensor_id (sensor_id),
  KEY idx_vcm_module_key (module_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- Table 3: monitor_module_definition
-- Metadata for the exact six business monitoring modules.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_module_definition (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module_key VARCHAR(32) NOT NULL,
  module_name VARCHAR(64) NOT NULL,
  module_label VARCHAR(64) NOT NULL,
  unit VARCHAR(64) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  remark VARCHAR(255) NULL,
  UNIQUE KEY uk_module_key (module_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- Seed: the six business modules (INSERT IGNORE keeps reruns additive)
-- ------------------------------------------------------------
INSERT IGNORE INTO monitor_module_definition
  (module_key, module_name, module_label, unit, enabled, sort_order, remark)
VALUES
  ('displacement', 'displacement', '位移监测', 'OS265 measured value', 1, 1, 'business module fed by unified vendor source'),
  ('acceleration', 'acceleration', '加速度监测', 'OS265 measured value', 1, 2, 'business module fed by unified vendor source'),
  ('strain', 'strain', '应变监测', 'OS265 measured value', 1, 3, 'business module fed by unified vendor source'),
  ('vibration', 'vibration', '振动监测', 'OS265 measured value', 1, 4, 'business module fed by unified vendor source'),
  ('stress', 'stress', '应力监测', 'OS265 measured value', 1, 5, 'business module fed by unified vendor source'),
  ('deflection', 'deflection', '挠度监测', 'OS265 measured value', 1, 6, 'business module fed by unified vendor source');

-- ------------------------------------------------------------
-- Seed: validated demo mapping
-- OS265 device OS-265, channel CH2 -> strain (FBG-STRAIN-CH2)
-- Primary value displays around -10.x; wavelength stays auxiliary
-- around 1563.x.
-- ------------------------------------------------------------
INSERT IGNORE INTO vendor_channel_mapping
  (source_type, device_no, channel_no, sensor_id, module_key, sensor_name, unit, enabled, remark)
VALUES
  ('OS265', 'OS-265', 'CH2', 'FBG-STRAIN-CH2', 'strain', 'OS265 Channel 2 Strain', 'OS265 measured value', 1, 'validated demo mapping');
