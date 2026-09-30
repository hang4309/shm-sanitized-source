# Backend structure

This reference describes the code included in this repository. See the [project overview](../../README.md) for setup and the synthetic demonstration, the [API reference](API_ENDPOINTS_REFERENCE.md) for request contracts, and the [database reference](DATABASE_SCHEMA_REFERENCE.md) for persistence.

## Data flow

```text
OS265 vendor-format channel file
  -> Python collector
  -> POST /api/sensor/os265/raw/upload
  -> VendorSourceService
  -> MySQL vendor_source_reading
  -> latest/history queries
  -> Vue monitoring pages
```

The backend stores readings in one shared model. Channel mappings assign readings to six business views: displacement, acceleration, strain, vibration, stress, and deflection. These views do not imply six separate ingestion pipelines or calibrated calculations for each engineering quantity.

The public demonstration uses SYNTHETIC records. Its values and timestamps illustrate software behavior; they do not establish sensor calibration, structural safety, or field acceptance.

## Packages

| Package under `com.example.shm` | Responsibility |
| --- | --- |
| `vendorsource` | Upload/query DTOs, reading and mapping entities, mapper interfaces, response VO, and the canonical service. |
| `monitor` | Business-module endpoints and the fixed module whitelist. |
| `fiber` | Transitional controllers that delegate to the shared vendor-source service. |
| `common` | Response wrapper, exception handling, request rate limiting, and connection-pool diagnostics. |

## Controllers

| Controller | POST routes | Role |
| --- | --- | --- |
| `Os265SensorController` | `/api/sensor/os265/raw/upload` | Canonical upload; requires source-file metadata. |
| `Os265DataController` | `/api/data/os265/latest`, `/api/data/os265/history` | Direct queries with optional vendor-reading filters. |
| `MonitorModuleDataController` | `/api/data/{moduleKey}/latest`, `/api/data/{moduleKey}/history` | Queries constrained to a business module. |
| `FiberSensorController` | `/api/sensor/fiber/raw/upload` | Legacy upload adapter. |
| `FiberDataController` | `/api/data/fiber/latest`, `/api/data/fiber/history` | Legacy query adapters. |

`MonitorModules.ALL` defines the accepted module keys. `MonitorModuleServiceImpl` validates the URL key, copies the supported request filters into a `VendorReadingQueryDTO`, and delegates to `VendorSourceService`. Unknown module keys produce a business error.

## Ingestion and query behavior

`VendorSourceServiceImpl` owns normalization and persistence:

- Canonical uploads require nonempty `sourceFile` and a non-null `sourceLine`. Missing `sourceType` defaults to `OS265`.
- `deviceNo`, `sensorId`, a channel (`channelNo` or legacy `fiberNo`), `collectTime`, and at least one primary-value field are required.
- The primary-value preference is `measuredValue`, then `rawValue`, then `intensity`. Missing aliases fall back to that value. Integrations should send consistent aliases.
- `wavelength` and `wavelengthShift` are auxiliary fields; this service does not perform physical calibration.
- The backend computes `sourceFileHash` from the trimmed source-file string. The database unique key prevents a repeated origin record from creating a second row.
- Module assignment first uses an enabled channel mapping, then an enabled sensor mapping, then a supplied `moduleKey`, and otherwise remains unassigned.
- Latest selects the greatest collection time, with database ID breaking ties. History selects the most recent matching rows and returns that subset in ascending collection-time/ID order.
- History defaults to 100 rows when the limit is absent or nonpositive and caps the limit at 500. Optional time bounds are inclusive.
- Legacy fiber ingestion supplies explicit compatibility source markers when those fields are missing.

A duplicate insert becomes a controlled business error. It is not reported as a new successful insertion.

## Persistence and configuration

| Mapper | XML file | Table |
| --- | --- | --- |
| `VendorSourceReadingMapper` | `src/main/resources/mapper/vendorsource/VendorSourceReadingMapper.xml` | `vendor_source_reading` |
| `VendorChannelMappingMapper` | `src/main/resources/mapper/vendorsource/VendorChannelMappingMapper.xml` | `vendor_channel_mapping` |

`application.yml` loads mapper XML from `classpath:mapper/**/*.xml`. Database connection settings come from `SHM_DB_URL`, `SHM_DB_USERNAME`, and `SHM_DB_PASSWORD`. The checked-in Hikari configuration sets a maximum pool size of 10 and a connection timeout of 30 seconds. `DataSourcePoolStartupLogger` reports effective settings at startup.

The [SQL reference](sql/vendor_source_unified_schema.sql) creates the three shared tables and example module/mapping rows. It is a manual schema reference; see the database document before using it.

## Responses and request limits

Controllers use `Result<T>`, containing `code`, `message`, and `data`. Successful requests have body code 200. `GlobalExceptionHandler` returns business errors through this wrapper; clients must inspect the body code because those handlers do not set a matching HTTP error status.

`ApiRateLimitFilter` uses in-memory token buckets keyed by client IP, HTTP method, and path:

| Protected POST route | Refill rate | Burst capacity |
| --- | --- | --- |
| Six business-module `latest` and `history` routes | 10 requests/second | 10 |
| `/api/sensor/os265/raw/upload` | 5 requests/second | 150 |

Rejected requests return HTTP 429, a body code of 429, and `Retry-After: 1`. The direct `/api/data/os265/*` and legacy fiber routes are outside this filter's current route pattern. These limits describe the implementation, not a throughput benchmark.

## Collector layout

`tools/collector/os265/os265_file_to_mysql_collector.py` is the command-line entrypoint; `run_os265_file_collector.ps1` is the PowerShell launcher. The adjacent `os265_collector` package separates configuration, file discovery, parsing, record normalization, scanning, state persistence, logging, and HTTP upload.

The collector reads vendor-format `*_通道*.txt` files, skips `物理量CH*.txt` files, and records the source file, line, and offset. Its implementation handles incremental reads, retries, and saved continuation state. See [API examples](../api/README.md) for synthetic request payloads and the project overview for a runnable demonstration.
