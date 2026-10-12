# OpenAI-compatible endpoints

ScrumUpdate is designed around **Claude Haiku** (`claude-haiku-4-5-20251001`). Prompts and AI features are written for that model.

KGS can also call an OpenAI-compatible chat API. Use this when you do not have a `CLAUDE_API_KEY`. Other models often answer differently. Some results will be weaker, oddly formatted, or just wrong. This is a way to run the stack, not the same experience as Claude.

## Switch KGS off Claude

KGS reads `LLM_API`.

| `LLM_API` | What it uses |
|-----------|----------------|
| `claude` (default) | `CLAUDE_API_KEY` and `CLAUDE_MODEL` |
| `openai` | `OPENAI_API_KEY`, `OPENAI_API_URL`, and `OPENAI_MODEL` |

All three `OPENAI_*` values must be set. If one is missing, KGS logs a warning and AI stays off. `CLAUDE_API_KEY` is not used while `LLM_API=openai`.

Set the values in the repo-root `.env`, or in the shell (the shell wins). Then recreate KGS — no image rebuild:

```bash
docker compose up -d kgs
```

The examples below are the lines to put in `.env`. Commented copies are already there.

## Ollama

Run Ollama on your machine and pull a model first (`ollama pull llama3.2`). From inside the KGS container, your machine is `host.docker.internal`, not `localhost`.

```env
LLM_API=openai
OPENAI_API_KEY=ollama
OPENAI_API_URL=http://host.docker.internal:11434/v1
OPENAI_MODEL=llama3.2
```

The key can be any non-empty string. Ollama does not check it. Docker Desktop resolves `host.docker.internal`. On Linux you may need to add that hostname yourself.

`OPENAI_MODEL` must be a model Ollama has pulled.

## Together AI

Compose already defaults the URL and model to a Together example. You still have to set `LLM_API` and a key, or KGS stays on Claude.

```env
LLM_API=openai
OPENAI_API_KEY=your-together-key
OPENAI_API_URL=https://api.together.xyz/v1
OPENAI_MODEL=meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo
```

Use a chat model your Together account can call. The model name above is only an example.

## OpenAI

```env
LLM_API=openai
OPENAI_API_KEY=your-openai-key
OPENAI_API_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o-mini
```

Use a chat model your OpenAI account can call. `gpt-4o-mini` is only an example.

## Switch back to Claude

```env
LLM_API=claude
CLAUDE_API_KEY=your-claude-key
```

`CLAUDE_MODEL` defaults to `claude-haiku-4-5-20251001`. Recreate KGS the same way: `docker compose up -d kgs`.
