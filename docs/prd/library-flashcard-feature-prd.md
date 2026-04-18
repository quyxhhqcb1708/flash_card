# Library, Flashcard Save, And Topic Practice PRD

## Goal

Build a Library flow for FlashCard so users can create collections, save flashcards from Scan Text and Translate, browse cards by topic, and practice a topic directly from the library.

## User Value

- Let users organize saved words into named flashcard collections.
- Let users save useful terms directly from scan and translate flows without losing context.
- Let users tag a word as easy, medium, or hard at the moment it is saved.
- Let users review topic contents and practice one topic at a time inside the app.

## In Scope

- Library tab with empty state, search, create collection, rename collection, and delete collection.
- Save To Flashcard screen that lets users choose an existing collection or create a new one.
- Save-time difficulty tagging with `Easy`, `Medium`, and `Hard`.
- Create Collection screen reused for both create and rename flows.
- Collection detail screen that lists saved flashcards and surfaces a topic practice CTA.
- Shared local storage for collections and flashcards.
- Lightweight practice metadata and manual difficulty on each flashcard item.
- Save flow from `ScanCameraActivity`.
- Save flow from `TranslateActivity`.

## Out Of Scope

- Cloud sync for collections or flashcards
- Flashcard audio playback
- Card-level edit and delete flows beyond collection browsing
- Reminder scheduling

## UX Summary

- Library follows the same white background, bubble accents, rounded cards, and gradient primary buttons already used in the app.
- Empty library state gives a clear `Create Now` entry point.
- Save flow opens a dedicated full-screen picker and asks the user to choose word difficulty before the card is stored.
- Users can create a new collection inline from either the Library tab or the Save flow.
- Opening a topic shows both the saved cards and a focused `Practice this topic` action.

## Data And State Changes

- Replace the previous flat saved-card store with collection-based local storage.
- Each collection stores:
  - collection id
  - name
  - created/updated timestamps
  - a list of flashcard items
- Each flashcard stores:
  - term
  - definition
  - source language
  - target language
  - manual difficulty chosen at save time
  - created/updated timestamps
  - practice count
  - correct count
  - last practiced timestamp

## Technical Approach

- Keep the project convention: `Activity + XML + ViewBinding`.
- Add a shared `FlashCardLibraryStore` backed by SharedPreferences JSON.
- Add `SaveToFlashcardActivity`, `CreateCollectionActivity`, and `LibraryCollectionActivity`.
- Update `LibraryFragment`, practice flow entry points, `ScanCameraActivity`, and `TranslateActivity` to use the new store and save flow.

## Dependencies And Risks

- SharedPreferences JSON is simple and fast for current scope, but future sync or large libraries may need Room migration.
- Flashcards are currently saved locally only.
- Manual difficulty is only one signal and must later be combined with review results for smarter quiz weighting.

## Acceptance Criteria

- User can create a collection from the Library tab.
- User can rename or delete a collection from Library.
- User can save a flashcard from Scan Text into an existing or newly created collection.
- User can save a flashcard from Translate into an existing or newly created collection.
- User can choose a save-time difficulty before the flashcard is stored.
- Library shows saved collections and supports search by collection name.
- Opening a collection shows its flashcards and topic practice summary.
