# Practice SRS Feature PRD

## Goal

Replace the previous `Card` tab with a full `Practice` flow that reviews vocabulary from the Library using a weighted 15-question multiple-choice session backed by the SM-2 algorithm.

## User Value

- Show all saved vocabulary in one practice-focused dashboard.
- Prioritize the cards that are due today while still allowing the user to practice all cards on demand.
- Turn review into a faster quiz flow with 4 answer choices instead of a passive card-flip flow.
- Update the next review date automatically after each answer so study time stays focused on words that are close to being forgotten.
- Preserve the manual difficulty chosen at save time as an extra signal for future question weighting.

## In Scope

- Replace the `Card` tab with a `Practice` tab.
- Add a Practice dashboard that reads every flashcard from Library collections.
- Group cards by next review day (`Today`, `Tomorrow`, later dates).
- Add a review session activity used by both the global Practice tab and topic-level practice from Library.
- Each review session contains up to `15` questions.
- Each question shows:
  - 1 prompt word
  - 4 answer choices
  - 1 correct answer
- Distractors are built from other words inside the Library.
- Store SM-2 state on every flashcard:
  - easiness factor (`EF`)
  - repetitions
  - interval days
  - next review day
  - last review quality
- Store manual difficulty chosen at save time:
  - `Easy`
  - `Medium`
  - `Hard`

## Out Of Scope

- Audio pronunciation in review
- Cross-device SRS sync
- Reminder scheduling / notifications
- Fill-in-the-blank, typing, or speaking review modes

## UX Summary

- `Practice` tab shows a summary card, today's review CTA, practice-all CTA, and a schedule grouped by day.
- Save To Flashcard captures manual difficulty before the card enters the SRS loop.
- Review sessions use one prompt and 4 answer choices.
- After the user selects an option, the UI highlights correct/incorrect answers and shows `Continue`.
- Completing a session shows a summary screen with reviewed count, correct count, wrong count, accuracy, and the number of cards still due today.
- Topic practice inside Library reuses the same quiz engine with a collection filter.

## Question Weighting Logic

- Session length target: `15` questions.
- Question prompts are sampled with weighted selection.
- Weighting signals include:
  - whether the card is due today
  - manual difficulty chosen at save time
  - low easiness factor (`EF`)
  - previous low review quality
  - error rate (`wrong / total reviews`)
  - whether the card is still new
- Stable easy cards and already-mastered cards are down-weighted.
- When the source pool is small, difficult cards may repeat inside one 15-question session, but repeat frequency is softened by a repeat penalty.

## Data And State Changes

- Extend `FlashCardItem` with SM-2 state fields.
- Extend `FlashCardItem` with `manualDifficulty`.
- Keep existing local SharedPreferences JSON storage and migrate old cards lazily by using defaults:
  - `EF = 2.5`
  - `repetitions = 0`
  - `intervalDays = 0`
  - `nextReviewAt = 0`
  - `lastReviewQuality = -1`
  - `manualDifficulty = Medium`
- Continue storing simple counts (`practiceCount`, `correctCount`) for quick summaries.

## Technical Approach

- Keep the current project convention: `Activity + XML + ViewBinding`.
- Add `Sm2Scheduler.kt` as the single source of truth for SRS formulas and date logic.
- Add `PracticeOverviewBuilder.kt` to build dashboard sections from Library data.
- Add `PracticeSessionBuilder.kt` to generate weighted 15-question sessions and 4-choice options.
- Add `PracticeReviewScorer.kt` to convert correct/incorrect quiz results into SM-2 quality updates.
- Add `PracticeUiFormatter.kt` to keep display copy and due labels out of fragment/activity code.
- Add `PracticeFragment` for the new bottom-nav tab.
- Add `ReviewSessionActivity` for quiz execution.
- Reuse the same review activity from `LibraryCollectionActivity`.

## SM-2 Rules Applied

- Initial easiness factor: `2.5`
- Minimum easiness factor: `1.3`
- Intervals:
  - `I(1) = 1 day`
  - `I(2) = 6 days`
  - `I(n) = ceil(I(n-1) * EF)` for `n > 2`
- Easiness factor update:
  - `EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))`
- If `q < 3`:
  - reset repetitions to `0`
  - set next interval to `1 day`
- In the current quiz implementation:
  - wrong answer -> `q = 2`
  - correct answer -> `q = 3..5` depending on manual difficulty and current SRS indicators

## Dependencies And Risks

- SharedPreferences remains acceptable for the current scope, but large libraries may later need Room.
- Review schedule is day-based, not time-of-day based, to keep the UX simple and predictable.
- Existing old cards are treated as new SRS cards until the user reviews them for the first time.
- If Library has fewer than 4 saved words, multiple-choice practice is blocked.

## Acceptance Criteria

- Bottom navigation opens `Practice` instead of the old `Card` tab.
- Practice tab shows all saved flashcards grouped by next review day.
- User can start a 15-question quiz session for due cards or all cards.
- Each question shows 4 answer choices taken from Library data.
- Correct/incorrect quiz results update SM-2 state automatically.
- Manual difficulty persists from save time on each flashcard.
- Library topic practice uses the same quiz engine with a collection filter.
- Build passes after integration.
