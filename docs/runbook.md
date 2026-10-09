# Run the SYNTHETIC demonstration

[Home](../README.md) · [Verification scope](validation.md)

Use a **dedicated local demo database**. Do not point this demonstration at operational equipment or an existing industrial database. These instructions describe the actual application chain; the current publication's executed checks are listed separately in [validation](validation.md).

## 1. Prerequisites and paths

Clone the public repository, then run the commands below from its root unless a step changes directory:

```sh
git clone https://github.com/hang4309/shm-sanitized-source.git
cd shm-sanitized-source
```

- JDK 17+; the project targets Java 17. Set `JAVA_HOME` for the Maven wrapper.
- Node `^20.19.0 || >=22.12.0` and npm. The current standard-install regression used Node 24.11.1 and npm 11.19.1.
- Python 3.10+ (stdlib only; see the tested interpreter in validation).
- For the full chain: a local MySQL 8 server compatible with `utf8mb4_0900_ai_ci`, and its `mysql` client.
- First-use Maven/npm dependency downloads require network access. Neither dependency directories nor compiled output are committed.

No database or Docker installer is bundled or run by these scripts.

## 2. Create the isolated database and import the existing schema

Open a local MySQL admin session from the repository root:

```sh
mysql -h 127.0.0.1 -P 3306 -u root -p
```

Run the following interactively. Replace the password placeholder with your own **local demo** password. If that database/user already exists, inspect it and choose a fresh demo name instead of replacing existing resources.

```sql
CREATE DATABASE shm_synthetic_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'shm_demo'@'localhost' IDENTIFIED BY 'REPLACE_WITH_LOCAL_DEMO_PASSWORD';
GRANT SELECT, INSERT ON shm_synthetic_demo.* TO 'shm_demo'@'localhost';
USE shm_synthetic_demo;
SOURCE shm-backend-fable5-copy/docs/sql/vendor_source_unified_schema.sql;
SELECT COUNT(*) AS reading_count FROM vendor_source_reading;
SELECT module_key FROM monitor_module_definition ORDER BY sort_order;
```

Expected on a fresh database: **0 readings**, six module definitions, and an example OS-265/CH2 → strain mapping. The schema is a manual, additive reference script; it does not create a database itself. Its historical example comments are not evidence that this synthetic demo or any device was accepted.

## 3. Configure and start the backend

The variable names and example values are in [`.env.example`](../.env.example). Spring Boot and the collector **do not automatically load that file**. Set the variables in the same terminal used to start Java.

PowerShell:

```powershell
$env:SHM_DB_URL = 'jdbc:mysql://127.0.0.1:3306/shm_synthetic_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:SHM_DB_USERNAME = 'shm_demo'
$env:SHM_DB_PASSWORD = Read-Host 'Local demo DB password' -MaskInput
cd shm-backend-fable5-copy
.\mvnw.cmd -DskipTests package
java -jar target/shm-backend-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=8080
```

`-MaskInput` requires PowerShell 7.1+. On older PowerShell, set the password through your local environment manager rather than committing it.

Bash (from the root, in an alternative terminal):

```sh
export SHM_DB_URL='jdbc:mysql://127.0.0.1:3306/shm_synthetic_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
export SHM_DB_USERNAME='shm_demo'
read -rsp 'Local demo DB password: ' SHM_DB_PASSWORD
export SHM_DB_PASSWORD
cd shm-backend-fable5-copy
./mvnw -DskipTests package
java -jar target/shm-backend-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=8080
```

`-DskipTests` builds the application without running tests. Run `./mvnw verify` (PowerShell: `.\mvnw.cmd verify`) for the complete backend suite; its context test uses an isolated in-memory H2 database and requires no operational database. Confirm application startup and the absence of datasource errors before proceeding. The published API has no established authentication boundary; keep this demo on loopback.

For a reverse proxy, configure `SHM_RATE_LIMIT_TRUSTED_PROXY_IPS` with exact IPv4/IPv6 socket addresses and ensure that proxy **overwrites** `X-Real-IP` with one numeric client address. Leave the setting empty for direct access; never mark an untrusted caller or an entire shared access host as a trusted proxy. CIDRs, hostnames, ports, IPv6 zones, shortened IPv4 and IPv4 with leading zeros are rejected. Missing, invalid or repeated `X-Real-IP` values share the proxy's own bucket; `X-Forwarded-For` and `Forwarded` are never identities.

