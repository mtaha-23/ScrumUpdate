# 🚀 Debugging / Running Locally

- Ensure PostgreSQL is running
    - You can use local installation or docker container
    - Should be available at default 5432 port; map the port accordingly if using docker
    - Should have khoji-admin database
    - Should have user khoji-admin with password khoji having access to the database
    - Take a backup from production/staging/development database and restore

- Open Visual Studio solution from the root
- Play/Start/Debug the Multipl Startup Projects configuration
    - You should see Migrations getting applied
    - You should see kgs schema generated in the database
- Say Hi
    - Note down the guid, when debugging locally it usually is 00000000-0000-0000-0000-0000000000020
- Open the database and khoji_user table, specify this guid in msft_teams_aad_object_id column
- Say Hi again
    - You should get Help/Hi card in response now

# 🌐 Environment Variables

- ConnectionStrings__DefaultConnection; the full Npgsql connection string used to reach PostgreSQL. Its default fallback in code is `Host=localhost;Port=5432;Username=khoji-admin;Password=khoji;Database=khoji`; set it in launchSettings.json for local debugging and in docker-compose for the container.

- TEAMS_APP_ID; this is set in launchSettings.json for local debugging and is also needed in staging/production

- KHOJIX_BASE_URL; required — there is no in-code default. Set it to the browser-facing app URL (e.g. `http://localhost:4241` for local dev).
- KHOJIX_BUSINESS_SERVER_URL; its default value is http://kbs:4243

- MCP_ATLASSIAN_URL; its default value is http://mcp-atlassian:4001/mcp/

- KHOJI_BASICAUTHUSERNAME / KHOJI_BASICAUTHUSERPASSWORD; the inter-service basic-auth credential pair shared by KGS, KBS and KSS. KGS derives the ApiSecurity bearer key (`base64(username:password)`) from them and uses the same pair when calling KBS.

- KIA Compatible Supported Environment Variables (See LLM Integration below)

# 💀 Anatomy of “Teams Bot”

1. Teams Application, that’s zip file containing three files available in bin/DeployedAgents/ScrumUpdate 
    - Two icons and one manifest a JSON file
    - Manifest has two important “guids” the teams app guid (da7e) and bot guid (6798) (last four digits of guids)
    - We give this zip to upload to Teams Admin Dashboard (where someone having Teams Administrator access in M365 Tenant can provision it)
    - We will be submitting this same zip for publishing later
2. Microsoft Application Registration
    - In the M365/Azure tenant, we have created this AAD registration, the application id is same as bot guid (6798)
    - This application is configured for multi tenancy / required for Teams/Bot to work later when published
    - This application has an associated Secret (id/value pair) with an expiry date that we will have to renew accordingly
3. Azure Bot Registration
    - In the M365/Azure tenant, we have setup an Azure Bot registration using the Microsoft Application Registration (6798)
    - In this registration we have specified the KGS messaging endpoint, http://localhost:8080/public/api/messages (or the deployed KGS public URL)
    - In this registration we have declared that bot will use “Teams Channel”
    - We are using Free tier here
4. KGS
    - KGS is hosting the bot messaging endpoint; it uses Microsoft Authentication Library for the Bot Handshake and for this there are following important environment variables
        - TEAMS_APP_ID is set to da7e the same which we used for our Teams application (1)
        - Connections__BotServiceConnection__Settings__ClientId is set to 6798 the same which is being used for Bot ID / Microsoft Application Registration / Azure Bot Registration
        - Connections__BotServiceConnection__Settings__ClientSecret is set to the secret we created in Microsoft Application Registration security/token, when we renew it by creating a new token, we should come and update this as well
        - TokenValidation__Audiences__0 is set to 6798 the same which is being used for Bot ID / Azure Bot Registration declaring appliation will decide who gets authenticated (for multi tenancy)
        - Connections__BotServiceConnection__Settings__AuthorityEndpoint is set appropriately for issueing tokens (MSAL implementation) we have to include our tenant id when we have created the Microsoft Application Registration after July 31, 2025

https://learn.microsoft.com/en-us/azure/bot-service/provision-and-publish-a-bot 

# 🛜 APIs

KGS exposes two endpoint surfaces (defined in `Program.cs` / `KiaEndpoints.cs` / `WebChatEndpoints.cs`):

**External — `/public/*`, port 8080** (`RequireHost("*:8080", "*")`): the internet-facing surface used by Teams (Azure Bot Registration), WebChat and any external caller.

