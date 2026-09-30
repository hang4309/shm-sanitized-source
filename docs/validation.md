# Verification record

## Standard-install repair

The frontend now installs with **standard `npm ci`** under Node **24.11.1** and npm **11.19.1**.

The previous lock omitted the top-level `@emnapi/runtime` peer required by `@napi-rs/wasm-runtime`. Separately, the unused `vite-plugin-vue-devtools` dependency brought inspection/RPC packages whose Vite peer ranges excluded the locked Vite 8 version. The application configuration never enabled that plugin.

The repair removes that unused development dependency and regenerates the existing lock through npm with normal peer resolution. npm added `@emnapi/runtime@1.11.3`, removed 81 obsolete lock entries, and retained **every remaining package version**, including Vue **3.5.32** and Vite **8.0.8**. Application code and lint/build scripts are unchanged.

| Check on a clean isolated candidate | Result |
| --- | --- |
| Standard `npm ci`, no pre-existing `node_modules` | PASS — 162 packages installed |
| Independent second clean-directory `npm ci` + dependency tree | PASS — 162 packages, zero tree issues |
| `npm run build` | PASS |
| `npm exec --offline -- oxlint .` | PASS |
| `npm exec --offline -- eslint . --no-cache` | PASS |
| `npm ls --all --json` | PASS |
| Python demo regression suite | PASS — 24 tests |
| Node chart/value helper suite | PASS — 4 tests |

No `legacy-peer-deps` or force mode was used for these checks. A separate verifier independently passed standard installation in another empty directory and found zero dependency-tree issues. Manifest and lock hashes remained unchanged during both installs.

The initial failure was `EUSAGE: Missing @emnapi/runtime@1.11.3 from lock file`. The original project used `npm install`; the public runbook's stricter clean-install command exposed an inherited lock/peer issue. The earlier compatibility-flag experiment diagnosed that issue; it is not the published installation procedure.

## Real synthetic application replay

On **2026-09-30**, an independent runtime check exercised public commit `8bf3f226083200f15b20194929361689976cb385` through the actual collector, packaged Spring Boot application, isolated MySQL, API queries and Chrome/Vue UI.

| Observation | Result |
| --- | --- |
| Schema initialization | 0 initial readings, 6 module definitions, CH2 → strain mapping |
| First collector run | 6 rows persisted |
| Same-state restart | 0 uploads; database remains at 6 rows |
| Fresh-state replay | 6 records replayed; database still contains 6 rows |
| Canonical and strain latest/history APIs | All four endpoints return the expected values and source identity |
| Primary values | 10.00, 10.25, 10.50, 10.75, 11.00, 11.25 |
| Latest reading | 11.25 at `2025-01-01 00:00:05` |
| Actual Vue page | 6 table rows, 6 rising SVG points, 0 browser errors |
| Independent evidence review | 24 checks passed |

That initial runtime check used the compatibility install before the lock repair. The real application path was then repeated with the **repaired standard-install candidate** at **15:29:37–15:29:56 UTC**. All ingestion, restart, duplicate-replay and four-API checks passed again; the real browser passed **18/18 assertions** with six matching table rows/curve points and zero errors. The verified backend JAR was reused after source and packaged-class comparison; the frontend used the newly installed dependencies.

[Actual synthetic-demo screenshot](assets/os265-synthetic-strain.png) · [Machine-readable hashes and results](evidence/dependency-runtime-20260930.json)

The six input records are **SYNTHETIC OS265-format channel data**. MySQL, Spring Boot/MyBatis, the original Python collector, Vite, Vue and Chrome are real processes. This exercises CH2/strain through the application's file boundary; the original project's OS265 hardware laboratory background is described separately on the homepage.

## Environment and execution details

| Component | Recorded version |
| --- | --- |
| Node / npm | 24.11.1 / 11.19.1 |
| Vue / Vite | 3.5.32 / 8.0.8 |
| Java / Maven | Temurin 25+36, Java 17 compilation target / 3.9.11 |
| Spring Boot / MySQL | 4.0.5 / 8.0.34 |
| Python / Chrome | 3.14.6 / 153 |

The runtime check used a separate MySQL datadir and loopback ports **13316 / 18080 / 15173**, with a demo-only Vite proxy override. Backend packaging used the installed Maven dependency cache. The original database and industrial devices were not used. Recorded test services are shut down after the run.

The documented default-port setup and first-use Maven wrapper download were not separately replayed. The test covers deterministic ingestion, query, resume, duplicate replay and the strain chart; other monitoring views were not populated with additional fixtures.

## Source preservation

The clean public history begins at `460ee8bb8c31e3f78d70231d8af2ead4112d9186`. The dependency repair changes only the frontend package declaration and npm-generated lock, plus documentation and demo evidence. Java, Vue/JS application code, Python collector, SQL schema and other build configuration remain unchanged.

The independent source audit matched the full original backup and public export, resolved the Java/MyBatis references, and confirmed that excluded historical XLSX files are not required by the active runtime chain. Before the real runtime check, offline compilation, Vite build, lint and parser/reference checks had also passed.

## Public source scope and notices

The public repository preserves the existing Fable5 refactor source. Old Git history, internal handoff material, backups, credentials, real measurement files, runtime logs and dependency/build output are excluded.

The six monitoring views organize unified readings through module mapping. Prediction and crack pages remain extension points; physical calibration and broader production operations are separate from this synthetic replay.

Existing third-party notices and dependency license metadata are retained. No new license is granted, and sanitization is not a legal determination of ownership.
