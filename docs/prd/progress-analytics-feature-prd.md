# Progress Analytics Feature PRD

## Goal

Replace the bottom-nav `User` tab with a `Progress` dashboard that helps the learner understand study status at a glance through key metrics and a visual chart, while keeping the actual account/profile screen inside `Setting`.

## User Value

- Show the most important learning signals in one place:
  - total saved words
  - mastered words
  - words due today
  - overall answer accuracy
- Turn stored Library and SRS data into a readable progress report instead of raw flashcard lists.
- Help the learner decide what to do next through a simple focus recommendation.
- Remove the duplicated `User` screen from bottom navigation and keep account actions in `Setting`.

## In Scope

- Replace the old `User` bottom-nav tab with `Progress`.
- Build a `ProgressFragment` using the current `Fragment + XML + ViewBinding` convention.
- Show summary cards for:
  - total saved
  - mastered
  - due today
  - accuracy
  - topic count
  - practiced count
- Add a chart that visualizes current review distribution:
  - due
  - building
  - scheduled
  - stable
- Add a focus card with a next-best-action message derived from the current SRS state.
- Add a topic list sorted by review pressure so the learner can quickly jump into the most urgent collection.
- Move the old profile/logout UI into a dedicated `UserProfileActivity` opened from `Setting > User`.

## Out Of Scope

- Long-term historical charts by week/month
- Cloud analytics or multi-device sync
- Notification analytics
- Advanced trend prediction

## UX Summary

- Bottom nav item `Progress` opens a dashboard instead of account details.
- The top area summarizes the current learning state in compact metric cards.
- A bar chart gives a quick visual read of the review load.
- A focus recommendation tells the learner whether to review due words, reinforce learning words, or continue adding new vocabulary.
- A topic-health list shows which library collections need the most attention.
- Tapping a topic row opens `LibraryCollectionActivity`.
- The real account screen remains available from `Setting > User`.

## Data And Metrics

- Total saved words: all flashcards across every Library collection.
- Mastered words: cards that satisfy the current `Sm2Scheduler.isMastered()` rule.
- Due today: cards that satisfy `Sm2Scheduler.isDueToday(now)`.
- Accuracy: `sum(correctCount) / sum(practiceCount)` across all reviewed cards.
- Topic health per collection:
  - total cards
  - practiced cards
  - due today
  - learning
  - upcoming
  - mastered
  - mastery rate
  - accuracy rate

## Technical Approach

- Add `LearningProgressBuilder.kt` to derive dashboard metrics from `FlashCardLibraryStore` and `Sm2Scheduler`.
- Add `LearningProgressChartView.kt` as a lightweight custom XML view for the bar chart.
- Add `ProgressUiFormatter.kt` for percentage and focus-message formatting.
- Add `ProgressFragment` for the new tab content.
- Add `UserProfileActivity` to preserve account/profile/logout behavior outside bottom navigation.
- Update `MainActivity`, `MainPagerAdapter`, `MainNavigationHost`, `SettingFragment`, manifest, and main strings/icon resources.

## Risks And Assumptions

- The dashboard uses current stored SRS/card state only; no historical event log exists yet, so charts are snapshot-based rather than timeline-based.
- Mastered count and chart `Stable` count represent related but slightly different ideas:
  - `Mastered` is all mastered words overall
  - `Stable` is the currently non-due stable slice in the visual chart
- SharedPreferences storage remains acceptable for the current library size.

## Acceptance Criteria

- Bottom nav shows `Progress` instead of `User`.
- The new tab shows total saved, mastered, due today, and charted learning status.
- Topic summaries reflect Library collections and are sorted by urgency.
- `Setting > User` opens the account/profile screen, not the progress tab.
- Existing logout flow still works from the moved profile screen.
- Build passes after integration.
