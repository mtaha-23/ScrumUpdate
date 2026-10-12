# Contributing to ScrumUpdate

Thanks for your interest! We're glad you're here. Contributions of all kinds are welcome — bug reports, docs, and pull requests.

## Issues

- Search existing issues before opening a new one.
- Use a clear, descriptive title and include steps to reproduce for bugs.

## Pull Requests

1. Fork the repo and create a feature branch (`feature/your-change`).
2. Keep changes focused and consistent with the surrounding code (see language conventions below).
3. Make sure the relevant tests and build pass locally.
4. Open a pull request describing what you changed and why.

### A note on review time

This project is maintained by a small team with **no dedicated governing body or full-time PR reviewers**. Reviewers are volunteers doing their best between their day-to-day work, so your PR may take a while to get attention. Please be patient and don't take it personally — we do review everything, just not always quickly. Ping us politely after a couple of weeks if there's been no movement.

## Build & Test

| Project | Build | Test |
|---|---|---|
| Kgs (.NET) | `dotnet build src/Kgs/KhojiGenAIServer/KhojiGenAIServer.csproj` | `dotnet test tests/Uworx.Khoji.Tests --filter TestCategory=Unit` |
| KBS (Java 21) | `cd src/khoji-business-server && mvn package -DskipTests` | `cd src/khoji-business-server && mvn test` |
| Angular | `cd src/khoji-angular && yarn install && yarn build` | `cd src/khoji-angular/angular && yarn test` |
| KSS (Python) | `cd src/kss && pip install -r requirements.txt` | `cd src/kss && pytest` |
| Analytics (Python) | `cd src/khoji-analytics/backend && pip install -r requirements.txt` | `cd src/khoji-analytics/backend && pytest` |

> Note: Java builds require JDK 21. The KSS and Analytics test suites need PostgreSQL and Redis (`docker compose up` at the repo root — see [Getting started](docs/GETTINGSTARTED.md)). More detail in `AGENTS.md`.

## Code Conventions

- **C#**: 4-space indent, PascalCase types/methods, camelCase locals/params.
- **Java**: 2-space indent, Lombok, Spring Boot.
- **TypeScript/Angular**: 2-space indent, single quotes, semicolons.
- **Python**: FastAPI + Celery.

## License

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE.md).