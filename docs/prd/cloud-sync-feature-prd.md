# Cloud Sync Study Data Feature PRD

## Goal

Add user-scoped cloud backup and restore for study data so a learner can sign in on another device and continue with the same flashcards, SM-2 progress, and review history.

## User Value

- Keep saved topics, flashcards, and review progress attached to the Firebase account.
- Restore previous learning data automatically on a newly installed device after sign-in.
- Let the learner control cloud sync directly from `Setting > Data Storage`.

## In Scope

- Add Firestore-backed snapshot sync for:
  - flashcard collections
  - flashcard items
  - SM-2 review state
  - practice counters and accuracy signals
- Add a `Cloud Sync` switch in `Setting`.
- Add manual actions:
  - `Sync Now`
  - `Restore From Cloud`
- Auto-restore cloud data after login when local data is empty.
- Auto-upload local changes when cloud sync is enabled.

## Out Of Scope

- Realtime multi-device merge conflict resolution
- Per-card Firestore documents
- Offline sync queue visualization
- Syncing UI preferences such as language, sound, or reminders

## UX Summary

- `Setting` gets a dedicated `Cloud Sync` row with switch state and status summary.
- `Data Storage` dialog shows:
  - local topic count
  - local word count
  - cloud sync state
  - `Sync Now` and `Restore From Cloud` actions when available
- On a new device:
  - user signs in
  - if a cloud snapshot exists and local storage is empty, the app restores it before opening the main flow

## Technical Approach

- Use `Cloud Firestore` under:
  - `users/{uid}/studyData/librarySnapshot`
- Store one snapshot document containing:
  - `collectionsJson`
  - `collectionCount`
  - `cardCount`
  - `updatedAt`
  - `version`
- Keep current local storage structure as the source used by screens.
- Extend `FlashCardLibraryStore` with:
  - export snapshot
  - import snapshot
  - latest update metadata
  - user-scoped local persistence
- Trigger background upload after local library mutations when sync is enabled.

## Risks And Assumptions

- Snapshot-based sync is simple and stable for current app scope, but it depends on Firestore document size limits.
- The first signed-in account on a legacy install may claim the previous local snapshot during migration from the old shared key.
- Conflict handling is intentionally simplified to `newer snapshot wins` during bootstrap/enable flow.

## Acceptance Criteria

- User can enable or disable cloud sync from `Setting`.
- User can manually sync current study data to Firestore.
- User can manually restore study data from Firestore.
- When logging in on a new device with empty local data, the latest cloud snapshot restores automatically.
- Changes to library and review progress upload automatically while cloud sync is enabled.
- Build passes after integration.
