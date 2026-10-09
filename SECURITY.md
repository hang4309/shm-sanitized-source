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

## Licensing boundary

This policy grants no copyright or license rights and adds no repository-wide license. Existing third-party notices and terms remain applicable to their respective material. Do not treat the security documentation as permission to relicense third-party code.
