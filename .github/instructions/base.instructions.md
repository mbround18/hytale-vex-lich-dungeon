---
name: Base Engineering Principles
applyTo: "**"
---

# Base Engineering Principles

## KISS (Keep It Simple)

- Prefer the simplest solution that fully meets requirements.
- Reduce moving parts; avoid cleverness and premature abstraction.
- Optimize for clarity, testability, and ease of repair.

## Twelve-Factor Alignment (summary)

- One codebase, many deploys.
- Declare and isolate dependencies explicitly.
- Store config in the environment (no secrets in code).
- Treat backing services as attached resources.
- Separate build, release, run stages.
- Run as stateless processes; persist only to backing services.
- Export services via port binding.
- Scale via the process model; favor horizontal scaling.
- Fast startup and graceful shutdown.
- Keep dev/prod parity high.
- Treat logs as event streams (no local log files as source of truth).
- Run admin tasks as one-off processes.
