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
- Scope: Build Library collections, save-to-flashcard flow, and shared local flashcard storage.
- Progress Notes:
  - PRD created for Library and flashcard save flow.
  - Added `FlashCardLibraryStore` with collection-based local storage.
  - Added `SaveToFlashcardActivity`, `CreateCollectionActivity`, and `LibraryCollectionActivity`.
  - Updated `LibraryFragment` and `CardFragment` to render collection data and recent flashcards.
  - Standardized touched auth/main/scan/translate/library strings to English.
  - Build validation completed with `assembleDebug`.
