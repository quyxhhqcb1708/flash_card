# Authentication Feature PRD

## Goal

Build login and register screens for the Flash Card app using the provided visual style and integrate email/password authentication with Firebase Authentication.

## User Value

- Let users create a new account quickly.
- Let returning users sign in securely.
- Present a polished onboarding experience aligned with the provided design.

## In Scope

- Login screen
- Register screen
- Forgot password screen
- Email/password validation
- Firebase Authentication integration for sign in and sign up
- Firebase password reset email flow
- Google Sign-In with Firebase Authentication
- Loading, disabled, and error states
- Navigation from auth screens to the main screen

## Out Of Scope

- Profile completion
- Persistent session routing logic beyond a basic logged-in check

## UX Summary

- White background with subtle decorative bubbles.
- Rounded input fields with focused and error states.
- Primary action button uses linear gradient `#A357FF -> #3A78FF`.
- Secondary CTA lets users switch between login and register.
- Forgot password uses Firebase reset email flow.
- Social login button signs in with Google when Firebase OAuth is configured.

## Technical Approach

- Keep the current project convention: `Activity + XML + ViewBinding`.
- Add a small `FirebaseAuthManager` wrapper around `FirebaseAuth`.
- Use `LoginActivity` and `RegisterActivity` under the existing `ui.login` package.
- Navigate to `MainActivity` after successful authentication.

## Risks

- Build/runtime requires proper Firebase project setup and `google-services.json`.
- Google Sign-In also requires SHA-1/SHA-256 and a generated `default_web_client_id`.

## Acceptance Criteria

- User can register with email/password.
- User can sign in with email/password.
- User can request a reset password email.
- User can sign in with Google when OAuth config is present.
- Invalid input shows inline error state.
- Auth request shows loading state and prevents duplicate taps.
- Successful auth opens the main screen.
