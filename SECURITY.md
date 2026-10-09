# Security policy

## Maintenance and supported code

[hang4309](https://github.com/hang4309), the owner of this repository, is responsible for receiving reports, coordinating reproduction and fixes, reviewing changes, and publishing security information for this demonstration. Contributors may propose fixes; repository maintenance and release decisions remain with the owner.

Security fixes target the current `main` branch. This repository does not provide a supported release series or a guaranteed response time. A successful test or demonstration is not certification for deployment against operational equipment.

## Reporting a vulnerability

Use the repository's [private vulnerability reporting page](https://github.com/hang4309/shm-sanitized-source/security/advisories/new) when that feature is available. Include the affected commit, configuration, affected endpoint, expected and observed behavior, and a small reproduction using synthetic data on a system you control. Describe whether a trusted reverse proxy or a forwarding filter is required.

If private reporting is unavailable, open a minimal issue asking the owner for a private reporting channel. Do not include exploit details, credentials, personal data, industrial measurements, or unredacted logs in a public issue. The owner will arrange a private channel before requesting those details.

The maintenance process is: acknowledge and triage the report, reproduce within an authorized local environment, agree on scope and severity, implement and independently review a fix, run relevant regressions, and publish the fix with a description of its limits. Coordinate public disclosure with the owner. Do not probe public services or systems belonging to another party without their permission.

## Deployment boundaries

- This is an engineering demo. The API has no established authentication or authorization boundary; the documented demonstration binds to loopback.
- The rate limiter covers the documented hot POST endpoints. Buckets are per process and endpoint, and active client counts have no hard memory cap. It is not a network firewall, distributed quota service, or general protection against denial of service.
- Direct connections use the socket peer. Only explicitly configured proxy IP literals may supply one valid `X-Real-IP`. The proxy must overwrite that header. Forwarding strategy must remain `NONE`; native container extensions that rewrite the peer are unsupported.
- Keep credentials in your local environment. Use synthetic fixtures and a dedicated local database when reproducing a report.

See the [runbook](docs/runbook.md) for configuration and the [verification record](docs/security-verification.md) for executed tests and evidence boundaries.

## Continuous security regression

This repository adopts the MIT-licensed SHM API Guard maintained by `KarlLee123`. Its [dedicated workflow](.github/workflows/shm-api-guard.yml) pins the tool's commit and checks this repository's actual push or PR merge commit, including the full backend suite and seven real API security checks. See [adoption and source-identity evidence](docs/security-tool-adoption.md). Both repositories have the same owner; this is not a claim of independent third-party adoption.

## Published security records

[GHSA-7hhv-g4ww-c8j3 / SHM-SEC-001](https://github.com/hang4309/shm-sanitized-source/security/advisories/GHSA-7hhv-g4ww-c8j3) records the forwarding-header rate-limit bypass at commit `51aefef11004e79227d47bb870caa0c09dd9b1be`, fixed by the main merge `21159f8b732067cfc248d3e6230abc8001a0e5e8`. It links the actual fix, bounded reproduction and regression evidence.

## Licensing boundary

The author's original SHM code and documentation are licensed under the root [MIT License](LICENSE). Third-party materials retain their original licenses and attribution; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Security maintenance and test results do not change those third-party terms.
