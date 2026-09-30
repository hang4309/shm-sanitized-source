# SHM · Sensor Data Pipeline & Dashboard

**From sensor files to queryable readings and interactive trends.**

A Java/Python pipeline that turns industrial sensor text files into structured measurements and a Vue dashboard — with incremental collection, source traceability and a shared query model.

[Quickstart](#quickstart) · [Full application demo](docs/runbook.md) · [Architecture](docs/architecture.md) · [Engineering notes](docs/validation.md) · [中文](docs/README.zh-CN.md)

![SHM architecture: sensor files flow through a Python collector, a Java and MySQL data layer, and a Vue monitoring dashboard.](docs/assets/sensor-pipeline.svg)

## Why this project

A growing sensor file is useful to a device, but harder to explore as application data. Files may still be writing when a collector reads them; uploads can fail; a point on a chart needs a path back to its source.

SHM connects those steps: follow appended records, normalize them through an API, preserve their origin in MySQL, and inspect latest readings or a selected time window in the browser.

## What it does

### Pick up where collection stopped

The Python collector tracks byte offsets and persists progress across runs. Partial-line handling accommodates files still being written; timeouts and bounded retries handle transient upload failures.

### Trace a reading back to its source

Each canonical upload carries its source file, line and offset alongside the sensor, channel and collection time. The storage model uses source identity for duplicate protection, keeping ingestion and later inspection connected.

### Use one data model across monitoring views

A unified reading table and channel-to-module mapping serve latest/history queries. Six views — displacement, acceleration, strain, vibration, stress and deflection — share that model instead of duplicating the ingestion pipeline.

### Explore readings as they arrive

The Vue dashboard combines polling, manual time-window queries, SVG trends and a reading table. Independent in-flight guards, cancellation and stale-response checks keep overlapping requests under control. Primary readings and auxiliary wavelength stay separate.

## Quickstart

**Try the collector without a database.** From a clone of this repository, with Python 3.10+:

```sh
python tools/demo/generate_synthetic.py
python -B -m unittest discover -s tools/demo -p test_demo.py -v
```

The first command creates six **SYNTHETIC** input records at `sample-data/os265/SYNTHETIC_demo_通道2.txt`. The second runs seven tests around the existing collector, including actual HTTP uploads to a local recorder, restart/resume and partial-line handling.

```text
SYNTHETIC: 6 invented records ready at ...
...
Ran 7 tests in ...
OK
```

The sample's primary values rise from **10.00 to 11.25**. The recorder demonstrates the collector locally; to run the Java/MySQL/dashboard chain, use the [application demo guide](docs/runbook.md), including setup commands, API checks and the exact chart time window.

[All demo tools](tools/demo/README.md) · [28-test verification record and scope](docs/validation.md)

## Stack

| Layer | Technology |
| --- | --- |
| File ingestion | Python standard library; OS265-format text adapter |
| API and persistence | Java 17 target · Spring Boot 4 · MyBatis · MySQL 8 schema |
| Dashboard | Vue 3 · Vite · SVG charts |
| Local checks | Python unittest · Node test runner · deterministic synthetic fixtures |

## Inside the implementation

| Decision | Where to look |
| --- | --- |
| Preserve incremental progress and file origin | [Collector modules](shm-backend-fable5-copy/tools/collector/os265/os265_collector) |
| Validate once and resolve channel mappings centrally | [Ingestion service](shm-backend-fable5-copy/src/main/java/com/example/shm/vendorsource/service/impl/VendorSourceServiceImpl.java) |
| Keep origin uniqueness in storage | [Schema](shm-backend-fable5-copy/docs/sql/vendor_source_unified_schema.sql) · [Database reference](shm-backend-fable5-copy/docs/DATABASE_SCHEMA_REFERENCE.md) |
| Coordinate live and manual queries in one page | [Monitoring component](shm-frontend-fable5-copy/src/components/monitor/MonitorModulePage.vue) |
| Preserve a flat signal when its primary value is constant | [Value helpers](shm-frontend-fable5-copy/src/utils/os265Value.js) · [Chart geometry](shm-frontend-fable5-copy/src/utils/monitor/chartGeometry.js) |

[Backend layout](shm-backend-fable5-copy/docs/PROJECT_BACKEND_STRUCTURE.md) · [API reference](shm-backend-fable5-copy/docs/API_ENDPOINTS_REFERENCE.md) · [Request examples](shm-backend-fable5-copy/api/README.md)

## Project status

Current focus: **file ingestion, unified queries and monitoring views**. The six views are mapping-based presentations of readings; physical calibration and prediction/crack integration remain future work. Samples are synthetic.

Local collector/helper checks are documented; the complete Java/MySQL/browser chain and production or safety suitability remain unverified. See [validation and project scope](docs/validation.md) for the full record and retained third-party notices.
