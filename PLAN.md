# ReDreamer — Dream Journal App: Global Plan

A private, offline-first Android dream journal. All data stays on the device; no accounts, no subscription, no analytics.

**Status: decisions locked 2026-07-23** — app lock ✔, no text formatting ✔ (plain text only), trash bin with 30-day auto-purge instead of undo ✔, tag management ✔, JSON backup ✔, nightmare/recurring flags ✔, sort by dream date with creation-date tiebreak ✔, optional Google Drive backup ✔, optional reality-check notifications ✔.

---

## 1. Core decisions

- **Sort order:** `dreamDate DESC, createdAt DESC` everywhere. Editing never reorders (`updatedAt` stored but never a sort key). `dreamDate` defaults to creation date, editable (for backfilled dreams).
- **Plain text only.** No rich text / Markdown. Ordinary characters suffice. Keeps search, copy, export and stats trivial.
- **Comment section** = single `notes` field (interpretation / afterthoughts), separate from dream text — enables clean "copy dream text only".
- **Lucidity** stored as nullable Int; null unless `isLucid`, so stats aren't polluted by zeros.
- **Moods:** fixed curated multi-select set (joyful, anxious, scared, peaceful, confused, sad, excited, neutral…). Custom moods maybe later.
- **Deletion = trash bin, not undo.** Soft delete via `deletedAt` timestamp. Bin screen with restore / delete-forever (confirmed). Items auto-purge 30 days after deletion. Purge runs on app launch (no background job needed). Binned dreams are excluded from list, search, filters, stats, and export.

## 2. Feature set

**Core:** dream CRUD; list with title, tags, moods, date, lucid badge, preview; global full-text search; filters (date range, tags, lucid/nightmare/recurring); long-press multi-select mass delete (to bin); copy dream / copy text only; share as text; statistics; tag management (rename/merge/delete/recolor); draft autosave.

**Privacy & data safety:** biometric/PIN app lock; JSON export/import via system file picker; automatic local backup; **optional Google Drive backup (off by default, toggle in settings)**.

**Optional notifications (all off by default):**
- Morning "Did you dream?" reminder at a chosen hour.
- **Reality-check reminders** at random-ish intervals during a configurable day window (e.g. 9:00–22:00, N times/day).

### Notification strategy (battery concern addressed)

Reality checks do **not** need exact timing — random drift is actually desirable for the technique. So:
- Use **WorkManager periodic work** (or inexact `AlarmManager`) — no wakelocks, no exact-alarm permission, negligible battery cost. Doze may delay a notification by minutes; irrelevant here.
- The battery-drain scenario only occurs with *exact* alarms (`SCHEDULE_EXACT_ALARM`) — we simply don't use them.
- Caveat: aggressive OEM battery managers (Xiaomi/Huawei/OnePlus…) can kill scheduled work regardless. WorkManager is the most resilient choice; if reminders get eaten, the fix is whitelisting the app in OEM settings (see dontkillmyapp.com) — link that from settings rather than fighting it in code.
- API 33+ requires the `POST_NOTIFICATIONS` runtime permission — request it only when the user enables a notification feature.

### Google Drive backup — approach

Google Drive's Android file picker does **not** support folder (tree) access, so "just save to a Drive folder via SAF" won't work for automatic backups. Two-tier plan:
1. **Android Auto Backup (free tier, on by default):** manifest-configured backup of the Room DB to the user's Drive app-data quota (25 MB limit — plenty for text). Zero code, restores on reinstall. Not user-visible but a real safety net.
2. **Explicit Drive backup (the settings toggle):** Drive REST API writing the JSON export to the app's hidden `appDataFolder` (scope `drive.appdata` — non-sensitive, no app verification hassle for personal use). Requires a Google Cloud project + OAuth client and sign-in via Credential Manager. Scheduled via WorkManager (constraint: network). This is the most complex feature in the app — scheduled last.

