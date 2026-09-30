# Verification record

This record distinguishes executed checks from instructions and earlier source checks. It is not a production acceptance report.

## Executed for this documentation/demo update

On 2026-09-30, in the isolated public worktree on Windows:

| Check | Result | What it establishes |
| --- | --- | --- |
| Python 3.14.6: `python -B -m unittest discover -s tools/demo -p "test_*.py" -v` | PASS: 24 tests, 1.346 s | 7 generator/collector tests and 17 verifier tests, including rejection cases |
| Node 24.11.1: `node --test tools/demo/test_chart.mjs` | PASS: 4 tests | Existing primary-value and SVG geometry helpers on synthetic inputs |
| `python tools/demo/generate_synthetic.py` | PASS: six records generated | Default collector input path and deterministic synthetic file creation |
| `python tools/demo/verify_api.py --help` | PASS | CLI entry/argument help, not API connectivity |
| `node --check tools/demo/vite.config.mjs` | PASS | Demo config syntax only, not dependency resolution or a running dev server |

The collector tests run byte-identical copies of the existing collector in temporary directories against an HTTP recorder bound to 127.0.0.1. They assert actual upload payloads, byte/line progress, restart without duplicate requests, partial-line continuation and malformed-line handling. The recorder returns test responses without a database.

Verifier unit tests deliberately supply malformed, missing, extra, reordered and incorrect-value responses. Their passing result establishes the checker's behavior, **not that the actual backend returned those records**. Node helper tests do not mount Vue or render a browser page.

## Source preservation and earlier checks

The initial public source snapshot was commit `460ee8bb8c31e3f78d70231d8af2ead4112d9186`. This update changes documentation and adds isolated examples/demo tests; existing Java, Vue/JS application code, Python collector, Maven/npm build declarations and SQL schema remain unchanged.

Before the initial export, independent local checks of the accepted source passed offline Maven `test-compile`, Vite build, Oxlint, ESLint, Java/Mapper references and Python/PowerShell syntax. Those results used existing local dependencies. They are **not a fresh-clone dependency installation test**, and no full build was repeated solely for documentation.

## NOT VERIFIED in this update

- Installing dependencies from an empty cache / a new clone.
- MySQL schema execution, Spring Boot startup, actual persistence and duplicate handling in MySQL.
- The four API queries against the Java backend.
- Browser rendering, development proxy operation and the complete input → collector → Java → MySQL → query → Vue chain.
- Physical sensors, calibration, industrial deployment, production load, authentication or safety suitability.

MySQL/Docker was not available in the inspected local environment. No database was installed, no load test was run, and no industrial device was contacted. The [runbook](runbook.md) supplies reproducible commands and acceptance observations for the remaining chain; it does not mark them as passed. Stop and investigate if the strict verifier fails or the expected six rows are absent.
