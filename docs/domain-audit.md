# Domain phase audit

Checked against `docs/homa-domain-plan.md` and `docs/homa-product-description.md` on a clean build.
Updated after the follow-up decisions (G1 to G4 and #5).

## 1. Every Gherkin scenario has a matching test

All **127 of 127** scenarios in the plan have a test. That is the original 122, plus the five in the
"Buffer between jobs" feature added to the plan later. The script that checks this maps each scenario
title to a test class and method(s), confirms each method exists, and confirms the name is camelCase.
Every row of the 12 Scenario Outlines is a parameterised case, and most tests add the other side of each
boundary (10:15 and 10:16, 300 m and 301 m, RM200 and RM201).

Fixed during this audit:

- Six method names were noun phrases copied from the scenario title, not outcome sentences. They were
  renamed: `release`, `partialRefund`, `fullRefund`, `exactMatch`, `priceWithASeparateTravelFee` and
  `priceWithTheTravelFeeIncluded`.
- Three feature test classes didn't follow `<Aggregate or domain service><Behaviour>Test`. They are now
  `EnquiryLifecycleTest`, `SearchCriteriaValidationTest` and `MatchClassifierMatchingTest`.
- Two tests checked less than their scenario says:
  - Row 51 claims "free on every tier" but tested only FIRM; it now runs on all three tiers.
  - Row 42 never published a version 2; it now builds a reservation under a version 2 snapshot and
    shows the first reservation keeps version 1 and its RM30.00 service fee.

Two scenarios map to more than one test:

- Row 13 is split into the name check and the photo check.
- The outlines in rows 15 and 29 are split by result: accepted, out of range, invalid range.

| # | Feature | Scenario | Test class | Test method(s) |
|---|---|---|---|---|
| 1 | Job list | Job codes are unique | `JobListCurationTest` | `jobCodesAreUnique` |
| 2 | Job list | A retired job stays in the list but is no longer offered | `JobListCurationTest` | `aRetiredJobStaysInTheListButIsNoLongerOffered` |
| 3 | Service area | Only launch cities are covered (outline) | `ServiceAreaCoverageTest` | `onlyLaunchCitiesAreCovered` |
| 4 | Shelves | A shelf needs a preset search with at least one job | `ShelfCurationTest` | `aShelfNeedsAPresetSearchWithAtLeastOneJob` |
| 5 | Homa-set values | The first version holds the launch values | `HomaPolicyVersioningTest` | `theFirstVersionHoldsTheLaunchValues` |
| 6 | Homa-set values | Changing a value creates a new version and keeps the old one | `HomaPolicyVersioningTest` | `changingAValueCreatesANewVersionAndKeepsTheOldOne` |
| 7 | Homa-set values | A cancellation fee can never exceed the customer no-show fee | `HomaPolicyVersioningTest` | `aCancellationFeeCanNeverExceedTheCustomerNoShowFee` |
| 8 | Customer registration | A customer can only register with a confirmed phone number | `CustomerAccountRegistrationTest` | `aCustomerCanOnlyRegisterWithAConfirmedPhoneNumber` |
| 9 | Customer registration | A wrong code does not confirm the phone | `PhoneVerificationConfirmationTest` | `aWrongCodeDoesNotConfirmThePhone` |
| 10 | Customer registration | An expired code is rejected | `PhoneVerificationConfirmationTest` | `anExpiredCodeIsRejected` |
| 11 | Provider readiness | A provider is ready for go-live only when verified and payout-ready (outline) | `ProviderAccountReadinessTest` | `aProviderIsReadyForGoLiveOnlyWhenVerifiedAndPayoutReady` |
| 12 | Helpers | A new helper starts unverified | `HelperVerificationTest` | `aNewHelperStartsUnverified` |
| 13 | Helpers | A helper needs a name and a photo | `HelperVerificationTest` | `aHelperNeedsAName`, `aHelperNeedsAPhoto` |
| 14 | Offering setup | A provider can only offer jobs from Homa's job list | `ProviderListingSetupTest` | `aProviderCanOnlyOfferJobsFromHomasJobList` |
| 15 | Offering setup | Hours must sit within Homa's range (outline) | `ProviderListingSetupTest` | `hoursWithinHomasRangeAreAccepted`, `hoursMustSitWithinHomasRange`, `theMinimumHoursCannotBeAboveTheMaximum` |
| 16 | Offering setup | Max helpers cannot exceed the verified team size | `ProviderListingSetupTest` | `maxHelpersCannotExceedTheVerifiedTeamSize` |
| 17 | Offering setup | Max helpers cannot exceed 10 | `ProviderListingSetupTest` | `maxHelpersCannotExceedTen` |
| 18 | Offering setup | The helper range shrinks when the team shrinks | `ProviderListingSetupTest` | `theHelperRangeShrinksWhenTheTeamShrinks` |
| 19 | Offering setup | Cities must be launch cities | `ProviderListingSetupTest` | `citiesMustBeLaunchCities` |
| 20 | Going live | A complete, verified provider goes live | `ProviderListingGoLiveTest` | `aCompleteVerifiedProviderGoesLive` |
| 21 | Going live | Missing requirements block go-live and are listed | `ProviderListingGoLiveTest` | `missingRequirementsBlockGoLiveAndAreListed` |
| 22 | Going live | An unverified helper blocks go-live | `ProviderListingGoLiveTest` | `anUnverifiedHelperBlocksGoLive` |
| 23 | Provider availability | A slot inside a window with nothing else is available | `ProviderCalendarAvailabilityTest` | `aSlotInsideAWindowWithNothingElseIsAvailable` |
| 24 | Provider availability | A synced busy block makes the slot unavailable | `ProviderCalendarAvailabilityTest` | `aSyncedBusyBlockMakesTheSlotUnavailable` |
| 25 | Provider availability | An accepted reservation holds the slot | `ProviderCalendarAvailabilityTest` | `anAcceptedReservationHoldsTheSlot` |
| 26 | Provider availability | Releasing a cancelled reservation frees the slot | `ProviderCalendarAvailabilityTest` | `releasingACancelledReservationFreesTheSlot` |
| 27 | Provider availability | Holding an overlapping slot is rejected | `ProviderCalendarAvailabilityTest` | `holdingAnOverlappingSlotIsRejected` |
| 28 | Provider availability | A slot partly outside the window is not available | `ProviderCalendarAvailabilityTest` | `aSlotPartlyOutsideTheWindowIsNotAvailable` |
| 29 | Buffer between jobs | A job cannot start within the buffer after another job | `ProviderCalendarBufferTest` | `aJobCannotStartWithinTheBufferAfterAnotherJob` |
| 30 | Buffer between jobs | A job cannot end within the buffer before another job | `ProviderCalendarBufferTest` | `aJobCannotEndWithinTheBufferBeforeAnotherJob` |
| 31 | Buffer between jobs | Holding a slot inside the buffer is rejected | `ProviderCalendarBufferTest` | `holdingASlotInsideTheBufferIsRejected` |
| 32 | Buffer between jobs | The buffer is kept around jobs, not around synced busy blocks | `ProviderCalendarBufferTest` | `theBufferIsKeptAroundJobsNotAroundSyncedBusyBlocks` |
| 33 | Buffer between jobs | The buffer cannot be negative | `ProviderCalendarBufferTest` | `theBufferCannotBeNegative` |
| 34 | Search criteria | Search values must be within Homa's ranges (outline) | `SearchCriteriaValidationTest` | `searchValuesWithinHomasRangesAreAccepted`, `theDurationMustBeWithinHomasRange`, `theHelpersMustBeWithinHomasRange` |
| 35 | Search criteria | The start must respect the minimum notice | `SearchCriteriaValidationTest` | `theStartMustRespectTheMinimumNotice` |
| 36 | Matching | Exact match | `MatchClassifierMatchingTest` | `aProviderThatFitsEverythingIsAnExactMatch` |
| 37 | Matching | Some jobs only is a close match | `MatchClassifierMatchingTest` | `someJobsOnlyIsACloseMatch` |
| 38 | Matching | Free near the time is a close match | `MatchClassifierMatchingTest` | `freeNearTheTimeIsACloseMatch` |
| 39 | Matching | Not covering the city is no match | `MatchClassifierMatchingTest` | `notCoveringTheCityIsNoMatch` |
| 40 | Matching | Helpers outside the range is no match | `MatchClassifierMatchingTest` | `helpersOutsideTheRangeIsNoMatch` |
| 41 | Pricing | Price with a separate travel fee | `ReservationPricingTest` | `aSeparateTravelFeeIsAddedToTheHoursFeeBeforeTheServiceFee` |
| 42 | Pricing | Price with the travel fee included | `ReservationPricingTest` | `withTravelIncludedTheProviderPriceIsTheHoursFee` |
| 43 | Pricing | Choosing fewer jobs does not change the price | `ReservationPricingTest` | `choosingFewerJobsDoesNotChangeThePrice` |
| 44 | Requesting a reservation | Request details must fit Homa's and the provider's rules (outline) | `ReservationRequestTest` | `requestDetailsMustFitHomasAndTheProvidersRules` |
| 45 | Requesting a reservation | Jobs must be offered by the provider | `ReservationRequestTest` | `jobsMustBeOfferedByTheProvider` |
| 46 | Requesting a reservation | The customer's city must be covered | `ReservationRequestTest` | `theCustomersCityMustBeCovered` |
| 47 | Requesting a reservation | The reservation keeps the policy in force when requested | `ReservationRequestTest` | `theReservationKeepsThePolicyInForceWhenRequested` |
| 48 | Request expiry | A request expires after 24 hours or at the booked start, whichever is sooner (outline) | `ReservationRequestExpiryTest` | `aRequestExpiresAfter24HoursOrAtTheBookedStartWhicheverIsSooner` |
| 49 | Request expiry | The provider cannot accept an expired request | `ReservationRequestExpiryTest` | `theProviderCannotAcceptAnExpiredRequest` |
| 50 | Request expiry | An unanswered request expires | `ReservationRequestExpiryTest` | `anUnansweredRequestExpires` |
| 51 | Accepting and declining | The provider accepts a request | `ReservationAcceptanceTest` | `theProviderAcceptsARequest` |
| 52 | Accepting and declining | The provider declines a request | `ReservationAcceptanceTest` | `theProviderDeclinesARequest` |
| 53 | Accepting and declining | Only a requested reservation can be accepted | `ReservationAcceptanceTest` | `onlyARequestedReservationCanBeAccepted` |
| 54 | Editing a request | The customer edits a request before it is accepted | `ReservationEditingTest` | `theCustomerEditsARequestBeforeItIsAccepted` |
| 55 | Editing a request | A reserved booking cannot be edited | `ReservationEditingTest` | `aReservedBookingCannotBeEdited` |
| 56 | Customer cancellation | Cancelling a request is free on every tier | `ReservationCustomerCancellationTest` | `customerCancelsForFreeOnEveryTier` |
| 57 | Customer cancellation | Cancelling a reserved booking before the start (outline) | `ReservationCustomerCancellationTest` | `cancellingAReservedBookingBeforeTheStartFollowsTheTier` |
| 58 | Customer cancellation | With travel included, the tier applies to the full provider price | `ReservationCustomerCancellationTest` | `withTravelIncludedTheTierAppliesToTheFullProviderPrice` |
| 59 | Customer cancellation | The customer cannot cancel during the check-in period before check-in | `ReservationCustomerCancellationTest` | `customerCannotCancelDuringCheckInPeriodBeforeCheckIn` |
| 60 | Customer cancellation | Cancelling after check-in counts as a customer no-show | `ReservationCustomerCancellationTest` | `cancellingAfterCheckInCountsAsACustomerNoShow` |
| 61 | Customer cancellation | A cancellation fee is released straight away | `ReservationCustomerCancellationTest` | `aCancellationFeeIsReleasedStraightAway` |
| 62 | Provider cancellation and fines | Provider fines depend on timing only (outline) | `ReservationProviderCancellationTest` | `providerFinesDependOnTimingOnly` |
| 63 | Provider cancellation and fines | Cancelling after the booked start but before check-in carries the no-show fine | `ReservationProviderCancellationTest` | `cancellingAfterTheBookedStartButBeforeCheckInCarriesTheNoShowFine` |
| 64 | Provider cancellation and fines | Cancelling after check-in carries the no-show fine | `ReservationProviderCancellationTest` | `cancellingAfterCheckInCarriesTheNoShowFine` |
| 65 | Provider cancellation and fines | The fine does not depend on the provider's tier | `ReservationProviderCancellationTest` | `theFineDoesNotDependOnTheProvidersTier` |
| 66 | Provider cancellation and fines | With travel included, the fine applies to the full provider price | `ReservationProviderCancellationTest` | `withTravelIncludedTheFineAppliesToTheFullProviderPrice` |
| 67 | Naming who is coming | The provider names the lead and helpers | `ReservationTeamNamingTest` | `theProviderNamesTheLeadAndHelpers` |
| 68 | Naming who is coming | The team must match the number of helpers booked | `ReservationTeamNamingTest` | `theTeamMustMatchTheNumberOfHelpersBooked` |
| 69 | Naming who is coming | Helpers must be on the verified roster | `ReservationTeamNamingTest` | `helpersMustBeOnTheVerifiedRoster` |
| 70 | Naming who is coming | The lead must be one of the named helpers | `ReservationTeamNamingTest` | `theLeadMustBeOneOfTheNamedHelpers` |
| 71 | Naming who is coming | The team cannot change from the booked start | `ReservationTeamNamingTest` | `theTeamCannotChangeFromTheBookedStart` |
| 72 | Check-in | Check-in needs the device near the customer within the check-in period (outline) | `ReservationCheckInTest` | `checkInNeedsTheDeviceNearTheCustomerWithinTheCheckInPeriod` |
| 73 | Check-in | Check-in is locked until the team is named | `ReservationCheckInTest` | `checkInIsLockedUntilTheTeamIsNamed` |
| 74 | Check-in | No check-in by the end of the check-in period (10:15) is a provider no-show | `ReservationCheckInTest` | `noCheckInByTheEndOfTheCheckInPeriodIsAProviderNoShow` |
| 75 | Check-in | A provider no-show cannot be marked early | `ReservationCheckInTest` | `providerNoShowIsMarkedOnlyAfterCheckInPeriodEnds` |
| 76 | Customer no-show | The provider reports a customer no-show after the wait | `ReservationCustomerNoShowTest` | `theProviderReportsACustomerNoShowAfterTheWait` |
| 77 | Customer no-show | A customer no-show cannot be reported before the wait ends | `ReservationCustomerNoShowTest` | `aCustomerNoShowCannotBeReportedBeforeTheWaitEnds` |
| 78 | Customer no-show | A customer no-show needs a check-in | `ReservationCustomerNoShowTest` | `aCustomerNoShowNeedsACheckIn` |
| 79 | Customer no-show | The customer challenges a no-show within the report window | `ReservationCustomerNoShowTest` | `theCustomerChallengesANoShowWithinTheReportWindow` |
| 80 | Customer no-show | Homa upholds the no-show and the reservation ends cancelled | `ReservationDecisionTest` | `homaUpholdsTheNoShowAndTheReservationEndsCancelled` |
| 81 | Customer no-show | Homa finds the no-show wrongful and refunds in full | `ReservationDecisionTest` | `homaFindsTheNoShowWrongfulAndRefundsInFull` |
| 82 | Customer no-show | A challenge after the report window is rejected | `ReservationCustomerNoShowTest` | `aChallengeAfterTheReportWindowIsRejected` |
| 83 | Customer no-show | The no-show fee never exceeds the ceiling | `SettlementInvariantTest` | `theNoShowFeeIsTheCeiling` |
| 84 | Completion | Completion needs photo or video evidence | `ReservationCompletionTest` | `completionNeedsPhotoOrVideoEvidence` |
| 85 | Completion | Completion needs a check-in first | `ReservationCompletionTest` | `completionNeedsACheckInFirst` |
| 86 | Completion | The completion reminder is due 30 minutes after the booked end (outline) | `ReservationCompletionTest` | `theCompletionReminderIsDue30MinutesAfterTheBookedEnd` |
| 87 | Completion | Missing the completion deadline sends it to Homa | `ReservationCompletionTest` | `missingTheCompletionDeadlineSendsItToHoma` |
| 88 | Completion | The completion deadline cannot be missed early | `ReservationCompletionTest` | `theCompletionDeadlineCannotBeMissedEarly` |
| 89 | Dispute window and payout | The window ends 1 hour after the later of the booked end and completion (outline) | `ReservationDisputeWindowTest` | `theWindowEndsAnHourAfterTheLaterOfTheBookedEndAndCompletion` |
| 90 | Dispute window and payout | The window has no end until completion is submitted | `ReservationDisputeWindowTest` | `theWindowHasNoEndUntilCompletionIsSubmitted` |
| 91 | Dispute window and payout | The window closes with no report and the payment is released | `ReservationDisputeWindowTest` | `theWindowClosesWithNoReportAndThePaymentIsReleased` |
| 92 | Dispute window and payout | The window cannot close early | `ReservationDisputeWindowTest` | `theWindowCannotCloseEarly` |
| 93 | Reports | The customer files a report inside the window | `ReservationReportTest` | `theCustomerFilesAReportInsideTheWindow` |
| 94 | Reports | The provider can file a report too | `ReservationReportTest` | `theProviderCanFileAReportToo` |
| 95 | Reports | Each party files at most one report | `ReservationReportTest` | `eachPartyFilesAtMostOneReport` |
| 96 | Reports | Reports before the booked start are rejected | `ReservationReportTest` | `reportsBeforeTheBookedStartAreRejected` |
| 97 | Reports | Reports after the window are rejected | `ReservationReportTest` | `reportsAfterTheWindowAreRejected` |
| 98 | Homa's decision | Release | `ReservationDecisionTest` | `homaReleasesThePaymentAndTheJobIsCompleted` |
| 99 | Homa's decision | Partial refund | `ReservationDecisionTest` | `aPartialRefundSplitsThePaymentAndTheServiceFeeFollowsTheProvider` |
| 100 | Homa's decision | Full refund | `ReservationDecisionTest` | `aFullRefundReturnsEverythingToTheCustomer` |
| 101 | Homa's decision | A partial refund cannot give the provider more than the provider price | `ReservationDecisionTest` | `aPartialRefundCannotGiveTheProviderMoreThanTheProviderPrice` |
| 102 | Enquiries | An enquiry closes when its date passes without a request | `EnquiryLifecycleTest` | `anEnquiryClosesWhenItsDatePassesWithoutARequest` |
| 103 | Enquiries | An enquiry turns into one reservation only | `EnquiryLifecycleTest` | `anEnquiryTurnsIntoOneReservationOnly` |
| 104 | Enquiries | Book again copies the details with a new date | `EnquiryLifecycleTest` | `bookAgainCopiesTheDetailsWithANewDate` |
| 105 | Taking payment | A card is authorised, not charged, at request | `PaymentTakingTest` | `aCardIsAuthorisedNotChargedAtRequest` |
| 106 | Taking payment | FPX is paid upfront | `PaymentTakingTest` | `fpxIsPaidUpfront` |
| 107 | Taking payment | A card is charged when the provider accepts | `PaymentTakingTest` | `aCardIsChargedWhenTheProviderAccepts` |
| 108 | Taking payment | A declined card request releases the authorisation and refunds nothing | `PaymentTakingTest` | `aDeclinedCardRequestReleasesTheAuthorisationAndRefundsNothing` |
| 109 | Taking payment | A declined FPX request is refunded in full | `PaymentTakingTest` | `aDeclinedFpxRequestIsRefundedInFull` |
| 110 | Taking payment | Editing an FPX request upward collects the difference | `PaymentTakingTest` | `editingAnFpxRequestUpwardCollectsTheDifference` |
| 111 | Settling | A settlement is fully accounted for | `PaymentSettlementTest` | `aSettlementIsFullyAccountedFor` |
| 112 | Settling | A settlement that doesn't add up is rejected | `PaymentSettlementTest` | `aSettlementThatDoesNotAddUpIsRejected` |
| 113 | Settling | A payment cannot be settled twice | `PaymentSettlementTest` | `aPaymentCannotBeSettledTwice` |
| 114 | Fines and payouts | A fine is deducted at most 50% per payout | `FineDeductionPolicyTest` | `aFineIsDeductedAtMostHalfOfAPayout` |
| 115 | Fines and payouts | The rest of the fine comes off the next payout | `FineDeductionPolicyTest` | `theRestOfTheFineComesOffTheNextPayout` |
| 116 | Fines and payouts | Older fines are deducted first | `FineDeductionPolicyTest` | `olderFinesAreDeductedFirst` |
| 117 | Fines and payouts | A waived fine is not deducted | `FineDeductionPolicyTest` | `aWaivedFineIsNotDeducted` |
| 118 | Fines and payouts | A partly deducted fine can be waived | `FineAndPayoutTest` | `aPartlyDeductedFineCanBeWaived` |
| 119 | Deciding a case | A decision needs a reason | `CaseDecisionTest` | `aDecisionNeedsAReason` |
| 120 | Deciding a case | A case is decided once | `CaseDecisionTest` | `aCaseIsDecidedOnce` |
| 121 | Deciding a case | A partial refund must be within the provider price | `CaseDecisionTest` | `aPartialRefundMustBeWithinTheProviderPrice` |
| 122 | Deciding a case | Every source can be decided with any outcome (outline) | `CaseDecisionTest` | `everySourceCanBeDecidedWithAnyOutcome` |
| 123 | Chat | Only the customer can start a chat | `ConversationChatTest` | `onlyTheCustomerCanStartAChat` |
| 124 | Chat | The provider can reply once the customer has started | `ConversationChatTest` | `theProviderCanReplyOnceTheCustomerHasStarted` |
| 125 | Chat | Requesting from an enquiry keeps the same chat | `ConversationChatTest` | `requestingFromAnEnquiryKeepsTheSameChat` |
| 126 | Chat | An archived chat is readable but closed | `ConversationChatTest` | `anArchivedChatIsReadableButClosed` |
| 127 | Chat | A reservation's chat stays open after the job | `ConversationChatTest` | `aReservationsChatStaysOpenAfterTheJob` |

## 2. Product spec rules with no test

### 2a. Domain rules not tested yet

These belong in the domain and have no test today.

| # | Spec rule | What is missing | Suggested fix |
|---|---|---|---|
| G1 (**done**) | Cancellation Policy: the review page and the reservation details show "the exact amount and time for that booking", for example "Cancel after Sat 9:00 am: you pay RM36" | No domain query gives the fee and the moment it starts. The rule is applied inside `cancelByCustomer`, but nothing exposes it | Added `Reservation.cancellationTerms()`, tested in `ReservationCancellationTermsTest` at the fee-start boundary on every tier. `cancelByCustomer` uses the same terms |
| G2 (**no rule**) | Check-in Policy: "The lead must be at the customer's location with the provider app to check in"; completion is submitted "through the lead on site" | `checkIn` and `submitCompletion` don't record or check who did them | Decided: not recorded or checked. Helpers have no accounts and the provider is accountable either way |
| G3 (**usecase**) | Chat: on reserving, "Homa checks the provider's availability again"; How Homa Works: "Bookable times come from each provider's own calendar" | `Reservation.request` takes no availability answer, and no test ties a request to `ProviderCalendar.isAvailable` or `hold` | Decided: the request and accept usecases check availability, with tests there |
| G4 (**optional**) | Dispute Policy: each report is filed "with a reason and evidence" | Evidence is optional; only the reason is required. The plan's own scenario has the provider file with a reason only | Decided: evidence is optional and a reason is required. The spec now says "with a reason, and evidence where available" |

### 2b. Rules outside the domain phase

These are left out by the plan's "Not in the domain phase" section, or are UI and read-model concerns.
Each will need a test in the usecase or infra phase.

- **Notifications:** new requests, team named or changed, check-in, completion submitted, customer
  no-show reported, case decided, fine charged, verification rejected. The events exist; the senders
  don't yet.
- **Read models:**
  - provider page stats (jobs done, cancellation count, price and minimum hours)
  - tools a provider does not bring
  - the Verified ID badge
  - the inbox grouped by provider
  - the customer's payments and refunds
  - admin tracking: overview, reservations history, payments, providers, customers, fines
- **Payments infra:** refunds going "back to the original payment method", and refund status and
  expected timing shown to the customer. `PaymentInstruction` doesn't carry the method; `Payment`
  knows it.
- **Evidence retention** for 7 days after the reservation is resolved: a scheduled infra job. The value
  is in `policy` and tested there.
- **Location** used only at the moment of check-in, and the distance itself, which the geoservice works
  out. The domain only checks the distance against the radius.
- **Privacy:** consent, user data requests, deletion after account closure, breach process.
- **UI wording:** the tier explained in words on the provider page, and the Homa Protection reminder in
  every chat.
- **Schedulers that call the domain at the right time:** request expiry, provider no-show, the end of
  the no-show report window, the completion reminder and deadline, closing the dispute window, the
  team reminder, and enquiry closing.

### 2c. Covered without a dedicated test

- "A provider who arrives within the check-in period but late is tolerated unless the customer reports
  it": a check-in at 10:05 and at 10:15 is accepted, and a customer report is accepted from the booked
  start. No single test is named for it.
- "During the check-in period the customer cannot cancel until the provider checks in or the period
  ends": after the period ends with no check-in, a customer cancellation is rejected and the reservation
  ends as a provider no-show with a full refund. The customer gets the same outcome by a different
  route.

## 3. Open-question assumptions the code relies on

| # | Assumption | Status | Where in the code | Tests |
|---|---|---|---|---|
| 1 | The customer no-show wait starts from the later of the booked start and the check-in | Confirmed | `reservation/domain/Reservation.java` `noShowWaitEndsAt()` (line 433), used by `reportCustomerNoShow` | `ReservationCustomerNoShowTest.WhereTheWaitStarts` |
| 2 | Percentages round half-up to the sen, and the customer refund is worked out last so the parts add up | Confirmed | `kernel/Percentage.java` `of(Money)` (HALF_UP, line 37); `reservation/domain/Settlement.java` `of` (line 34); `reservation/domain/PriceBreakdown.java` (service fee). The fine cap uses a variant that rounds in the provider's favour: `payment/domain/FineDeductionPolicy.java` `mostDeductible` (line 36) | `PercentageTest`, `SettlementInvariantTest`, `FineDeductionPolicyTest.theCapRoundsInTheProvidersFavour` |
| 3 | Editing a request does not reset its expiry | Confirmed | `reservation/domain/Reservation.java` `requestExpiresAt()` (lines 116–117, from `requestedAt`); `edit` leaves `requestedAt` alone | `ReservationEditingTest.editingDoesNotResetTheExpiry`, `movingTheStartEarlierCanBringTheExpiryForward` |
| 4 | "Near the chosen time" for a close match means free for the same duration within 2 hours either side | Confirmed | `discovery/domain/MatchClassifier.java` `NEAR_THE_CHOSEN_TIME` (line 24), passed by the usecase to `availability/domain/ProviderCalendar.java` `nearestAvailableStart` (line 94) | `MatchClassifierMatchingTest`, `ProviderCalendarNearestStartTest` |
| 5 | SMS codes are 6 digits, valid for 5 minutes, with a new code needed after 5 wrong attempts | Confirmed | `account/domain/PhoneVerification.java` `CODE_VALIDITY`, `MAX_WRONG_ATTEMPTS` and `SIX_DIGITS` (lines 19–22) | `PhoneVerificationConfirmationTest` |
| 6 | The lead is one of the named helpers; a provider who is their own helper can be the lead | Confirmed | `reservation/domain/TeamAssignment.java` constructor (line 16) | `ReservationTeamNamingTest.theLeadMustBeOneOfTheNamedHelpers`, `aProviderWhoIsTheirOwnHelperCanLeadTheTeam` |
| 7 | Fines come off payouts for completed jobs only, not cancellation-fee or no-show-fee payouts | Confirmed | `payment/domain/PayoutKind.java`; `payment/domain/FineDeductionPolicy.java` `apply` (line 24) | `FineDeductionPolicyTest.onlyPayoutsForCompletedJobsHaveFinesDeducted` |
| 8 | A request keeps the offering snapshot it was made with | Confirmed | `reservation/domain/Reservation.java` `offering` field (line 54, final) and `edit` checking against it (line 127); `offering/domain/ProviderListing.java` `snapshot()` | `ProviderListingGoLiveTest.aLaterChangeDoesNotTouchASnapshotAlreadyTaken` |

All eight are confirmed. The other choices made while building are listed in the README under
"Decisions taken while building", and all of them were confirmed too.

## 4. Gates per context

From `mvn clean verify` and `python3 tools/quality_gates.py` on this commit.

| Context | Worst CRAP (limit < 6) | Mutation score (limit ≥ 90%) | Test strength | Mutants | Dependency rule test |
|---|---|---|---|---|---|
| kernel | 3.0 | 100% | 100% | 37 | `KernelArchitectureTest`: only `java..`; public types are records |
| catalogue | 4.0 | 100% | 100% | 80 | `CatalogueArchitectureTest` |
| policy | 3.0 | 100% | 100% | 58 | `PolicyArchitectureTest` |
| account | 5.0 | 100% | 100% | 86 | `AccountArchitectureTest` |
| offering | 4.0 | 100% | 100% | 158 | `OfferingArchitectureTest` |
| availability | 4.0 | 100% | 100% | 66 | `AvailabilityArchitectureTest` |
| reservation | 5.0 | 100% | 100% | 269 | `ReservationArchitectureTest` |
| payment | 5.0 | 100% | 100% | 127 | `PaymentArchitectureTest` |
| resolution | 3.0 | 100% | 100% | 40 | `ResolutionArchitectureTest` |
| conversation | 3.0 | 100% | 100% | 38 | `ConversationArchitectureTest` |
| discovery | 5.0 | 100% | 100% | 70 | `DiscoveryArchitectureTest` |
| ie | 1.0 | n/a (exempt in the plan) | n/a | – | `IntegrationEventArchitectureTest` |

- **CRAP below 6:** passes in every context. The worst is 5.0, in account, reservation, payment and
  discovery.
- **Mutation at least 90% for kernel and domain:** every context is at 100% score and test strength,
  1029 of 1029 mutants killed. The ratchet in `quality/ratchet.json` holds each context at 100%.
- **Domain imports only kernel:** each context's `<Context>ArchitectureTest` checks that its domain
  depends only on itself, `kernel..` and `java..`. That also stops any context importing another. A
  planted `offering → policy` import failed this test.
- **`ie` imports only kernel:** `IntegrationEventArchitectureTest.ieDependsOnlyOnTheKernelAndJava`.
- **No domain imports `ie`:** `IntegrationEventArchitectureTest.noDomainImportsTheEventContracts`. A
  planted `payment → ie` import failed this test.
- **`kernel` imports nothing outside the JDK:** `KernelArchitectureTest.kernelDependsOnlyOnTheJavaStandardLibrary`.
- **Domain exceptions** live in each context's `domain/exception` package, checked by
  `domainExceptionsLiveInTheExceptionPackage`.

## 5. Integration events

- **No `Optional` fields** (commit `17015a2`). Events that could have had a missing value were split, so
  every field is always present:
  - The listing events carry `travelIncluded` and a `travelFee` that is RM0 when travel is included.
  - `ListingWentOfflineIE` is new.
  - `EnquiryRequestedIE` replaces the optional enquiry id on `ReservationRequestedIE`.
  - `CaseDecidedIE.providerReceives` is always set, from `Case.providerReceives`.
- **The event rule is enforced** by
  `IntegrationEventContractTest.anEventCarriesOnlyPrimitivesStringsKernelAndJdkValueTypes`. It allows
  primitives, Strings, `Money`, `Percentage`, `Instant`, `Duration`, and a `List` or `Set` of Strings.
  `Optional` is not allowed. The rule's wording is the same in the plan, the discipline and the README.
- **Collections are copied** with `List.copyOf` or `Set.copyOf` in every compact constructor.
  `anEventCannotBeChangedAfterItIsBuilt` checks this, and `anEventCanOnlyBeBuiltWithEveryRequiredField`
  checks that nulls are rejected.
- **Rejected verification has its own events** (commit `219d592`): `ProviderVerificationRejectedIE` and
  `HelperVerificationRejectedIE`, so there is one event per outcome. Both are in the plan's event table,
  listened to by offering and notifications, and in the contract test.
- **36 events in all**, each in the contract test: the plan's 32, plus `ListingWentOfflineIE`,
  `EnquiryRequestedIE` and the two rejected events.
