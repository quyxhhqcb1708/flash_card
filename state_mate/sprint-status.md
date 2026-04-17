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
  - Validation left for manual build/runtime check by Anh preference.