**Internal — `/*`, port 9090 only** (`RequireHost("*:9090")`): the inter-service surface for trusted callers inside the same network (KBS, KSS, analytics, MCP) or the host.

JWT bearer authentication (`AddBotAspNetAuthentication`) validates tokens when they are presented, but **no endpoint currently requires authentication** (no `[Authorize]` / fallback authorization policy). The `ApiSecurity` inter-service bearer key (`base64(KHOJI_BASICAUTHUSERNAME:KHOJI_BASICAUTHUSERPASSWORD)`) only lets a caller skip JWT validation — it does not gate access on its own. Protection of the internal surface therefore relies on network isolation of port 9090. In the local docker-compose `9090:9090` is published to the host; do not expose port 9090 externally in deployed environments.

### Test

- /public/test — external
- /test — internal

### Teams

- /public/api/messages — external (Azure Bot messaging endpoint)
- /api/messages — internal

### WebChat

- /public/api/chat — external; no auth, needed info is passed / verified from URLs

### Internal Consumptions

Internal only (port 9090), **not authenticated today**:

- /api/analytics/{yamlFilePath}
- /api/llm/invoke
- /api/kia/* (scrum-update, weekly-retro, categorize-issuetypes, generate-worklogs)

### OpenAI Endpoint

- /v1/chat/completions — mapped on **both** surfaces: internal `/v1/chat/completions` and external `/public/v1/chat/completions`

### Development

- /api/notification — internal; TODO: protect with ApiSecurity before shipping

# 🧠 LLM Integration

There are two ways you can configure LLM in KGS
- KIA style; where you setup one LLM (either Claude or OpenAI Compatible); in this mode, it supports KIA compatible environment variables listed below
- Using llms.yaml; where you can setup multiple LLMs naming them, Claude and OpenAI Compatible both and then can invoke them using their name

Once LLM is/are configured, you can make the API call to KGS to invoke them; example below showing:
- you can specify the **Cache-Control** header, the duration is in seconds; in reply KGS will also set the same header back acknowledging the action

```
POST http://localhost:8080/api/llm/invoke
Cache-Control: max-age=120
Content-Type: application/json


{
  "llm": "together",
  "prompt": "BurndownInterpreter",
  "input": "KFX-1 SP5 Monday, KFX-2 SP3 Tuesday"
}
```

Please note that
- You can continue to define the KIA environment variables and KGS will work in KIA style with default configured LLM and you can also define more LLMs in llms.yaml
- When you have configured LLMs in llms.yaml; you can still invoke without specifying the LLM name, and KGS will fall back to KIA style
- Caching only works when used with particular LLM (llms.yaml) and if LLM is not specified, KGS falls back to KIA style; and will not cache anything
- You can use the environment variables in llms.yaml as well

Please refer to _LlmIntegration.http file for examples

KGS LLM Integration is now controlled by (Unleash) Feature Flag and it can be overriden by using KGS-LLM=Enabled environment variable
KGS Agents Integration is now controlled by (Unleash) Feature Flag and it can be overriden by using KGS-AGENTS=Enabled environment variable

- For development / when using Local Docker
- Get the Claude Developer Key and define it as CLAUDE_API_KEY environment variable in your machine
- Uncomment KGS-LLM=Enabled environment variable in Local Docker Compose's KGS
- Uncomment KGS-AGENTS=Enabled environment variable in Local Docker Compose's KGS

Please dont check in this change as we dont want to burn Claude account unknowingly, the needed volume entries exist in Docker Compose; so we can try/work with Prompts when container is up, once we are happy with specific Prompt, simply check in

## KIA Compatible Supported Environment Variables

These are aligned with KIA

### CLAUDE_API_KEY
when using Claude, __this needs to be set__

### CLAUDE_MODEL
when using Claude, __this needs to be set__, set this to claude-haiku-4-5-20251001

### LLM_API
if set, it will be used, possible values, claude, gemini or openai, if missing, claude will be used

Local Docker: set this in the repo-root `.env`. See [docs/OPENAI.md](../../../docs/OPENAI.md) for OpenAI-compatible endpoints (Ollama, Together AI, OpenAI).

### OPENAI_API_KEY
__this needs to be set__, for Ollama set it to anything its required but not used for Ollama

### OPENAI_API_URL
it can be set to Ollama Url, http://localhost:11434/v1

### OPENAI_MODEL
this needs to be some valid Open AI model, when using Ollama it can be any model that Ollama can load, e-g llama3.2

### GEMINI_API_KEY
when using Gemini, __this needs to be set__

### GEMINI_MODEL
when using Gemini, __this needs to be set__ to some valid Gemini model
