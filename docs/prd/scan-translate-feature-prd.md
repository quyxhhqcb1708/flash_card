# Scan And Translate Feature PRD

## Goal

Build the Scan Text and Translate flow for the Flash Card app with realtime OCR scanning, phrase-level quick translate after capture, gallery image translation, and a dedicated translate screen aligned with the provided design.

## User Value

- Let users point the camera at text and see highlighted OCR regions in realtime.
- Let users capture a frame, tap a highlighted phrase, and translate it quickly.
- Let users send all detected text to a dedicated translate screen.
- Let users pick an image from the library and translate its text without using the camera.

## In Scope

- Scan Text launcher card opens a dedicated camera scan screen.
- CameraX preview with realtime OCR highlighting.
- Capture current frame from preview for post-scan review.
- Gallery picker on the scan screen.
- Phrase-level translation by tapping OCR-highlighted text after capture.
- Translate All action that renders translated text directly on top of the captured image.
- Translate screen with source text, target text, language swap, image picker, and auto translation.
- Save flashcard entry points from scan and translate into the shared Library flow.
- English to Vietnamese and Vietnamese to English translation using ML Kit.

## Out Of Scope

- Persisting scan history
- Offline download management UI for ML Kit models
- Multi-language selection beyond English and Vietnamese

## UX Summary

- Keep the existing white/gradient visual language used by auth and main screens.
- Scan Text screen is fullscreen with camera preview, top controls, language chips, and OCR overlay.
- In review mode, highlighted phrases are tappable and show a quick translation card.
- Translate All uses an image overlay approach to replace the detected text visually instead of navigating away.
- Translate screen follows the provided design with two large panels and a centered action button.

## Data And State Changes

- Add temporary state for current OCR payload, selected phrase, translated phrase, and scan review mode.
- Add language pair state shared by scan and translate screens through intent extras.
- Save actions hand off term/definition data into the shared flashcard library store.

## Technical Approach

- Keep the project convention: `Activity + XML + ViewBinding`.
- Add `ScanCameraActivity` and `TranslateActivity`.
- Use CameraX `PreviewView` and `ImageAnalysis` for realtime camera OCR.
- Use ML Kit Text Recognition for live frames and gallery/captured images.
- Use ML Kit Translate for `English <-> Vietnamese`.
- Add a custom `OcrOverlayView` to render tappable OCR boxes over preview or still images.

## Dependencies And Risks

- Camera flow requires camera permission at runtime.
- ML Kit translation may need a one-time model download on device before first translation.
- `PreviewView.bitmap` capture quality depends on the displayed preview frame.
- OCR box accuracy can vary with blur, low light, or dense documents.

## Acceptance Criteria

- User can open Scan Text and see realtime camera preview.
- OCR regions appear on top of live preview when text is visible.
- After capture or gallery pick, user can tap a highlighted phrase and see its translation.
- User can use Translate All to translate directly on the captured image.
- User can open Translate directly, type text, or pick an image to OCR and translate.
- User can swap between English and Vietnamese translation directions.
- User can save a translated phrase from Scan Text and Translate into a flashcard collection.
- New screens follow the current app's XML/ViewBinding conventions.
