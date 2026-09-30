# API reference

This reference follows the repository's controllers, DTOs, service, and mapper XML. The [API example directory](../api/README.md) contains copyable JSON requests; the [project overview](../../README.md) describes setup and the synthetic demonstration.

All example timestamps, readings, and source metadata in this document are **SYNTHETIC**. They demonstrate software behavior and carry no physical-unit calibration or field-acceptance claim.

## Transport and responses

Use JSON bodies with `Content-Type: application/json`. The local examples assume `http://localhost:8080`; adjust the port to your local backend configuration.

Controllers return:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

The `data` type depends on the endpoint: upload returns null, latest returns a reading or null, and history returns an array. No matching rows is a valid empty result.

Inspect the JSON `code` as well as the HTTP status. Business exceptions use a non-200 body code; the current exception handlers do not set a matching HTTP status. Most validation failures, including duplicate origins, use body code 400. An unknown business-module key uses body code 404. The rate-limit filter separately returns actual HTTP 429 and `Retry-After: 1`.

## Primary-value semantics

`measuredValue`, `rawValue`, and `intensity` represent the primary vendor intensity/energy quantity. Ingestion selects the first available value in that order and fills missing aliases from it. Supply consistent aliases when sending more than one.

`wavelength` and `wavelengthShift` remain auxiliary reference fields. The current backend does not calibrate these readings into microstrain, stress, displacement, or other engineering units. Business module names determine grouping and presentation.

## Canonical upload

`POST /api/sensor/os265/raw/upload`

| Item | Contract |
| --- | --- |
| Controller / DTO | `Os265SensorController` / `VendorReadingUploadDTO` |
| Required identity | `deviceNo`, `sensorId`, and `channelNo` (legacy `fiberNo` is a fallback) |
| Required measurement | At least one of `measuredValue`, `rawValue`, `intensity` |
| Required origin/time | `collectTime`, nonempty `sourceFile`, non-null `sourceLine` |
| Optional fields | `sourceType` (defaults to `OS265`), `moduleKey`, `wavelength`, `wavelengthShift`, `sourceOffset` |
| Success | Body code 200; `data: null` |
| Rate limit | 5 requests/second per client IP and endpoint; burst capacity 150 |

SYNTHETIC first-record example, matching [os265-upload-example.json](../api/os265-upload-example.json):

```json
{
  "sourceType": "OS265",
  "deviceNo": "OS-265",
  "channelNo": "CH2",
  "sensorId": "FBG-STRAIN-CH2",
  "moduleKey": "strain",
  "rawValue": 10.0,
  "measuredValue": 10.0,
  "intensity": 10.0,
  "wavelength": 1550.000,
  "wavelengthShift": 0.0,
  "collectTime": "2025-01-01 00:00:00",
  "sourceFile": "SYNTHETIC_demo_通道2.txt",
  "sourceOffset": 0,
  "sourceLine": 1
}
```

Upload writes to the backend's configured database; use the isolated local demo database for this example. The example has valid synthetic origin metadata, not evidence of a physical sensor record.

The service resolves `moduleKey` from an enabled channel mapping first, then a sensor mapping when no channel mapping exists, then the payload. See the [database reference](DATABASE_SCHEMA_REFERENCE.md) for the exact ordering.

Repeating an upload with the same unique origin key returns a controlled duplicate business error and leaves one stored row. The key includes the source-file string's hash, collection time, and line number. A basename and an absolute path are different source identities even if they refer to the same file.

## Direct OS265 queries

| POST endpoint | Request DTO | Response data |
| --- | --- | --- |
| `/api/data/os265/latest` | `VendorReadingQueryDTO` | A `VendorReadingVO` or null |
| `/api/data/os265/history` | `VendorReadingQueryDTO` | An array of `VendorReadingVO` |

Optional equality filters are `sourceType`, `deviceNo`, `channelNo`, `sensorId`, and `moduleKey`. These direct routes do not add an implicit source-type filter, so supply `sourceType` when querying a database with mixed sources.

SYNTHETIC latest query:

```json
{
  "sensorId": "FBG-STRAIN-CH2"
}
```

SYNTHETIC history query, covering all six demo timestamps:

```json
{
  "sensorId": "FBG-STRAIN-CH2",
  "startTime": "2025-01-01 00:00:00",
  "endTime": "2025-01-01 00:00:05",
  "limit": 50
}
```

History accepts optional inclusive `startTime` and `endTime` in `yyyy-MM-dd HH:mm:ss` format. The start must not be later than the end. The service uses local date/time values without timezone offsets. The default history limit is 100; nonpositive values use the default and values above 500 are capped at 500.

Latest returns the greatest collection time, with ID breaking ties. History first selects the most recent matching subset up to the limit, then returns that subset in ascending collection-time/ID order. Latest ignores history time bounds and limits.

## Business-module queries

`POST /api/data/{moduleKey}/latest`
`POST /api/data/{moduleKey}/history`

The six accepted keys are `displacement`, `acceleration`, `strain`, `vibration`, `stress`, and `deflection`. `MonitorModuleDataController` uses `ModuleDataQueryDTO` and pins the module from the URL.

Optional body filters are `sensorId`, `deviceNo`, and `channelNo`. History also accepts `startTime`, `endTime`, and `limit` with the rules above. The direct-query request examples can be used with the strain routes as supplied; see [strain-latest-example.json](../api/strain-latest-example.json) and [strain-history-example.json](../api/strain-history-example.json).

Modules without matching assigned readings return null for latest and an empty array for history. A route existing does not imply that its module already has data.

## Reading response fields

`VendorReadingVO` exposes source/device/channel/sensor identifiers, the module key, the primary and auxiliary value fields, `collectTime`, `sourceFile`, and `sourceLine`. `fiberNo` is a legacy response alias for `channelNo`. Internal database ID, source-file hash, and source offset are not returned by this VO.

In an isolated database containing only the complete six-record SYNTHETIC demo for the example sensor, the latest response has these relevant fields:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sensorId": "FBG-STRAIN-CH2",
    "moduleKey": "strain",
    "measuredValue": 11.25,
    "rawValue": 11.25,
    "intensity": 11.25,
    "wavelength": 1550.005,
    "collectTime": "2025-01-01 00:00:05"
  }
}
```

This is a selected-fields illustration. After uploading only the single first-record fixture into an empty database, latest instead returns that first record. An existing database may contain other matching readings with later collection times.

## Request limits and compatibility

The six business-module latest/history routes each use a token bucket with a refill rate of 10 requests/second and burst capacity 10, keyed by client IP, method, and path. Canonical upload uses the limit listed above. Excess requests return HTTP 429 and `Retry-After: 1`. The current filter does not cover direct `/api/data/os265/*` queries or legacy fiber routes.

The following legacy POST routes remain as adapters over the shared vendor-source service:

- `/api/sensor/fiber/raw/upload`
- `/api/data/fiber/latest`
- `/api/data/fiber/history`

Legacy uploads missing source metadata receive explicit compatibility markers. New integrations should use the canonical OS265 upload and the appropriate direct or business-module queries.
