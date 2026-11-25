# Test Traceability — the 13 spec test cases

This maps each of the 13 test cases from the interview workbook test table to the automated
tests that exercise it. Every case has at least one real, passing test.

For each case, **`AcceptanceTraceabilityTest`** (in
`src/test/java/com/cookify/acceptance/AcceptanceTraceabilityTest.java`) provides a single,
self-contained, thin end-to-end-style test as the canonical entry point — its method names and
`@DisplayName`s are numbered 1:1 against the case list below. Deeper, exhaustive coverage of the
same behavior (edge cases, failure paths, persistence detail) lives in the pre-existing unit/
integration test classes also listed per case.

Run just this suite with:

```bash
./gradlew test --offline --tests "com.cookify.acceptance.AcceptanceTraceabilityTest"
```

| # | Spec test case | `AcceptanceTraceabilityTest` method | Supporting tests |
|---|---|---|---|
| 1 | Account creation | `case1_accountCreation` | `RegistrationServiceTest#registersValidUserWithHashedPassword`, `#rejectsDuplicateUsername`, `#rejectsWeakPassword`, `#rejectsPasswordMismatch`; `SignupControllerTest#getRendersForm`, `#postValidRegistersAndRedirects`, `#postDuplicateUsernameShowsError` |
| 2 | Login + 2FA + session | `case2_loginTwoFactorAndSession` | `LoginFlowTest#loginByUsernameSucceeds`, `#loginByEmailSucceeds`, `#wrongPasswordFails`; `TwoFactorFlowTest#loginWith2faRedirectsToChallenge`, `#validOtpCompletesLogin`, `#wrongOtpShowsError`, `#rememberMeIsGatedBy2fa`, `#loginDoesNotPurgeOtherDeviceRememberMeTokens`, `#preTwoFactorSessionIsNotAuthenticated`; `RememberMeTest#setsRememberMeCookie` |
| 3 | Recipe storage (text/image/video + filters) | `case3_recipeStorageTextImageVideoAndAttributes` | `RecipeServiceTest#createReturnsVegRecipeAndPersistsCategory`, `#createReturnsNonVegRecipeWhenNotVeg`; `RecipeControllerTest#createsRecipeAndRedirectsToDetail`, `#updateByOwnerChangesFieldsAndRedirects`; `RecipeRepositoryTest#persistsSubclassWithDiscriminator`; `FileStorageServiceTest` (image/video upload hardening) |
| 4 | Search by filters | `case4_searchByFilters` | `RecipeSearchServiceTest#vegFilterReturnsOnlyVegRecipes`, `#nonVegFilterReturnsOnlyNonVegRecipes`, `#maxCostExcludesPricierRecipes`; `ExploreControllerTest#explorePageIsPublicAndListsSeededRecipe` |
| 5 | Veg/Non-Veg toggle | `case5_vegNonVegToggle` | `RecipeHierarchyTest#factoryPicksSubclassAndPolymorphicLabel`; `RecipeServiceTest#createReturnsVegRecipeAndPersistsCategory`, `#createReturnsNonVegRecipeWhenNotVeg` |
| 6 | Popularity sorting | `case6_popularitySorting` | `RecipeSearchServiceTest#popularSortOrdersHigherViewsAndRatingFirst` (explicit ordering assertion) |
| 7 | UX / aesthetics | `case7_uxAesthetics` | `HomeControllerTest#landingPageRenders`; `ExploreControllerTest#explorePageIsPublicAndListsSeededRecipe`; `LanguagePageTest` (shared branded layout + widget present site-wide) |
| 8 | Commenting + NSFW filter + email warning | `case8_commentingNsfwAndEmailWarning` | `CommentServiceTest#cleanCommentIsStoredAndListed`, `#nsfwCommentIsRejectedNotStoredAndWarns`; `CommentServiceModerationPersistenceTest#nsfwCommentPersistsWarningDespiteRejectionRollback`; `ContentFilterServiceTest` (all methods) |
| 9 | Rating averaged | `case9_ratingAveraged` | `RatingServiceTest#singleRatingProducesItsOwnAverage`, `#secondUserRatingAveragesAcrossBoth`, `#reRatingBySameUserReplacesRatherThanAdds`, `#starOutOfRangeThrows` |
| 10 | Code modularity | `case10_codeModularity` | `RecipeHierarchyTest#factoryPicksSubclassAndPolymorphicLabel` (OOP polymorphism); package-by-feature layout under `src/main/java/com/cookify/*` (one package per feature: `recipe`, `user`, `auth`, `rating`, `comment`, `moderation`, `subscription`, `search`, `chat`, `category`, `saved`, `storage`, `email`) |
| 11 | Ban after 3 warnings + content purge | `case11_banAfterThreeWarningsAndPurge` | `ModerationServiceTest#twoWarningsDoNotBan`, `#thirdWarningBansAndPurgesContent`; `CommentServiceModerationPersistenceTest#thirdNsfwCommentPersistsBanAndPurgeDespiteRejectionRollback` |
| 12 | Multilingual | `case12_multilingual` | `LanguagePageTest#landingPageIncludesGoogleTranslateWidgetAndLanguageOptionsAndCsp`, `#explorePageAlsoIncludesLanguageWidget` |
| 13 | Subscription + email | `case13_subscriptionAndEmail` | `SubscriptionServiceTest#subscribingEmailsTheCreator`, `#duplicateSubscribeIsNoOpAndSendsNoSecondEmail`, `#unsubscribeRemovesSubscription`, `#notifyNewRecipeEmailsAllSubscribersWithTitleAndLink` |

## Notes

- All test classes referenced above live under `src/test/java/com/cookify/`, mirroring the
  package of the production class they test (e.g. `RatingServiceTest` is in
  `com/cookify/rating/`).
- Cases 8 and 13 send real email via `JavaMailSender`; both `AcceptanceTraceabilityTest` and the
  supporting tests point it at an embedded GreenMail SMTP server for the test run (see
  `@TestPropertySource(properties = "spring.mail.port=3025")`), so they don't depend on MailHog
  being up. In a real `dev` run, those same emails are visible in MailHog's web UI — see the
  README's "2FA / email" section.
- This file is the deliverable required by Phase 5 Task 2; re-run it whenever a case's
  underlying behavior changes so the mapping stays accurate.
