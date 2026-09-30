# SHM sanitized source snapshot

This repository contains a clean export of the existing Fable5 refactor source. Application code is copied unchanged from the independently checked, sanitized snapshot. It contains one new initial commit and no prior repository history.

## Layout

- `shm-backend-fable5-copy/`: Spring Boot backend, MyBatis mappings, and OS265 collector source.
- `shm-frontend-fable5-copy/`: Vue frontend and its existing package lock.

## Local setup

The backend uses Java 17 or later. Configure `SHM_DB_URL`, `SHM_DB_USERNAME`, and `SHM_DB_PASSWORD` in your local environment before running it. An example database URL is `jdbc:mysql://localhost:3306/shm_lab?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai`.

The existing table definitions and generic example mapping are in `shm-backend-fable5-copy/docs/sql/vendor_source_unified_schema.sql`. This is a schema/reference script, not a database dump; review it against your own database before use.

The collector defaults to `sample-data/os265` under this repository. No measurement files are included. Supply your own authorized data location with `--data-dir`; the PowerShell launcher uses `-DataDir`. Runtime logs and continuation/deduplication state are not included.

For the frontend, follow its existing README and package scripts. Dependencies and build output are not included.

## Validation scope

Before this export, independent local checks passed for offline Maven `test-compile`, Vite build, Oxlint, ESLint, Java/Mapper references, Python syntax, PowerShell syntax, and collector `--help`. Exported source bytes are checked against the accepted snapshot. These checks do not establish Spring startup, database/device access, live collection, or end-to-end operation.

## Export contents and rights

Old Git metadata, internal handoff/review documents, archives, credentials, database dumps, real measurement workbooks/data, logs, caches, dependencies, and build artifacts are excluded. The original worktree and its backups are unchanged.

Existing third-party notices and dependency license metadata are retained. This export adds no new license and makes no ownership or legal clearance claim.