**Deliberately out of scope:** AI interpretation, dream dictionaries, cloud accounts/sync beyond backup, social features, sleep-stage tracking.

## 3. Tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin + Jetpack Compose, Material 3 |
| Architecture | MVVM, single module |
| Database | Room + FTS4 table for full-text search |
| DI | Hilt |
| Navigation | Navigation Compose |
| Settings | DataStore (Preferences) |
| Export/import | kotlinx.serialization → JSON via SAF |
| App lock | androidx.biometric |
| Background | WorkManager (notifications, Drive backup) |
| Drive backup | Drive REST API, `drive.appdata` scope, Credential Manager sign-in |
| Min SDK | 26 (Android 8.0) |

## 4. Data model

```
Dream
  id            Long (PK)
  title         String
  content       String            // main dream text (plain)
  notes         String            // commentary/interpretation
  dreamDate     LocalDate         // sort key; defaults to createdAt date, editable
  createdAt     Instant
  updatedAt     Instant
  deletedAt     Instant?          // null = live; set = in bin; purged 30 days later
  isLucid       Boolean
  lucidity      Int? (0–10, null unless lucid)
  clarity       Int  (0–10)
  isNightmare   Boolean
  isRecurring   Boolean
  moods         Set<Mood>         // enum, stored via converter

Tag
  id, name (unique, case-insensitive), color

DreamTagCrossRef
  dreamId, tagId

DreamFts (FTS4)                   // mirrors title + content + notes; live dreams only
```

Export format: single JSON file `{ version, exportedAt, tags[], dreams[] }`, tag names inlined per dream; import re-links/creates tags by name. Binned dreams excluded.

## 5. Screens

1. **Dream list (home)** — cards: title, dream date, tag chips, mood icons, lucid/nightmare badges, 1–2 line preview. Search bar, filter sheet, FAB. Long-press → selection mode → mass delete to bin.
2. **Dream editor** — title, dream date picker, content, mood chips, clarity slider, lucid checkbox → lucidity slider, nightmare/recurring checkboxes, tags (autocomplete + inline create), notes. Autosaves draft.
3. **Dream detail** — read mode: edit, copy full / copy text only, share, delete (to bin).
4. **Bin** — deleted dreams with days-remaining label; restore / delete forever (confirm); empty-bin action.
5. **Statistics** — totals, dreams/month, recall streak, top tags, mood distribution, clarity over time, lucid %, nightmare %, recurring %.
6. **Tag management** — usage counts; rename, merge, delete, recolor.
7. **Settings** — theme, app lock, export/import, local auto-backup, Drive backup toggle, notifications (morning reminder, reality checks), link to OEM battery-whitelist help.

## 6. Phased roadmap

**Phase 0 — Foundation:** project setup, theme, Room schema (incl. `deletedAt` from day one), repository, navigation skeleton, Auto Backup manifest config.

**Phase 1 — Core CRUD (usable MVP):** dream list (dreamDate desc, createdAt tiebreak), editor with all fields, detail view, soft delete to bin + purge-on-launch, draft autosave, empty state.

**Phase 2 — Find & act:** FTS global search; filters (date range, tags, lucid/nightmare/recurring); long-press multi-select mass delete; Bin screen; copy dream / copy text only; share as text.

**Phase 3 — Insight & hygiene:** statistics screen; tag management.

**Phase 4 — Data safety & privacy:** JSON export/import via SAF; automatic local backup; biometric/PIN app lock.

**Phase 5 — Notifications & Drive:** morning reminder; reality-check reminders (WorkManager, inexact); Google Drive backup toggle (OAuth + Drive API) — last, it's the most complex piece.

**Phase 6 — Polish (pick freely):** calendar view + recall streaks, quick-capture widget / QS tile, recurring dream-sign detection, "on this night a year ago", per-tag stats drill-down, encrypted export, linked recurring dreams, animations, tablet layout.
