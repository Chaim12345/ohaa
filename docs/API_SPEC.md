# OpenHands Cloud — API Specification (v1)

> Source of truth: [Cloud API Overview](https://docs.openhands.dev/openhands/usage/cloud/cloud-api)
> and the live Swagger UI at `https://app.all-hands.dev/docs`. Verified September 2026.
> This is the contract the **Canvas V2** Android client is built against.

## 1. Basics

| | |
|---|---|
| **Base URL** | `https://app.all-hands.dev` (configurable; user-supplied endpoints allowed) |
| **Auth** | `Authorization: Bearer <OPENHANDS_API_KEY>` on every request |
| **Content type** | `application/json` |
| **Errors** | Non-2xx status. `401` = invalid/missing key → prompt for Settings. Other codes are surfaced verbatim. |
| **Health** | `GET /api/service/health` (no version prefix) |

## 2. Endpoints

### 2.1 Health
```
GET /api/service/health
→ 200 { "status": "ok" }
```

### 2.2 Create conversation (start a task)
```
POST /api/v1/app-conversations
{
  "initial_message": { "content": [ { "type": "text", "text": "<task prompt>" } ] },
  "selected_repository": "owner/repo"          // optional
}
→ 200 {
  "id": "<start-task-uuid>",
  "status": "WORKING",                          // startup status, see §3
  "app_conversation_id": "<conversation-uuid>", // null until READY
  "sandbox_id": "sandbox-abc123",
  "created_at": "2025-01-15T10:30:00Z"
}
```
The response is a **start task**, not the conversation itself. Poll §2.3 until `READY`.

### 2.3 Start-task status
```
GET /api/v1/app-conversations/start-tasks?ids=<start-task-id>
→ 200 [ { "id": "...", "status": "READY", "app_conversation_id": "...", "sandbox_id": "..." } ]
```

### 2.4 List / fetch conversations
```
GET /api/v1/app-conversations?ids=<id>            // ids optional → all
GET /api/v1/app-conversations/search?q=<query>    // text search
→ 200 [
  {
    "id": "660e8400-...",
    "title": "Fix README",
    "sandbox_status": "RUNNING" | "STARTING" | "PAUSED" | "ERROR" | "MISSING",
    "execution_status": "idle" | "running" | "finished" | "paused" |
                        "waiting_for_confirmation" | "error" | "stuck",
    "selected_repository": "owner/repo",
    "created_at": "2025-01-15T10:30:00Z",
    "updated_at": "2025-01-15T11:00:00Z",
    "cost": 0.08
  }
]
```

### 2.5 Send message (follow-up prompt)
```
POST /api/v1/app-conversations/{conversation_id}/send-message
{ "content": [ { "type": "text", "text": "<message>" } ] }
→ 200 {}
```

### 2.6 Events (agent transcript)
```
GET /api/v1/conversation/{conversation_id}/events/search
→ 200 {
  "events": [
    {
      "id": "...",
      "kind": "MessageEvent" | "AgentThinkingEvent" | "ObservationEvent" | ...,
      "source": "user" | "agent" | "environment",
      "timestamp": "2025-01-15T10:31:00Z",
      "llm_message": { "role": "assistant", "content": "..." },
      "thought": "...",
      "action": "bash" | "edit" | "browse" | ...,
      "args": { ... },
      "observation": "..."
    }
  ],
  "next_page_id": "..."        // null on last page
}
```
The cloud event payload is intentionally heterogeneous; the client normalizes it
(`AgentEvent`) instead of trusting one field.

### 2.7 Delete conversation
```
DELETE /api/v1/app-conversations/{conversation_id} → 200
```

### 2.8 Credits (billing)
```
GET /api/billing/credits
→ 200 { "credits": 12.50 }
```

## 3. Status model

Two orthogonal axes returned by §2.4:

**sandbox_status** — the sandbox environment:

| Value | Meaning | Client treatment |
|---|---|---|
| `STARTING` | Sandbox being created | Pending (poll) |
| `RUNNING` | Sandbox active | Check execution_status |
| `PAUSED` | Rate limits / user action | Paused (auto-resumes) |
| `ERROR` | Sandbox error | Failed (terminal) |
| `MISSING` | Sandbox deleted | Failed (terminal) |

**execution_status** — the agent's task (when sandbox is `RUNNING`):

| Value | Meaning | Client treatment |
|---|---|---|
| `idle` | Ready for tasks | Idle |
| `running` | Actively working | Working (poll) |
| `paused` | Paused, auto-resumes | Paused |
| `waiting_for_confirmation` | Needs user action in UI | **Action required** — deep-link to web UI |
| `finished` | Task complete | Done (terminal) |
| `error` | Agent error | Failed (terminal) |
| `stuck` | Cannot proceed | Failed (terminal) |

Startup statuses from §2.2/§2.3 (`WORKING`, `WAITING_FOR_SANDBOX`, `PREPARING_REPOSITORY`,
`SETTING_UP_SKILLS`, `READY`, `ERROR`) are transitional and map into the same UI chips.

Terminal polling states: `finished`, `error`, `stuck`, `waiting_for_confirmation`.

## 4. Client polling policy (Canvas V2)

| Surface | Interval | Trigger |
|---|---|---|
| Conversation list | 15 s | While list tab visible |
| Detail: pending startup | 4 s | Until execution_status leaves startup set |
| Detail: active run | 10 s | While `running` / sandbox `STARTING` |
| Detail: terminal state | none | Stop; user can manual-refresh |

Polling is canceled when the screen is hidden (lifecycle-aware) or the user navigates back.

## 5. Resilience rules

1. **Never fabricate agent events.** If the API fails, the UI shows cached Room data plus an
   explicit offline banner. No simulated "agent responses".
2. **Offline-first.** Room is the single source of truth for UI; network refresh merges into it.
3. **Demo mode** (no API key configured) seeds clearly-labeled sample data so the app is
   explorable; banners say "Demo data" instead of pretending to be live.
4. `401` → Settings deep-link. `404` on events for a conversation that still starts → keep
   cached events, retry on next poll.
