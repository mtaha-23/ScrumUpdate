# ScrumUpdate: AI-Powered Productivity and Analytics for Scrum Teams

## 🧭 Overview

**ScrumUpdate** is an AI-driven productivity and analytics platform designed to streamline workflows for **software engineering scrum teams** and their **team leaders/managers**.  
By integrating seamlessly with **Jira** and **Microsoft 365 (Outlook Calendar)**, ScrumUpdate simplifies worklog management, enhances sprint visibility, and automates scrum reporting — all powered by a robust analytics and AI engine.

## Get started

You need [Docker](https://docs.docker.com/get-docker/) with Compose v2. You do not need Java, .NET, Node, or Python installed to run the stack.

1. In the repo-root `.env`, set `CLAUDE_API_KEY` to your [Anthropic](https://console.anthropic.com/) key. A value already in your shell wins over `.env`. AI features do nothing without a key. No Claude key? See [OpenAI-compatible endpoints](docs/OPENAI.md).
2. From the repo root, start everything:

```bash
docker compose up --build
```

The first start builds images and can take several minutes. Later starts only need `docker compose up`. Stop with `docker compose down`. Adding `-v` also deletes local database data.

3. Open http://localhost:4241 and choose **Login with Jira**.

Ports, the Jira callback URL, email, and what to do when login or API calls fail are in [Getting started](docs/GETTINGSTARTED.md). If a port or redirect looks wrong, check [Local Docker setup](docs/LOCAL_DOCKER.md) before changing anything.

## Turn a feature on

Two steps. Both are required for a feature that is off by default. The list of names, what each one does, and the buttons you will see are in [Features](docs/FEATURES.md).

1. Open [`src/unleash-init/features-map.json`](src/unleash-init/features-map.json). Set `"defaultEnabled": true` on the feature you want (`my-worklogs`, `scrum-assistant`, `worklog-insights`, `team-pulse`, `standup-board`, and the rest). Save the file. From the repo root, run `docker compose up` again. That file is applied on every start. A feature left as `false` stays hidden.
2. Sign in, connect your Jira site, and press the enable button on that feature (for example **Enable AI Timelog**, **Enable Team Dashboard**, **Enable Worklog Dashboard**, **Enable Team Pulse**, **Enable Standup Board**). That unlocks it for your Jira site. Jira may ask you to allow extra permissions the first time. Accept them.

`scrum-updates` is already on. Microsoft calendar, the Teams bot, and reCAPTCHA stay off until the matching `.env` values are set. See [Getting started](docs/GETTINGSTARTED.md).

## 👥 User Categories & Features

### 1. For Scrum Team Members
ScrumUpdate empowers individual team members to stay on top of their daily and sprint activities with the help of AI assistance and automation.

#### Key Features
- **AI Worklog Recommendations:**  
  Suggests which Jira tickets a user should log work against based on activity and calendar data.
- **Jira & Outlook Calendar Integrations:**  
  Connects directly with Jira for task management and Outlook Calendar (M365) for time tracking.
- **AI-Generated Scrum Updates:**  
  Automatically drafts daily scrum updates.
- **AI-Generated Weekly Retrospectives:**  
  Summarizes accomplishments, blockers, and learnings.
- **Teams Bot Interaction:**  
  Users can:
  - Set **daily worklog reminders**.
  - Receive **adaptive cards** for quick worklog submissions directly from Microsoft Teams.
  - Query **scrum updates** and **retrospectives** through conversational interactions.

---

### 2. For Scrum Leaders / Managers / Supervisors
ScrumUpdate offers a comprehensive analytics solution for engineering leaders to track productivity and team health.

#### Key Features
- **Worklog & Sprint Dashboards:**  
  Real-time visualization of team performance metrics built on top of Jira data.
- **Worklog Dashboard:**  
  Aggregates and analyzes time logged by team members.
- **Email Reminder Setup:**  
  Allows supervisors to configure periodic reminders for team members to submit their worklogs.
- **AI-Assisted Insights:**  
  Extracts key metrics and trends for sprint retrospectives, blockers, and productivity.

---

## ⚙️ System Architecture

### 1. **Data Synchronization**
- ScrumUpdate periodically syncs data from **Jira** into a **PostgreSQL** database.
- The sync process ensures all analytics and AI modules operate on fresh and accurate data.

### 2. **Analytics Engine**
- Queries the PostgreSQL database to prepare analytical datasets.
- Executes **Python routines** for metric computation, transformations, and anomaly detection.
- Optionally leverages **Large Language Models (LLMs)** to interpret data contextually and generate human-readable insights.

### 3. **Data Presentation Layer**
- The processed metrics and insights are presented through a modern **Angular-based web frontend**.
- Interactive dashboards visualize sprints, worklogs, and performance indicators for both individual contributors and managers.

### 4. **Teams Bot Component**
- Integrated **Microsoft Teams Bot** enables:
  - Interactive notifications and reminders.
  - Submission of worklogs via adaptive cards.
  - Retrieval of scrum updates and retrospective summaries on demand.

---

## 🧩 Technology Stack

| Component | Technology |
|------------|-------------|
| **Frontend** | Angular |
| **Backend** | Java (Spring Boot), .NET (Minimal Web APIs) and Python (FastAPI) |
| **Database** | PostgreSQL |
| **Integrations** | Jira API, Microsoft Graph and Teams Bot |
| **AI Layer** | Anthropic / Claude |
| **Infrastructure** | Cloud-based (containerized microservices) |

---

## 🚀 Benefits

- Automates tedious worklog and scrum update tasks.
- Reduces reporting overhead for developers and managers.
- Provides actionable insights into sprint progress and productivity trends.
- Enhances communication and accountability through Teams integration.

## 📢 Notices

* **Haiku 4.5** support ends **2026-10-15** (claude-haiku-4-5-20251001)
    - https://docs.claude.com/en/docs/about-claude/model-deprecations
* **Python 3.12** (KSS and KAS) end of life is **2028-10**
* **.NET 10 (LTS)** (KGS and its libraries) end of life is **2028-11-14**
    - https://dotnet.microsoft.com/en-us/platform/support/policy
