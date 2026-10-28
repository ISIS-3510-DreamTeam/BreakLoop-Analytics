# BreakLoop Analytics

Spring Boot service that reads Firestore directly (via `FirebaseConfig`) and exposes aggregated analytics plus a static dashboard.

| Endpoint | Description |
|---|---|
| `GET /api/analytics/focus` | Focus Mode session totals |
| `GET /api/analytics/bq12?from=yyyy-MM-dd&to=yyyy-MM-dd&minUsers=5` | BQ12 (below) |
| `/dashboard` | Static dashboard |

## Dashboard

Run the app (needs Google application default credentials for Firestore) and open `http://localhost:8080/dashboard`.
It is plain HTML/CSS/JS in `src/main/resources/static/dashboard/index.html`, with no build step.

### Adding a section

In the `<script>` of `index.html`, add one call:

```js
addSection({
  id: 'bq6', bq: 'BQ6', owner: 'Pablo', title: 'Optional heading',
  question: 'The business question',
  endpoint: '/api/analytics/your-endpoint',          // omit to show "Pending"
  render: (data, body) => { body.append(/* build DOM from data */); }
});
```

For a section with its own controls (like BQ12), pass `setup: body => {...}` instead of `endpoint`/`render`.
Helpers available: `el(tag, attrs, ...children)`, `fetchJson(url)`, `barChart(rows)`, `pct(x)`.
To replace a "pending" placeholder, remove its entry from the pending list at the bottom and call `addSection` with an `endpoint`.

## BQ12 — place and time of offline-activity acceptance

**Question (Type 4):** At which times and places do users accept each category of offline activity the most?

**Place** is the user's saved label for where the visit happened: `HOME`, `UNIVERSITY`, `OTHER`, or `UNLABELED` when none was set. Time is `timeOfDay`: `MORNING`, `AFTERNOON`, `EVENING`, `NIGHT`.

**Formula.** Group = `category × timeOfDay × placeLabel`.
- Each element of `shownSuggestions` adds 1 to `shown` in its group.
- `accepted` adds 1 to the group of the shown item whose `activityId` equals `acceptedActivityId`, only when `outcome = ACCEPTED`. One interaction yields at most one acceptance.
- `acceptanceRate = accepted / shown`.

**Threshold (k-anonymity).** A group (and any category / timeOfDay / place total, outcome count or decline-reason breakdown) is hidden when fewer than `minUsers` distinct users (default 5) contributed. Hidden groups are counted in `suppressedGroups`.

**Open visits.** `PENDING` older than 2 hours is counted as `NO_ACTIVITY` with reason `UNKNOWN` and reported as `inferred`; more recent ones are ignored (`ignoredPending`).

**Outcomes and reasons per category.** `ACCEPTED` is counted under the accepted item's category; `OTHER_ACTIVITY` / `NO_ACTIVITY` and `declineReason` are counted once under each distinct category shown in that visit.

**Date filtering.** On `openedAt`, in memory (no composite index), with day boundaries in `America/Bogota`. Defaults: last 30 days.

**Fields read** from `collectionGroup("suggestionInteractions")` (via `.select`): `openedAt`, `timeOfDay`, `placeLabel`, `shownSuggestions`, `outcome`, `acceptedActivityId`, `declineReason`.
`latitude` and `longitude` are never read: place is answered by the label, so precise location adds privacy risk without adding information. The uid is taken from the document path only to count distinct users, and never appears in responses, logs or the dashboard.

## Tests

```
./mvnw test -Dtest='Bq12*Test'
```
These need no credentials. (`BreakloopAnalyticsApplicationTests` loads the full context and may need them.)