Keep `server.forward-headers-strategy=NONE`, as committed in `application.yml`. Setting `SERVER_FORWARD_HEADERS_STRATEGY=NATIVE` or `FRAMEWORK` prevents startup because those modes can rewrite the socket peer. Do not install a container `RemoteIpValve` or other native peer-rewriting extension outside Spring configuration. Standard servlet wrappers are unwrapped defensively for the peer, headers and protected path; malformed or excessive wrapping is rejected. See the [security policy](../SECURITY.md) and [security verification record](security-verification.md).

## 4. Generate and ingest six records

In another terminal at the repository root:

```sh
python tools/demo/generate_synthetic.py
python shm-backend-fable5-copy/tools/collector/os265/os265_file_to_mysql_collector.py --data-dir sample-data/os265 --base-url http://127.0.0.1:8080 --state-file sample-data/os265/SYNTHETIC-demo-state.json --once --no-verify
python tools/demo/verify_api.py --base-url http://127.0.0.1:8080
```

The generator writes `SYNTHETIC_demo_通道2.txt`. All six timestamps and values are artificial:

| Property | Expected |
| --- | --- |
| Sensor / module | FBG-STRAIN-CH2 / strain (existing example identifiers) |
| Local timestamp strings | 2025-01-01 00:00:00 through 00:00:05 |
| Primary values | 10.00, 10.25, 10.50, 10.75, 11.00, 11.25 |
| Auxiliary wavelengths | 1550.000 through 1550.005 in steps of 0.001 |
| Source lines | 1 through 6 |

The strict verifier reads canonical and strain latest/history endpoints. It must reject missing, reordered or changed records. Do not substitute collector exit code or log text for this check.

Run the same collector command again: progress should prevent another upload of these lines. In the dedicated database, independently inspect:

```sql
SELECT sensor_id, module_key, measured_value, wavelength, collect_time, source_line
FROM shm_synthetic_demo.vendor_source_reading
WHERE source_file = 'SYNTHETIC_demo_通道2.txt'
ORDER BY collect_time, id;
SELECT COUNT(*) FROM shm_synthetic_demo.vendor_source_reading
WHERE source_file = 'SYNTHETIC_demo_通道2.txt';
```

Expected: six rows, count still six after replay. To repeat from scratch, use a new empty demo database and a new state-file path. Do not delete application data to reset a demonstration.

## 5. Start the unchanged frontend

In another terminal:

```sh
cd shm-frontend-fable5-copy
npm ci
npm run dev -- --config ../tools/demo/vite.config.mjs
```

The committed dependency graph supports standard `npm ci` on the tested Node/npm versions. The [verification record](validation.md) describes the lock consistency repair and its checks.

Open the local URL Vite prints (normally `http://127.0.0.1:5173`). The demo-only Vite override binds to IPv4 loopback and forwards `/api` to `http://127.0.0.1:8080`, avoiding localhost IPv4/IPv6 differences. The original application configuration is unchanged.

Select **监测数据 → 应变** (monitoring → strain). Enter sensor `FBG-STRAIN-CH2`, start `2025-01-01 00:00:00`, end `2025-01-01 00:00:05`, limit `50`, then **查询**. Expect six table rows and six points rising from 10.00 to 11.25, with the latest timestamp at 00:00:05. The old synthetic timestamps produce a large displayed lag; that is not a latency benchmark. Other modules have no synthetic mapped readings.

Check the browser Network panel for successful `/api/data/strain/latest` and `/api/data/strain/history` responses. A successful helper test alone does not verify browser rendering. Close the page and stop foreground servers with Ctrl+C when finished.

## Troubleshooting and build checks

- Empty history: confirm strain, sensor ID, fixed time window, schema mapping, and six DB rows. The generator's fixed date is intentionally unrelated to today's date.
- Collector uploads zero after resetting the DB: use a fresh state-file path. Existing progress belongs to the earlier DB contents.
- HTTP errors: inspect backend console and the actual JSON `code`, not just status 200. If a log says uploaded but verification fails, treat the demo as failed.
- MySQL access denied or public-key retrieval errors: check your local account/authentication and JDBC configuration; do not weaken a production database to run this demo.
- A successful build is not a running system. `npm run build` creates frontend assets; `npm run preview` is not a production deployment recipe. Configure a same-origin API reverse proxy for deployment.
- The existing `npm run lint` scripts use auto-fix. For review-only checks after `npm ci`, use `npx --no-install oxlint .` and `npx --no-install eslint .`.
