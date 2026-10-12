# Getting started

Run the full ScrumUpdate stack on your machine with Docker. You do not need Java, .NET, Node, or Python installed unless you are building a single service outside Docker.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) with Compose v2 (`docker compose version`)
- A **Claude API key**. AI features do not work without one. Get a key from the [Anthropic console](https://console.anthropic.com/) and set `CLAUDE_API_KEY` in the shell or in the repo-root `.env` before you start. A value in the shell wins over `.env`.

The rest of `.env` is already set for local Docker. You do not need to copy or create another env file.

No Claude key? Point KGS at an OpenAI-compatible endpoint instead (Ollama, Together AI, or OpenAI). See [OpenAI-compatible endpoints](OPENAI.md). The app is designed around Claude Haiku, so other models can give different or unsatisfactory results.

## Start

From the repo root:

```bash
docker compose up --build
```

The first start builds images and seeds Postgres and feature flags. That can take several minutes. Later starts only need:

```bash
docker compose up
```

Stop the stack with `docker compose down`. Adding `-v` also deletes local Postgres and Redis data.

## Open the app

| What | Where |
|------|--------|
| App | http://localhost:4241 |
| Captured email (Mailpit) | http://localhost:8025 |
| Feature flags (Unleash) | http://localhost:4242 |
| Business API (KBS) | http://localhost:4243 |
| AI server (KGS) | http://localhost:9090 |
| Analytics | http://localhost:9000 |
| Jira sync API (KSS) | http://localhost:8000 |
| Postgres | `localhost:5435` |
| Redis | `localhost:6379` |

Postgres and Unleash logins are in `.env` (`POSTGRES_*`, `UNLEASH_ADMIN_PASSWORD`). The Unleash username defaults to `admin`.

## Sign in

Open http://localhost:4241 and choose **Login with Jira**. Atlassian sends the browser back to `http://localhost:4241/login`. The Atlassian app's callback URL must be that exact address.

`JIRA_CLIENT_ID` and `JIRA_CLIENT_SECRET` are already in `.env`. Login works only if that Atlassian app still allows the callback above. If it does not, register your own app and put its id and secret in `.env`, then restart the stack (see [Change a setting](#change-a-setting)).

## Optional pieces

Leave these blank unless you need the feature. The stack still starts. AI is not optional — see [Prerequisites](#prerequisites).

- **Email.** Already pointed at Mailpit. Messages the app sends show up at http://localhost:8025.
- **Microsoft calendar, Teams bot, reCAPTCHA.** Leave the related `.env` values empty. Those features stay off.

## Change a setting

Edit `.env`, then apply it:

```bash
docker compose up -d
```

That recreates containers that picked up the new values. You do not need to rebuild images for an `.env` change. Rebuild only after code or Dockerfile changes:

```bash
docker compose up --build
```

## If something is wrong

- **The app loads, but API calls fail.** `NG_SERVER_PORT` must stay `4243` (the business API) and `NG_APP_PORT` must stay `4241` (the app the browser opens). If those two match, the browser talks to the wrong service.
- **Jira login fails on the way back.** The Atlassian callback must be `http://localhost:4241/login`, and it must match `JIRA_CLIENT_REDIRECT_URI` in `.env`.
- **`relation "khoji_user" does not exist`.** Do not override `SPRING_DATASOURCE_URL` unless it still ends with `?currentSchema=khoji`. Compose already sets this.
- **AI features do nothing.** KGS logs that Claude is not configured. Set `CLAUDE_API_KEY` and run `docker compose up -d kgs`, or follow [OpenAI-compatible endpoints](OPENAI.md).
- **You want a clean database.** `docker compose down -v` deletes local Postgres and Redis data. The next `docker compose up` starts empty.

## Work on one service

Build and test commands are in [CONTRIBUTING.md](../CONTRIBUTING.md). Each service under `src/` has its own README.
