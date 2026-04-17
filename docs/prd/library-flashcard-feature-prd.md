# Library And Flashcard Save Feature PRD

## Goal

Build a Library flow for FlashCard so users can create collections, save flashcards from Scan Text and Translate, and browse saved flashcards later.

## User Value

- Let users organize saved words into named flashcard collections.
- Let users save useful terms directly from scan and translate flows without losing context.
- Let users review recent flashcards and collection contents inside the app.

## In Scope

- Library tab with empty state, search, create collection, rename collection, and delete collection.
- Save To Flashcard screen that lets users choose an existing collection or create a new one.
- Create Collection screen reused for both create and rename flows.
- Collection detail screen that lists saved flashcards.
- Save flow from `ScanCameraActivity`.
- Save flow from `TranslateActivity`.
- Shared local storage for collections and flashcards.
- English-only user-facing copy for auth, main, scan, translate, and library flows touched by this feature.

## Out Of Scope

- Cloud sync for collections or flashcards
- Study mode, swipe deck, or quiz mode
- Flashcard audio playback
- Card-level edit and delete flows beyond collection browsing

## UX Summary

- Library follows the same white background, bubble accents, rounded cards, and gradient primary buttons already used in the app.
- Empty library state gives a clear `Create Now` entry point.
- Save flow opens a dedicated full-screen picker to match the provided design direction.
- Users can create a new collection inline from either the Library tab or the Save flow.

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
  - created/updated timestamps

## Technical Approach

- Keep the project convention: `Activity + XML + ViewBinding`.
- Add a shared `FlashCardLibraryStore` backed by SharedPreferences JSON.
- Add `SaveToFlashcardActivity`, `CreateCollectionActivity`, and `LibraryCollectionActivity`.
- Update `LibraryFragment`, `CardFragment`, `ScanCameraActivity`, and `TranslateActivity` to use the new store and save flow.
- Render collection and flashcard lists with existing XML item bindings and dynamic inflation.

## Dependencies And Risks

- SharedPreferences JSON is simple and fast for current scope, but future sync or large libraries may need Room migration.
- Flashcards are currently saved locally only.
- Collection browsing is implemented, but advanced study/edit flows remain follow-up work.

## Acceptance Criteria

- User can create a collection from the Library tab.
- User can rename or delete a collection from Library.
- User can save a flashcard from Scan Text into an existing or newly created collection.
- User can save a flashcard from Translate into an existing or newly created collection.
- Library shows saved collections and supports search by collection name.
- Opening a collection shows its flashcards.
- Card tab shows recent flashcards across collections.
- User-facing strings in the touched flows are shown in English.
