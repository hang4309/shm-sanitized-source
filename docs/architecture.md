# Architecture and engineering discussion

[Home](../README.md) · [Runbook](runbook.md) · [Backend reference](../shm-backend-fable5-copy/docs/PROJECT_BACKEND_STRUCTURE.md)

## OS265 integration boundary

The original project’s integration chain was validated in the lab: **FBG sensors → OS265 optical-fiber interrogator → vendor software → channel TXT files → Python collector → Spring Boot API → MySQL → Vue dashboard**.

The integration boundary is the vendor software's channel-file output. The public repository extracts a focused demo of this data path. Generated SYNTHETIC channel files enter the existing collector at the same file boundary, making the software workflow reproducible without the lab hardware.

## Responsibilities

1. **File adapter:** discovers eligible channel text files, decodes lines, tracks byte offsets and records source identity. The current parser's fifth whitespace-delimited field is the primary value; the fourth is auxiliary wavelength.
2. **HTTP boundary:** the canonical upload endpoint validates required origin and identity fields. The collector falls back to the legacy fiber upload only when the canonical endpoint returns 404/405.
3. **Storage:** one normalized reading table, one channel mapping table, and one table of module metadata. A composite unique key prevents the same origin record being inserted twice. The source-file MD5 is a path identity component, not a security signature.
4. **Module queries:** the six permitted module keys filter the unified reading table. Mapping resolution tries channel identity, sensor identity, then a payload module key; otherwise the reading remains unassigned.
5. **Presentation:** Vue shares a monitoring page across six module entries. Latest and history poll independently; an in-flight request is skipped rather than queued, and unmount cancels requests.

## Semantics that matter

- File progress and database uniqueness address different failure windows. A successful upload followed by a crash before state persistence can be replayed; the backend may respond with a duplicate-reading business error which the collector recognizes.
- Bounded retries cover connection failures, timeout, HTTP 429 and 5xx. They do not establish exactly-once delivery. Inspect the current textual duplicate detection and response handling before adopting it as a general protocol.
- History selects a limited recent set and returns it chronologically. Defaults and caps are documented in the [API reference](../shm-backend-fable5-copy/docs/API_ENDPOINTS_REFERENCE.md).
- Chart X coordinates are evenly spaced by record order, not proportional to elapsed time. Timestamps are still shown; irregular sampling needs care.
- A constant primary series stays flat even if wavelength changes. Axis padding improves visibility without changing measurements.
- Datetimes are local, timezone-naive strings throughout the application contract. The demo uses fixed strings, not real acquisition latency.

## Useful interview walkthrough

Start with one generated line, identify how it becomes a payload, follow validation and module assignment, then trace the mapper and the frontend curve. Explain which parts you personally worked on accurately; repository presence alone is not evidence of individual authorship.

Questions the source supports discussing:

- What happens if a writer leaves a partial line, a request times out, or the process restarts?
- Which fields define duplicate identity, and what if a file is renamed or reused?
- Why keep raw/primary values separate from wavelength? What calibration would a real strain unit require?
- How do request cancellation and stale-response guards differ from rate limiting?
- What testing is missing before trusting actual equipment or safety decisions?

## Deliberate limits

The demo covers six synthetic CH2 readings. It does not manufacture channels for all six modules. Prediction/crack UI shells, authentication, calibration, broader automated database integration coverage and production operations remain future work. The public documentation/demo update leaves existing application and collector source unchanged.
