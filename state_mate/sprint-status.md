# Sprint Status

## Sprint Authentication UI

- Status: Completed
- Scope: Build login/register UI and connect Firebase email/password authentication.
- Progress Notes:
  - PRD created.
  - Login/Register UI completed from provided design.
  - Firebase email/password flow connected in code.
  - Forgot password flow added with Firebase reset email.
  - Google Sign-In flow added with config guard for missing OAuth client id.

## Sprint Scan Translate

- Status: Completed
- Scope: Build Scan Text and Translate flow with camera OCR, gallery OCR, phrase translate, and translate-all handoff.
- Progress Notes:
  - PRD created for scan/translate flow.
  - Added `ScanCameraActivity` with CameraX preview, live OCR overlay, capture review mode, and gallery input.
  - Added `TranslateActivity` with language swap, image picker OCR, and ML Kit translation.
  - Connected the Scan Text home cards to the new scan and translate screens.
  - Updated `Translate All` to render translated text directly on the image.
  - Connected scan and translate save actions to the shared flashcard library flow.
  - Build validation completed with `assembleDebug`.

## Sprint Flashcard Library

- Status: Completed
- Scope: Build Library collections, save-to-flashcard flow, shared local flashcard storage, and topic-based practice inside Library.
- Progress Notes:
  - PRD created for Library and flashcard save flow.
  - Added `FlashCardLibraryStore` with collection-based local storage.
  - Added `SaveToFlashcardActivity`, `CreateCollectionActivity`, and `LibraryCollectionActivity`.
  - Upgraded `LibraryFragment` to show topic overview, search, and practice-ready states per collection.
  - Upgraded `LibraryCollectionActivity` with topic summary and `Practice this topic` CTA.
  - Added save-time difficulty tagging (`Easy / Medium / Hard`) in the flashcard save flow.
  - Standardized touched auth/main/scan/translate/library strings to English.
  - Build validation completed with `assembleDebug`.

## Sprint Practice SRS

- Status: Completed
- Scope: Replace the old Card tab with a Practice tab backed by SM-2 spaced repetition across all Library flashcards, using a 15-question multiple-choice review flow.
- Progress Notes:
  - PRD created for SRS practice flow and review session behavior.
  - Added `Sm2Scheduler` with EF, interval, reset, and due-day logic.
  - Added `PracticeFragment` dashboard grouped by review day.
  - Added `ReviewSessionActivity` with 15-question multiple-choice sessions and 4 answer options per question.
  - Added weighted question generation based on due state, save-time difficulty, EF, and previous mistakes.
  - Reused the same review session from `LibraryCollectionActivity` for topic review.
  - Extended `FlashCardLibraryStore` to persist SM-2 state on each flashcard.
  - Added manual difficulty persistence from save time as an extra review signal.
  - Build validation completed with `assembleDebug`.

## Sprint Progress Analytics

- Status: Completed
- Scope: Replace the bottom-nav User tab with a learning progress dashboard and move the account/profile UI into Setting.
- Progress Notes:
  - PRD created for progress analytics and dashboard behavior.
  - Replaced the bottom-nav `User` destination with a new `ProgressFragment`.
  - Added a snapshot progress dashboard with total saved words, mastered words, due-today count, accuracy, topic count, and practiced count.
  - Added a custom chart view to visualize due, building, scheduled, and stable vocabulary.
  - Added topic-health cards sorted by review pressure and linked them to `LibraryCollectionActivity`.
  - Moved the old profile/logout screen into `UserProfileActivity`, opened from `Setting > User`.
  - Build validation completed with `assembleDebug`.

## Sprint Settings Localization

- Status: Completed
- Scope: Complete the Setting screen and add full English/Vietnamese app localization with in-app language switching.
- Progress Notes:
  - PRD created for settings and localization behavior.
  - Added `AppLanguageManager` and persisted language selection through shared preferences.
  - Added `values-vi` resources for the current user-facing app strings.
  - Rebuilt `SettingFragment` with account access, app language selection, reminder toggle, sound toggle, and data storage summary.
  - Added localized auth error normalization through `AuthErrorResolver`.
  - Applied startup locale initialization from `App`.
  - Build validation completed with `assembleDebug`.

## Sprint Cloud Sync Storage

- Status: Completed
- Scope: Add Firestore-backed cloud backup and restore for study data, exposed from `Setting > Data Storage`.
- Progress Notes:
  - PRD created for cloud sync architecture and user flow.
  - Added `StudyCloudSyncManager` backed by Firebase Auth + Cloud Firestore.
  - Extended `FlashCardLibraryStore` with snapshot export/import and user-scoped local storage keys.
  - Added `Cloud Sync` switch, `Sync Now`, and `Restore From Cloud` actions in `Setting`.
  - Added login bootstrap restore so a new device can load cloud data before entering the main flow.
  - Build validation completed with `assembleDebug`.
