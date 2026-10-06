# Features

Start the stack first. See [Getting started](GETTINGSTARTED.md). Then turn on the features you want to try.

A feature that is off by default needs both steps below. Skipping either one leaves it hidden.

## 1. Enable it in the repo

Open [`src/unleash-init/features-map.json`](../src/unleash-init/features-map.json). Each entry has `"defaultEnabled"`. Set that to `true` for the feature you want. Save the file.

From the repo root:

```bash
docker compose up
```

The file is read again on every start. Leaving `"defaultEnabled": false` keeps that feature off. You do not need to rebuild images for this change.

| Name in the file | What it is | On by default |
|------------------|------------|----------------|
| `scrum-updates` | Daily scrum updates in My Work | yes |
| `kgs-llm` | AI calls | yes |
| `my-worklogs` | Worklog dashboard and AI worklog suggestions | no |
| `worklog-insights` | Team worklog insights | no |
| `team-pulse` | Team Pulse | no |
| `standup-board` | Standup Board | no |
| `scrum-assistant` | AI chat | no |
| `plan-details` | Plan details on the profile | no |
| `sprint-watch` | Background sprint checks | no |

`angular-posthog`, `kgs-posthog`, and `cheat-codes` are also in that file and are off. Leave them off unless you mean to turn them on.

Each entry also lists `jiraScopes`. Those are the Jira permissions that feature needs.

## 2. Unlock it in the app

1. Open http://localhost:4241 and choose **Login with Jira**.
2. Connect the Jira site you want to use.
3. Press the enable button on the feature card:
   - **For Me** — Enable AI Timelog
   - **For My Team** — Enable Team Dashboard
   - **Smart Categories** — Enable Smart Categories
   - **Worklog Dashboard** — Enable Worklog Dashboard
   - **Team Pulse** — Enable Team Pulse
   - **Standup Board** — Enable Standup Board

That binds the feature to your Jira site. The first time, Jira may ask you to allow more permissions. Accept them. If it refuses, the Atlassian app is missing a scope from `jiraScopes` for that feature. Add the scope on the Atlassian app, put the app id and secret in `.env` if they are not already there, and sign in again. Callback and `.env` details are in [Getting started](GETTINGSTARTED.md).

If the card or tab never appears, that name is still `"defaultEnabled": false`. Set it to `true` and run `docker compose up` again.

## Optional integrations

Microsoft calendar, the Teams bot, and reCAPTCHA stay off until the matching `.env` values are set. The stack still starts without them. See [Getting started](GETTINGSTARTED.md).

If the app loads but calls fail, or Jira login bounces, check [Local Docker setup](LOCAL_DOCKER.md) before changing ports.
