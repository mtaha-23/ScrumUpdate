# Local Docker Configuration — Critical Settings

**Check these first when something breaks in local Docker.** These values
control how Angular talks to backends. The production stack (Jinja2 template
in `devops/Jenkins/demo-stack-deployment/khoji-stack.yml.j2`) does NOT set
most of these — it relies on Traefik hostname routing. Local Docker needs
explicit ports because there's no reverse proxy.

> Historical context for the broader modernization lives in
> [MODERNIZATION.md](MODERNIZATION.md).

## `.env` — Angular env vars

| Variable | Value | What it controls |
|----------|-------|------------------|
| `NG_SERVER_PORT` | `4243` | KBS Java backend port. Angular's `API_URL` is built from this. |
| `NG_APP_PORT` | `4241` | Express public-facing port. Used for `LOGIN_PAGE_URL` (OAuth redirect_uri). |

**These must be DIFFERENT.** `NG_SERVER_PORT` tells Angular where KBS is
(direct, no Express). `NG_APP_PORT` tells Angular what port the browser
reaches it on (for OAuth redirects). If you set both to `4241`, every API
call goes to Express instead of KBS, and nothing works.

## How `API_URL` is computed (`helper-functions.ts`)

```
API_URL = http://localhost:{NG_SERVER_PORT}
```

With `NG_SERVER_PORT=4243` → `API_URL = http://localhost:4243`

Angular sends ALL XHR/fetch calls (login, teams, worklogs, etc.) directly
to KBS on port 4243. Express does NOT proxy these — it's just the SPA server.

## How `LOGIN_PAGE_URL` is computed (`helper-functions.ts`)

```
LOGIN_PAGE_URL = http://localhost:{NG_APP_PORT}/login
```

With `NG_APP_PORT=4241` → `LOGIN_PAGE_URL = http://localhost:4241/login`

This is the OAuth redirect_uri sent to Atlassian. Must include the port
because Angular is exposed on 4241, not 80.

## OAuth redirect chain (Atlassian 3LO)

1. User clicks "Login with Jira" → browser goes to `https://auth.atlassian.com/...`
   with `redirect_uri=http://localhost:4241/login`
2. Atlassian redirects back to `http://localhost:4241/login?code=...`
3. Express serves the Angular SPA (catch-all route)
4. Angular reads the `code` param, POSTs to `API_URL + '/login'`
   → `http://localhost:4243/login` (KBS directly)
5. KBS `JwtAuthenticationFilter` exchanges the SSO code for tokens
6. Returns JWT → Angular stores it → redirects to dashboard

## KBS `application-dev.properties`

```
jira.client.redirect.uri=http://localhost:4241/login
```

Must match the OAuth redirect_uri. If wrong, Atlassian accepts it (it checks
against its own config), but KBS rejects the callback because the origins
don't match.

## KBS datasource — `currentSchema` is required

KBS tables live in the `khoji` Postgres schema (Liquibase default-schema).
The profile properties files set `?currentSchema=khoji` in the JDBC URL.
The compose override `SPRING_DATASOURCE_URL` MUST also carry it, or
Hibernate queries fail with `relation "khoji_user" does not exist`:

```
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-khoji}?currentSchema=khoji
```

## `docker-compose.yml` — Angular service

```yaml
ports:
  - "4241:8081"   # host:container
```

Express runs on 8081 inside the container, mapped to 4241 on the host.
The Angular Dockerfile sets `ENV PORT=8081` — don't change this.

## `environment.docker.ts` — runtime env chain

```
.env → docker-entrypoint.sh → envsubst → env.js → base64 → window["env"]
```

1. `.env` values are injected into the container by Docker Compose
2. `docker-entrypoint.sh` runs `envsubst` on `env.template.js`
3. The result is base64-encoded into `env.js`
4. Angular reads `window["env"]` at runtime

**If you change `.env`, you must rebuild the Angular container**
(`docker compose build angular`) because `env.js` is baked in at build time.

## Production (Jinja2) vs Local Docker

| Setting | Production (Jinja2) | Local Docker |
|---------|---------------------|--------------|
| `NG_SERVER_PORT` | not set (empty) | `4243` |
| `NG_APP_PORT` | not set (empty) | `4241` |
| `API_URL` | `https://kbs.{domain}` | `http://localhost:4243` |
| Angular → KBS | via Traefik hostname | direct port 4243 |
| OAuth redirect | `https://{domain}/login` | `http://localhost:4241/login` |

## Email / SMTP testing with Mailpit

The stack includes **Mailpit**, a local fake-SMTP sink (SMTP `:1025`, web UI
`http://localhost:8025`). It accepts any credentials and keeps every message so
email features (worklog reports, invites, revoke, reminders) can be tested
end-to-end: KBS → SMTP → Mailpit.

Set these in `.env` (never committed) to enable email against the sink:

```env
EMAIL_FROM=khoji@scrumupdate.local
EMAIL_HOST=mailpit          # compose service name, resolves on the network
EMAIL_PORT=1025
EMAIL_USERNAME=mailpit      # non-blank satisfies isConfigured(); Mailpit accepts any creds
EMAIL_PASSWORD=mailpit
EMAIL_STARTTLS=false        # Mailpit is plain SMTP — no TLS negotiation
```

