# Settings And Localization Feature PRD

## Goal

Complete the `Setting` screen so it becomes a real configuration center for the app, and add full `English / Vietnamese` app localization with an in-app language switcher.

## User Value

- Let the learner switch the whole app between English and Vietnamese from `Setting`.
- Keep the app language consistent across auth, scan, translate, library, practice, progress, and settings.
- Replace placeholder settings with meaningful controls:
  - account access
  - app language
  - study reminders
  - sound feedback
  - study data storage status

## In Scope

- Add app-wide locale management for `en` and `vi`.
- Persist the selected language locally.
- Apply the selected language on app launch and after user changes it in settings.
- Add `values-vi` translations for the current user-facing string resources.
- Add a language picker inside `Setting`.
- Convert notification and sound options into real persisted toggles.
- Show actual local data storage status inside `Setting`.
- Replace user-facing auth error fallbacks with localized resource-driven messages.

## Out Of Scope

- Remote-config driven localization
- More than two app languages
- Notification scheduling backend
- Cloud sync implementation

## UX Summary

- `Setting` becomes a structured screen with sections:
  - `Account`
  - `Preferences`
  - `Study Data`
- `Account` opens the existing profile/account screen.
- `App Language` opens a single-choice dialog with:
  - `English`
  - `Tiếng Việt`
- `Study Reminders` and `Sound Feedback` are stored locally through switches.
- `Data Storage` shows the current number of topics and saved words stored on device, plus an info dialog.

## Technical Approach

- Add `AppLanguageManager` and `AppLanguageOption` as the single source of truth for supported app languages.
- Initialize the locale from `App.onCreate()`.
- Continue persisting the chosen language through the existing shared preference mechanism.
- Keep current project conventions: `Activity/Fragment + XML + ViewBinding`.
- Add Vietnamese string resources under `res/values-vi`.
- Add `AppSettingsStore` for persisted settings booleans.
- Rebuild `SettingFragment` UI to display current state instead of placeholder rows.
- Reuse localized strings in auth error handling via `AuthErrorResolver`.

## Risks And Assumptions

- Locale switching depends on AppCompat locale recreation behavior, which should refresh the current activity automatically.
- The app currently stores study data locally only, so data storage messaging must stay explicit about local-only persistence.
- Firebase backend error details are normalized into app-owned localized messages rather than raw backend text to keep language consistent.

## Acceptance Criteria

- App supports both English and Vietnamese.
- User can change app language from `Setting`.
- Language change applies across the main app flow and auth flow.
- Current setting rows are no longer placeholder-only.
- Notification and sound preferences persist locally.
- Study data storage summary displays real topic/card counts.
- Build passes after integration.
