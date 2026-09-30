# Backend API JSON Examples

All five JSON files in this folder are **SYNTHETIC** fixtures for a disposable local demo. Their identifiers are conventional demo identifiers; their timestamps, values, and file provenance are fabricated. They are not observed measurements, calibration results, or evidence of hardware accuracy.

The upload endpoint writes to the database configured for the running backend. Use these fixtures only with a disposable local demo database. The JSON files do not configure or isolate that database for you.

See the [repository README](../../README.md) for the complete local demo setup.

## Fixtures and routes

| File | Endpoint | Purpose |
| --- | --- | --- |
| `os265-upload-example.json` | `POST /api/sensor/os265/raw/upload` | Insert one synthetic source reading. |
| `os265-latest-example.json` | `POST /api/data/os265/latest` | Query the latest reading for the demo sensor. |
| `os265-history-example.json` | `POST /api/data/os265/history` | Query the demo sensor in the synthetic time window. |
| `strain-latest-example.json` | `POST /api/data/strain/latest` | Query the latest reading within the strain module. |
| `strain-history-example.json` | `POST /api/data/strain/history` | Query history within the strain module. |

Run the commands from the backend project root after configuring and starting the backend against a disposable local database. Adjust the example port to match your local configuration.

## PowerShell usage

```powershell
$baseUri = 'http://localhost:8080'
$body = Get-Content -Raw -Encoding UTF8 .\api\os265-upload-example.json
Invoke-RestMethod -Method Post `
  -Uri "$baseUri/api/sensor/os265/raw/upload" `
  -ContentType 'application/json; charset=utf-8' `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body))

$queries = @{
  'os265-latest-example.json' = '/api/data/os265/latest'
  'os265-history-example.json' = '/api/data/os265/history'
  'strain-latest-example.json' = '/api/data/strain/latest'
  'strain-history-example.json' = '/api/data/strain/history'
}
foreach ($entry in $queries.GetEnumerator()) {
  $body = Get-Content -Raw -Encoding UTF8 (Join-Path '.\api' $entry.Key)
  Invoke-RestMethod -Method Post `
    -Uri ($baseUri + $entry.Value) `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
}
```

## curl usage

```bash
BASE_URL='http://localhost:8080'
curl -X POST "$BASE_URL/api/sensor/os265/raw/upload" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @api/os265-upload-example.json

curl -X POST "$BASE_URL/api/data/strain/history" \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @api/strain-history-example.json
```

Use the matching fixture and endpoint from the table for the other queries. In Windows PowerShell, use `curl.exe` when calling the curl executable.

## Data semantics and expected responses

`rawValue`, `measuredValue`, and `intensity` represent the primary business quantity and act as fallbacks for one another in the service. They use the same synthetic number in this upload fixture. `wavelength` and `wavelengthShift` are auxiliary fields; these examples do not establish a calibrated conversion or a physical strain unit.

The upload fixture contains one record at `2025-01-01 00:00:00`, with a primary value of `10.0` and a wavelength of `1550.000`. Both history fixtures cover `2025-01-01 00:00:00` through `2025-01-01 00:00:05`, with a limit of `50`. Timestamps use the backend's `yyyy-MM-dd HH:mm:ss` format and carry no explicit timezone offset.

After uploading only this one record into a clean demo database, latest queries return this one record. If a separate six-record demo sequence has been loaded, with one record per second, primary values increasing by `0.25` and wavelengths by `0.001`, its last record is at `00:00:05`, with a primary value of `11.25` and a wavelength of `1550.005`. The single upload fixture does not create that full sequence. Existing records and configured module mappings can affect query results.

Application responses use a `code`, `message`, and `data` wrapper. Check the response body's `code` as well as the HTTP status: a controlled business error can have an HTTP `200` response with a non-success application code. A successful upload returns:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

For a successful latest query, `data` is one `VendorReadingVO` object or `null` when no reading matches. For a successful history query, `data` is a list of those objects, or `[]` when no reading matches. The strain routes restrict results to the strain module. Empty results are possible before loading the matching fixture.

`sourceFile` and `sourceLine` are required provenance fields for canonical uploads. The synthetic filename is a provenance label; uploading this JSON does not read that file from disk. Repeating the same source record returns a controlled duplicate-reading business error; it is not silently accepted as a successful no-op. Use a clean disposable demo database when repeating the walkthrough.

Excessive bursts to protected endpoints can return HTTP `429` with `Retry-After: 1`. Respect that delay before retrying. A rate-limit response does not mean the synthetic fixture has been accepted.
