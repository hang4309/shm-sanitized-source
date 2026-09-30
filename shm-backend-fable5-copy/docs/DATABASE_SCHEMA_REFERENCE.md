# Database schema reference

This document describes the entities, mapper XML, service behavior, and [SQL reference](sql/vendor_source_unified_schema.sql) included in the repository. It contains schema information and SYNTHETIC demo identifiers; it contains no database export or field measurement records.

## Tables

| Table | Purpose |
| --- | --- |
| `vendor_source_reading` | One normalized vendor-source reading per stored row. |
| `vendor_channel_mapping` | Channel/sensor-to-business-module assignments. |
| `monitor_module_definition` | Display metadata for the six business modules. |

The current runtime reads and writes the unified tables. The reference SQL leaves legacy tables unchanged.

## Reading fields

| SQL column | Java/API field | Meaning |
| --- | --- | --- |
| `id` | Internal database ID | Auto-increment key and tie-breaker for query ordering; omitted from the reading response VO. |
| `source_type` | `sourceType` | Source identifier; canonical uploads default to `OS265`. |
| `device_no` | `deviceNo` | Device identifier. |
| `channel_no` | `channelNo` | Channel identifier. The response's legacy `fiberNo` aliases this value. |
| `sensor_id` | `sensorId` | Sensor identifier used in queries and mapping lookup. |
| `module_key` | `moduleKey` | Assigned business module; nullable for unassigned readings. |
| `measured_value` | `measuredValue` | Primary vendor intensity/energy value selected during ingestion. |
| `raw_value` | `rawValue` | Primary-value alias; falls back to the selected value when absent. |
| `intensity` | `intensity` | Primary-value alias; falls back to the selected value when absent. |
| `wavelength` | `wavelength` | Optional auxiliary wavelength reference. |
| `wavelength_shift` | `wavelengthShift` | Optional auxiliary shift supplied by the client; no shift is calculated here. |
| `collect_time` | `collectTime` | Vendor record time, parsed as a local date/time without a timezone offset. |
| `source_file` | `sourceFile` | Trimmed source-file name or path supplied by the uploader. |
| `source_file_hash` | Internal persistence field | Backend-computed MD5 of the source-file string. |
| `source_offset` | Upload field `sourceOffset` | Optional byte offset, persisted but omitted from the reading response VO. |
| `source_line` | `sourceLine` | Source line number; required by canonical ingestion. |
| `created_at` | Internal persistence field | Insert time. |
| `deleted` | Internal persistence field | Soft-delete flag; query mappers select rows with value 0. |

The SQL stores numeric readings as `DECIMAL(18,6)`. A stored decimal is not evidence of calibration or a particular physical unit. The service chooses the first supplied field in the order `measuredValue`, `rawValue`, `intensity`; explicitly supplied aliases are retained, so callers should keep them consistent.

## Source identity and duplicate protection

Canonical uploads require a nonempty `sourceFile` and a non-null `sourceLine`. The unique origin key is:

```text
(source_type, device_no, channel_no, collect_time, source_file_hash, source_line)
```

Repeating that key causes the database to reject a second row; the service translates the duplicate-key exception into a business error. Different collection times, source-file strings, channels, or line numbers can produce distinct keys.

`source_file_hash` identifies the submitted filename/path string. It is not a checksum of file contents or proof that an uploaded record came from a physical instrument. `source_offset` is retained for traceability but is not part of the unique key.

## Channel mapping

`vendor_channel_mapping` stores `source_type`, `device_no`, `channel_no`, `sensor_id`, `module_key`, `sensor_name`, `unit`, `location`, `enabled`, and `remark`, along with database IDs, timestamps, and the soft-delete flag. The SQL unique channel key is `(source_type, device_no, channel_no)`.

The service resolves a module in this order:

1. An enabled, undeleted mapping matching `sourceType + deviceNo + channelNo`.
2. If no channel mapping exists, an enabled, undeleted mapping matching `sourceType + sensorId`.
3. A nonempty `moduleKey` supplied in the upload.
4. An unassigned value (`null`).

Mapping queries select the lowest database ID when more than one row matches. A matching channel row takes precedence over the sensor lookup. If the selected row has an empty module key, the service falls back to the upload's module key.

The SYNTHETIC demonstration retains these generic identifiers to match the existing collector and schema seed:

```text
OS265 / OS-265 / CH2 / FBG-STRAIN-CH2 -> strain
```

These identifiers describe the example mapping; they do not identify an included real measurement dataset.

## Module metadata and queries

`monitor_module_definition` holds the module key, display name/label, unit text, enabled flag, sort order, and remark. The six keys are `displacement`, `acceleration`, `strain`, `vibration`, `stress`, and `deflection`.

Business-route validation uses Java's `MonitorModules.ALL`. Adding a metadata row alone does not add a new supported route. Module labels organize readings; the current service does not convert vendor values into six calibrated engineering quantities.

Latest queries sort by `collect_time DESC, id DESC` and select one row. History applies the optional inclusive time bounds, selects the most recent limited subset, and returns it by `collect_time ASC, id ASC`. The service defaults the history limit to 100 and caps it at 500.

## Synthetic example and schema use

The [API fixtures](../api/README.md) use a synthetic interval from `2025-01-01 00:00:00` to `2025-01-01 00:00:05`. The six-record demonstration has primary values `10.0 + 0.25 * i` and auxiliary wavelengths `1550.000 + 0.001 * i` for `i = 0..5`. These are invented software-test values, without physical calibration.

`vendor_source_unified_schema.sql` is an additive manual reference using `CREATE TABLE IF NOT EXISTS` and `INSERT IGNORE`. It does not reconcile an existing incompatible table definition. Its `utf8mb4_0900_ai_ci` collation targets MySQL 8. Review the target database and use the repository's isolated local demo workflow for examples. Once compatible tables exist, application ingestion does not require rerunning this SQL file.
