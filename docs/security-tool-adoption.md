# SHM API Guard adoption

This repository uses the MIT-licensed [SHM API Guard](https://github.com/KarlLee123/shm-security-validation) as a security regression dependency. The [adoption workflow](../.github/workflows/shm-api-guard.yml) fetches tool commit `8a23d8fc8786cf3e5ae656602578b5ee5d3b3900` and runs it on **this workflow's checked-out source**, not the historical application revision in the tool's lock file.

## Source and execution evidence

For a push or manual run, the target is `github.sha`. For a pull request, it is GitHub's actual test merge commit (`github.sha`), including the proposed changes. The same value is passed as the runner's required `--expected-commit`. The runner rejects a different HEAD or dirty source and checks the worktree again after building.

The tool builds the real SHM backend, runs its Maven tests, and loads that JAR into its synthetic integration harness. The actual Spring controllers, services and MyBatis mappers run with an isolated H2 database, loopback listener and synthetic data. Its report contains expected and observed source commit, source tree, tool identity, built/loaded JAR SHA256 values and the process identity.

Seven checks cover latest-query isolation, bound SQL metacharacters, the history result cap and ordering, invalid time input, reversed time ranges, and separate controlled `X-Forwarded-For` and `X-Real-IP` rate-limit experiments. In current-source mode, every expected check must pass. A missing check, security finding, failure or inconclusive check fails CI; historical expected findings cannot make current-source CI green.

Each run uploads reports and logs as `shm-api-guard-<source SHA>` for 90 days. The GitHub run and log identify the checkout even after artifacts expire. The workflow and commit remain public. Reproduce from the pinned tool checkout with:

```sh
python -B scripts/run_integration.py --revision current --expected-commit FULL_SOURCE_SHA --upstream PATH_TO_CLEAN_SHM_CHECKOUT --work-dir PATH_TO_OUTPUT
```

## CI permission and trigger boundary

The workflow uses ordinary `pull_request`, main-branch `push`, and manual triggers. It does not use `pull_request_target`, `workflow_run`, repository dispatch, or cross-repository workflow dispatch. Running the tool's Python script does not start the tool repository's workflows, so there is no A/B trigger cycle.

All action implementations and the tool are pinned to full commit SHAs. The job grants only `contents: read`, checks out without persisted credentials, uses an ephemeral GitHub-hosted runner, supplies no repository secrets to the build and uses no shared build cache. PR source and Maven plugins execute as untrusted code on that isolated runner; a passing run is a bounded regression result, not proof against malicious test evasion. Tool updates require changing the reviewed SHA in this workflow.

## Maintenance and ownership

`hang4309` maintains this application's adoption and upstream fixes. `KarlLee123` maintains the original security tool and its [security policy](https://github.com/KarlLee123/shm-security-validation/blob/main/SECURITY.md). Both accounts have the same owner. This is actual use across two repositories, with no claim of independently controlled third-party adoption.

Both the tool's original code and this repository's original SHM code have an MIT license. The SHM author's explicit grant is in [LICENSE](../LICENSE); [third-party terms](../THIRD_PARTY_NOTICES.md) remain in effect for the respective components.