`EMAIL_USERNAME`/`EMAIL_PASSWORD` must be non-blank for the app to send
(`isConfigured()`); Mailpit accepts whatever you put there. The app writes
email-status lines to its log file inside the container
(`logs/kbs-application.log`) — view with
`docker exec <kbs-container> tail -f logs/kbs-application.log`. The compose
console only carries the Camel route registrations (e.g. `Started route5
(direct://khojiEmailQueue)`), which appear when email is enabled.

**Trigger a test email** (APP basic-auth endpoint, dev profile only):

```
curl -u "$KHOJI_BASICAUTHUSERNAME:$KHOJI_BASICAUTHUSERPASSWORD" "http://localhost:4243/email/triggerRevokedUserEmail?userName=recipient@example.com&revokedUser=SomeUser"
```

Credentials come from the `.env` values `KHOJI_BASICAUTHUSERNAME` /
`KHOJI_BASICAUTHUSERPASSWORD` (see the basic-auth section in `example.env`).

KBS logs `email sent successfully`. Inspect the message at
`http://localhost:8025` (web UI) or via the Mailpit REST API
(`GET http://localhost:8025/api/v1/messages`).

**Negative path:** blank `EMAIL_USERNAME` in `.env`, restart KBS → the app logs
the "Email not configured" warning and no email is sent (Mailpit stays empty).

## Tenant token keystore

KBS encrypts `jwt_source_token` and `jwt_source_refresh_token` with an external
JCEKS file. The keystore is not in the repository. Any keystore or password
that was previously committed must be treated as compromised. Deployments must
generate new key material and new passwords. Do not reuse the old file.

Existing ciphertext in `jwt_source_token` and `jwt_source_refresh_token` cannot
be decrypted with the new key. Re-encrypt those stored values before discarding
the old key file. The old passwords and the old file stay out of git. Supply
them only for the migration run.

Set these in `.env`. Leave the passwords blank in git and supply them only
in the environment that starts KBS:

```env
KEY_STORE_PASSWORD=
KEY_PASSWORD=
KEY_STORE_MIGRATE=
OLD_KEY_STORE_PATH=
OLD_KEY_STORE_PASSWORD=
OLD_KEY_PASSWORD=
```

`KEY_STORE_PATH` is not secret. Outside Docker it defaults to
`/run/secrets/khoji-keystore.jck` and can be set to another external path.
Docker Compose always sets the container path to that location and mounts the
host secrets directory, not the keystore file. A missing file bind would make
Compose create a directory at that path.

```yaml
KEY_STORE_PATH: /run/secrets/khoji-keystore.jck
volumes:
  - ./secrets:/run/secrets:ro
```

If an earlier run created `secrets/khoji-keystore.jck` as a directory, delete
that directory before generating the file. Place the generated file at
`secrets/khoji-keystore.jck` on the host before `docker compose up`. That
directory is gitignored. KBS fails startup when either password is missing or
blank, or when the file, store password, key password, or required alias is
invalid. Startup errors name the setting and do not print secret values.

The file must be JCEKS and contain secret-key aliases `ssapitokenkey` and
`ssapitokensalt`. Generate new key bytes at deploy time. Do not copy the old
file. From `src/khoji-business-server`, with JDK 21 and the three `KEY_*`
variables set in the environment:

```shell
mvn -pl khoji-agile-models -DskipTests package
java -cp khoji-agile-models/target/khoji-agile-models-latest.jar uk.co.uworx.khoji.agile.internal.helper.KeystoreFiles
```

`KeystoreFiles` reads `KEY_STORE_PATH`, `KEY_STORE_PASSWORD`, and `KEY_PASSWORD`
from the environment and refuses to overwrite an existing file. It does not
print those passwords.

To rotate stored tokens, put the old file where the process can read it and
set `KEY_STORE_MIGRATE=true` plus `OLD_KEY_STORE_PATH`,
`OLD_KEY_STORE_PASSWORD`, and `OLD_KEY_PASSWORD` for one startup. Inside
Compose, `OLD_KEY_STORE_PATH` is a container path under `/run/secrets/`, and
the old file must sit in the host `secrets/` directory. KBS re-encrypts every
`jwt_source_token` and `jwt_source_refresh_token` column it finds, then logs
only the row count. It fails if those settings are missing, the old and new
key material match, a value cannot be decrypted, or the columns are absent.
After that run, unset `KEY_STORE_MIGRATE`, clear the old settings, and delete
the old file. Do not commit it.

## What NOT to change

- **`NG_SERVER_PORT`**: Must be `4243`. This is the KBS port. Express does
  NOT proxy KBS endpoints (except `/gateway/*` and `/api/*` for Jira routes).
- **`NG_APP_PORT`**: Must be `4241`. This is the public-facing port.
  Changing it breaks the OAuth redirect_uri.
- **`ENV PORT=8081`** in Angular Dockerfile: This is the Express server port
  inside the container. The host mapping `4241:8081` translates it.
