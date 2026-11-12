# Domain phase audit

Checked against `docs/homa-domain-plan.md` and `docs/homa-product-description.md` on a clean build.
Updated after the follow-up decisions (G1 to G4 and #5).

## 1. Every Gherkin scenario has a matching test

The plan has **244** scenarios: **232 have a test**, and **12 are retired** because the rule they describe moved to the KYC provider, or no longer exists now that every provider is a business. The tested ones are the original 122, plus:
- five in the "Buffer between jobs" feature
- 25 in the four `feedback` features
- the "Team membership" change, which replaced the two "Helpers" scenarios with six and reworded one
  go-live scenario
- 14 in the two re-verification features
- five in "Verified for the whole job"
- 23 in the four features for provider types, supervisors, face check-in and adding a team member
  (the expiry scenario was removed with the time limit)
- seven in "Business verification through eKYB"; the older "A business cannot go live before its
  registration is checked" now maps to the go-live test
- eleven in the features for business providers only and verification by a KYC provider
- fifteen in the features for ongoing monitoring, a seriously flagged provider and its payouts
- thirteen in the three features added while reviewing the usecase plan: retired jobs and tools, a
  payment that fails at request, and a request paid before the provider answers

**Retired (12):**
- Due-date maths and re-check timing (rows 157, 158, 180).
- Early re-checks (rows 165 to 167).
- Document and nationality rules (rows 176, 177, 195, 202).

These are now the KYC provider's rules, listed as Homa's requirements for it in the product
description. Rows 200 and 203 are about independent providers and eKYC for a business, which no longer
exist. The other verification scenarios now map to the tests of the KYC-result rules.

The plan's older scenarios on invites, the lead and the 1 to 10 helper limit describe rules these
changes replaced. Their rows point to the tests of the rule that replaced them:
- **Team membership (rows 12 to 17)** now map to `TeamMemberAddingTest`, and one to
  `TeamMemberReverificationTest`.
- **"Max helpers cannot exceed 10" (row 21)** maps to `homaSetsNoUpperLimitOnHelpers`.
- **The lead scenarios (rows 71, 73 and 74)** map to the helper and supervisor naming tests.
- **The helper search range (row 38)** maps to `aSearchIsForAtLeastOneHelper`. The script that checks this maps each scenario
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
| 11 | Provider readiness | A provider is ready for go-live only when verified and payout-ready (outline) | `ProviderAccountReadinessTest` | `aBusinessIsReadyWhenVerifiedAndPayoutReady` |
| 12 | Team membership | An invited helper is not yet a team member | `TeamMemberAddingTest` | `aWaitingVerificationIsNotOnTheTeam` |
| 13 | Team membership | A verified invite joins the team | `TeamMemberAddingTest` | `aConfirmedAndVerifiedPersonIsAdded` |
| 14 | Team membership | A rejected invite stays off the team and can retry | `TeamMemberAddingTest` | `aRejectedVerificationAddsNothing` |
| 15 | Team membership | The provider can withdraw an invite or remove a member (outline) | `TeamMemberAddingTest` | `theProviderCanRemoveATeamMember` |
| 16 | Team membership | A removed helper cannot be verified back onto the team | `TeamMemberReverificationTest` | `aRemovedMemberIsNotReverifiedOrLapsed` |
| 17 | Team membership | An invite needs a name and a photo | `TeamMemberAddingTest` | `theAddFormNeedsAProviderARolePhotoAndTime` |
| 18 | Offering setup | A provider can only offer jobs from Homa's job list | `ProviderListingSetupTest` | `aProviderCanOnlyOfferJobsFromHomasJobList` |
| 19 | Offering setup | Hours must sit within Homa's range (outline) | `ProviderListingSetupTest` | `hoursWithinHomasRangeAreAccepted`, `hoursMustSitWithinHomasRange`, `theMinimumHoursCannotBeAboveTheMaximum` |
| 20 | Offering setup | Max helpers cannot exceed the verified team size | `ProviderListingSetupTest` | `maxHelpersCannotExceedTheVerifiedTeamSize` |
| 21 | Offering setup | Max helpers cannot exceed 10 | `ProviderListingSetupTest` | `homaSetsNoUpperLimitOnHelpers` |
| 22 | Offering setup | The helper range shrinks when the team shrinks | `ProviderListingSetupTest` | `theHelperRangeShrinksWhenTheTeamShrinks` |
| 23 | Offering setup | Cities must be launch cities | `ProviderListingSetupTest` | `citiesMustBeLaunchCities` |
| 24 | Going live | A complete, verified provider goes live | `ProviderListingGoLiveTest` | `aCompleteVerifiedProviderGoesLive` |
| 25 | Going live | Missing requirements block go-live and are listed | `ProviderListingGoLiveTest` | `missingRequirementsBlockGoLiveAndAreListed` |
| 26 | Going live | A pending invite does not block go-live | `ProviderListingGoLiveTest` | `aPendingInviteDoesNotBlockGoLive` |
| 27 | Provider availability | A slot inside a window with nothing else is available | `ProviderCalendarAvailabilityTest` | `aSlotInsideAWindowWithNothingElseIsAvailable` |
| 28 | Provider availability | A synced busy block makes the slot unavailable | `ProviderCalendarAvailabilityTest` | `aSyncedBusyBlockMakesTheSlotUnavailable` |
| 29 | Provider availability | An accepted reservation holds the slot | `ProviderCalendarAvailabilityTest` | `anAcceptedReservationHoldsTheSlot` |
| 30 | Provider availability | Releasing a cancelled reservation frees the slot | `ProviderCalendarAvailabilityTest` | `releasingACancelledReservationFreesTheSlot` |
| 31 | Provider availability | Holding an overlapping slot is rejected | `ProviderCalendarAvailabilityTest` | `holdingAnOverlappingSlotIsRejected` |
| 32 | Provider availability | A slot partly outside the window is not available | `ProviderCalendarAvailabilityTest` | `aSlotPartlyOutsideTheWindowIsNotAvailable` |
| 33 | Buffer between jobs | A job cannot start within the buffer after another job | `ProviderCalendarBufferTest` | `aJobCannotStartWithinTheBufferAfterAnotherJob` |
| 34 | Buffer between jobs | A job cannot end within the buffer before another job | `ProviderCalendarBufferTest` | `aJobCannotEndWithinTheBufferBeforeAnotherJob` |
| 35 | Buffer between jobs | Holding a slot inside the buffer is rejected | `ProviderCalendarBufferTest` | `holdingASlotInsideTheBufferIsRejected` |
| 36 | Buffer between jobs | The buffer is kept around jobs, not around synced busy blocks | `ProviderCalendarBufferTest` | `theBufferIsKeptAroundJobsNotAroundSyncedBusyBlocks` |
| 37 | Buffer between jobs | The buffer cannot be negative | `ProviderCalendarBufferTest` | `theBufferCannotBeNegative` |
| 38 | Search criteria | Search values must be within Homa's ranges (outline) | `SearchCriteriaValidationTest` | `searchValuesWithinHomasRangesAreAccepted`, `theDurationMustBeWithinHomasRange`, `aSearchIsForAtLeastOneHelper` |
| 39 | Search criteria | The start must respect the minimum notice | `SearchCriteriaValidationTest` | `theStartMustRespectTheMinimumNotice` |
| 40 | Matching | Exact match | `MatchClassifierMatchingTest` | `aProviderThatFitsEverythingIsAnExactMatch` |
| 41 | Matching | Some jobs only is a close match | `MatchClassifierMatchingTest` | `someJobsOnlyIsACloseMatch` |
| 42 | Matching | Free near the time is a close match | `MatchClassifierMatchingTest` | `freeNearTheTimeIsACloseMatch` |
| 43 | Matching | Not covering the city is no match | `MatchClassifierMatchingTest` | `notCoveringTheCityIsNoMatch` |
| 44 | Matching | Helpers outside the range is no match | `MatchClassifierMatchingTest` | `helpersOutsideTheRangeIsNoMatch` |
| 45 | Pricing | Price with a separate travel fee | `ReservationPricingTest` | `aSeparateTravelFeeIsAddedToTheHoursFeeBeforeTheServiceFee` |
| 46 | Pricing | Price with the travel fee included | `ReservationPricingTest` | `withTravelIncludedTheProviderPriceIsTheHoursFee` |
| 47 | Pricing | Choosing fewer jobs does not change the price | `ReservationPricingTest` | `choosingFewerJobsDoesNotChangeThePrice` |
| 48 | Requesting a reservation | Request details must fit Homa's and the provider's rules (outline) | `ReservationRequestTest` | `requestDetailsMustFitHomasAndTheProvidersRules` |
| 49 | Requesting a reservation | Jobs must be offered by the provider | `ReservationRequestTest` | `jobsMustBeOfferedByTheProvider` |
| 50 | Requesting a reservation | The customer's city must be covered | `ReservationRequestTest` | `theCustomersCityMustBeCovered` |
| 51 | Requesting a reservation | The reservation keeps the policy in force when requested | `ReservationRequestTest` | `theReservationKeepsThePolicyInForceWhenRequested` |
| 52 | Request expiry | A request expires after 24 hours or at the booked start, whichever is sooner (outline) | `ReservationRequestExpiryTest` | `aRequestExpiresAfter24HoursOrAtTheBookedStartWhicheverIsSooner` |
| 53 | Request expiry | The provider cannot accept an expired request | `ReservationRequestExpiryTest` | `theProviderCannotAcceptAnExpiredRequest` |
| 54 | Request expiry | An unanswered request expires | `ReservationRequestExpiryTest` | `anUnansweredRequestExpires` |
| 55 | Accepting and declining | The provider accepts a request | `ReservationAcceptanceTest` | `theProviderAcceptsARequest` |
| 56 | Accepting and declining | The provider declines a request | `ReservationAcceptanceTest` | `theProviderDeclinesARequest` |
| 57 | Accepting and declining | Only a requested reservation can be accepted | `ReservationAcceptanceTest` | `onlyARequestedReservationCanBeAccepted` |
| 58 | Editing a request | The customer edits a request before it is accepted | `ReservationEditingTest` | `theCustomerEditsARequestBeforeItIsAccepted` |
| 59 | Editing a request | A reserved booking cannot be edited | `ReservationEditingTest` | `aReservedBookingCannotBeEdited` |
| 60 | Customer cancellation | Cancelling a request is free on every tier | `ReservationCustomerCancellationTest` | `customerCancelsForFreeOnEveryTier` |
| 61 | Customer cancellation | Cancelling a reserved booking before the start (outline) | `ReservationCustomerCancellationTest` | `cancellingAReservedBookingBeforeTheStartFollowsTheTier` |
| 62 | Customer cancellation | With travel included, the tier applies to the full provider price | `ReservationCustomerCancellationTest` | `withTravelIncludedTheTierAppliesToTheFullProviderPrice` |
| 63 | Customer cancellation | The customer cannot cancel during the check-in period before check-in | `ReservationCustomerCancellationTest` | `customerCannotCancelDuringCheckInPeriodBeforeCheckIn` |
| 64 | Customer cancellation | Cancelling after check-in counts as a customer no-show | `ReservationCustomerCancellationTest` | `cancellingAfterCheckInCountsAsACustomerNoShow` |
| 65 | Customer cancellation | A cancellation fee is released straight away | `ReservationCustomerCancellationTest` | `aCancellationFeeIsReleasedStraightAway` |
| 66 | Provider cancellation and fines | Provider fines depend on timing only (outline) | `ReservationProviderCancellationTest` | `providerFinesDependOnTimingOnly` |
| 67 | Provider cancellation and fines | Cancelling after the booked start but before check-in carries the no-show fine | `ReservationProviderCancellationTest` | `cancellingAfterTheBookedStartButBeforeCheckInCarriesTheNoShowFine` |
| 68 | Provider cancellation and fines | Cancelling after check-in carries the no-show fine | `ReservationProviderCancellationTest` | `cancellingAfterCheckInCarriesTheNoShowFine` |
| 69 | Provider cancellation and fines | The fine does not depend on the provider's tier | `ReservationProviderCancellationTest` | `theFineDoesNotDependOnTheProvidersTier` |
| 70 | Provider cancellation and fines | With travel included, the fine applies to the full provider price | `ReservationProviderCancellationTest` | `withTravelIncludedTheFineAppliesToTheFullProviderPrice` |
| 71 | Naming who is coming | The provider names the lead and helpers | `ReservationTeamNamingTest` | `theProviderNamesTheHelpers` |
| 72 | Naming who is coming | The team must match the number of helpers booked | `ReservationTeamNamingTest` | `theTeamMustMatchTheNumberOfHelpersBooked` |
| 73 | Naming who is coming | Helpers must be on the verified roster | `ReservationTeamNamingTest` | `helpersMustBeOnTheTeamAsHelpers` |
| 74 | Naming who is coming | The lead must be one of the named helpers | `ReservationTeamNamingTest` | `theSamePersonCannotBeNamedTwice` |
| 75 | Naming who is coming | The team cannot change from the booked start | `ReservationTeamNamingTest` | `theTeamCannotChangeFromTheBookedStart` |
| 76 | Check-in | Check-in needs the device near the customer within the check-in period (outline) | `ReservationCheckInTest` | `checkInNeedsTheDeviceNearTheCustomerWithinTheCheckInPeriod` |
| 77 | Check-in | Check-in is locked until the team is named | `ReservationCheckInTest` | `checkInIsLockedUntilTheTeamIsNamed` |
| 78 | Check-in | No check-in by the end of the check-in period (10:15) is a provider no-show | `ReservationCheckInTest` | `noCheckInByTheEndOfTheCheckInPeriodIsAProviderNoShow` |
| 79 | Check-in | A provider no-show cannot be marked early | `ReservationCheckInTest` | `providerNoShowIsMarkedOnlyAfterCheckInPeriodEnds` |
| 80 | Customer no-show | The provider reports a customer no-show after the wait | `ReservationCustomerNoShowTest` | `theProviderReportsACustomerNoShowAfterTheWait` |
| 81 | Customer no-show | A customer no-show cannot be reported before the wait ends | `ReservationCustomerNoShowTest` | `aCustomerNoShowCannotBeReportedBeforeTheWaitEnds` |
| 82 | Customer no-show | A customer no-show needs a check-in | `ReservationCustomerNoShowTest` | `aCustomerNoShowNeedsACheckIn` |
| 83 | Customer no-show | The customer challenges a no-show within the report window | `ReservationCustomerNoShowTest` | `theCustomerChallengesANoShowWithinTheReportWindow` |
| 84 | Customer no-show | Homa upholds the no-show and the reservation ends cancelled | `ReservationDecisionTest` | `homaUpholdsTheNoShowAndTheReservationEndsCancelled` |
| 85 | Customer no-show | Homa finds the no-show wrongful and refunds in full | `ReservationDecisionTest` | `homaFindsTheNoShowWrongfulAndRefundsInFull` |
| 86 | Customer no-show | A challenge after the report window is rejected | `ReservationCustomerNoShowTest` | `aChallengeAfterTheReportWindowIsRejected` |
| 87 | Customer no-show | The no-show fee never exceeds the ceiling | `SettlementInvariantTest` | `theNoShowFeeIsTheCeiling` |
| 88 | Completion | Completion needs photo or video evidence | `ReservationCompletionTest` | `completionNeedsPhotoOrVideoEvidence` |
| 89 | Completion | Completion needs a check-in first | `ReservationCompletionTest` | `completionNeedsACheckInFirst` |
| 90 | Completion | The completion reminder is due 30 minutes after the booked end (outline) | `ReservationCompletionTest` | `theCompletionReminderIsDue30MinutesAfterTheBookedEnd` |
| 91 | Completion | Missing the completion deadline sends it to Homa | `ReservationCompletionTest` | `missingTheCompletionDeadlineSendsItToHoma` |
| 92 | Completion | The completion deadline cannot be missed early | `ReservationCompletionTest` | `theCompletionDeadlineCannotBeMissedEarly` |
| 93 | Dispute window and payout | The window ends 1 hour after the later of the booked end and completion (outline) | `ReservationDisputeWindowTest` | `theWindowEndsAnHourAfterTheLaterOfTheBookedEndAndCompletion` |
| 94 | Dispute window and payout | The window has no end until completion is submitted | `ReservationDisputeWindowTest` | `theWindowHasNoEndUntilCompletionIsSubmitted` |
| 95 | Dispute window and payout | The window closes with no report and the payment is released | `ReservationDisputeWindowTest` | `theWindowClosesWithNoReportAndThePaymentIsReleased` |
| 96 | Dispute window and payout | The window cannot close early | `ReservationDisputeWindowTest` | `theWindowCannotCloseEarly` |
| 97 | Reports | The customer files a report inside the window | `ReservationReportTest` | `theCustomerFilesAReportInsideTheWindow` |
| 98 | Reports | The provider can file a report too | `ReservationReportTest` | `theProviderCanFileAReportToo` |
| 99 | Reports | Each party files at most one report | `ReservationReportTest` | `eachPartyFilesAtMostOneReport` |
| 100 | Reports | Reports before the booked start are rejected | `ReservationReportTest` | `reportsBeforeTheBookedStartAreRejected` |
| 101 | Reports | Reports after the window are rejected | `ReservationReportTest` | `reportsAfterTheWindowAreRejected` |
| 102 | Homa's decision | Release | `ReservationDecisionTest` | `homaReleasesThePaymentAndTheJobIsCompleted` |
| 103 | Homa's decision | Partial refund | `ReservationDecisionTest` | `aPartialRefundSplitsThePaymentAndTheServiceFeeFollowsTheProvider` |
| 104 | Homa's decision | Full refund | `ReservationDecisionTest` | `aFullRefundReturnsEverythingToTheCustomer` |
| 105 | Homa's decision | A partial refund cannot give the provider more than the provider price | `ReservationDecisionTest` | `aPartialRefundCannotGiveTheProviderMoreThanTheProviderPrice` |
| 106 | Enquiries | An enquiry closes when its date passes without a request | `EnquiryLifecycleTest` | `anEnquiryClosesWhenItsDatePassesWithoutARequest` |
| 107 | Enquiries | An enquiry turns into one reservation only | `EnquiryLifecycleTest` | `anEnquiryTurnsIntoOneReservationOnly` |
| 108 | Enquiries | Book again copies the details with a new date | `EnquiryLifecycleTest` | `bookAgainCopiesTheDetailsWithANewDate` |
| 109 | Taking payment | A card is authorised, not charged, at request | `PaymentTakingTest` | `aCardIsAuthorisedNotChargedAtRequest` |
| 110 | Taking payment | FPX is paid upfront | `PaymentTakingTest` | `fpxIsPaidUpfront` |
| 111 | Taking payment | A card is charged when the provider accepts | `PaymentTakingTest` | `aCardIsChargedWhenTheProviderAccepts` |
| 112 | Taking payment | A declined card request releases the authorisation and refunds nothing | `PaymentTakingTest` | `aDeclinedCardRequestReleasesTheAuthorisationAndRefundsNothing` |
| 113 | Taking payment | A declined FPX request is refunded in full | `PaymentTakingTest` | `aDeclinedFpxRequestIsRefundedInFull` |
| 114 | Taking payment | Editing an FPX request upward collects the difference | `PaymentTakingTest` | `editingAnFpxRequestUpwardCollectsTheDifference` |
| 115 | Settling | A settlement is fully accounted for | `PaymentSettlementTest` | `aSettlementIsFullyAccountedFor` |
| 116 | Settling | A settlement that doesn't add up is rejected | `PaymentSettlementTest` | `aSettlementThatDoesNotAddUpIsRejected` |
| 117 | Settling | A payment cannot be settled twice | `PaymentSettlementTest` | `aPaymentCannotBeSettledTwice` |
| 118 | Fines and payouts | A fine is deducted at most 50% per payout | `FineDeductionPolicyTest` | `aFineIsDeductedAtMostHalfOfAPayout` |
| 119 | Fines and payouts | The rest of the fine comes off the next payout | `FineDeductionPolicyTest` | `theRestOfTheFineComesOffTheNextPayout` |
| 120 | Fines and payouts | Older fines are deducted first | `FineDeductionPolicyTest` | `olderFinesAreDeductedFirst` |
| 121 | Fines and payouts | A waived fine is not deducted | `FineDeductionPolicyTest` | `aWaivedFineIsNotDeducted` |
| 122 | Fines and payouts | A partly deducted fine can be waived | `FineAndPayoutTest` | `aPartlyDeductedFineCanBeWaived` |
| 123 | Deciding a case | A decision needs a reason | `CaseDecisionTest` | `aDecisionNeedsAReason` |
| 124 | Deciding a case | A case is decided once | `CaseDecisionTest` | `aCaseIsDecidedOnce` |
| 125 | Deciding a case | A partial refund must be within the provider price | `CaseDecisionTest` | `aPartialRefundMustBeWithinTheProviderPrice` |
| 126 | Deciding a case | Every source can be decided with any outcome (outline) | `CaseDecisionTest` | `everySourceCanBeDecidedWithAnyOutcome` |
| 127 | Chat | Only the customer can start a chat | `ConversationChatTest` | `onlyTheCustomerCanStartAChat` |
| 128 | Chat | The provider can reply once the customer has started | `ConversationChatTest` | `theProviderCanReplyOnceTheCustomerHasStarted` |
| 129 | Chat | Requesting from an enquiry keeps the same chat | `ConversationChatTest` | `requestingFromAnEnquiryKeepsTheSameChat` |
| 130 | Chat | An archived chat is readable but closed | `ConversationChatTest` | `anArchivedChatIsReadableButClosed` |
| 131 | Chat | A reservation's chat stays open after the job | `ConversationChatTest` | `aReservationsChatStaysOpenAfterTheJob` |
| 132 | Rating a completed reservation | The customer rates the provider | `RatingSubmissionTest` | `theCustomerRatesTheProvider` |
| 133 | Rating a completed reservation | Only the reservation's customer can rate it | `RatingSubmissionTest` | `onlyTheReservationsCustomerCanRateIt` |
| 134 | Rating a completed reservation | A reservation is rated once | `RatingSubmissionTest` | `aReservationIsRatedOnce` |
| 135 | Rating a completed reservation | Ratings are accepted only within the rating window (outline) | `RatingSubmissionTest` | `ratingsAreAcceptedOnlyWithinTheRatingWindow` |
| 136 | Rating a completed reservation | Stars are whole numbers from 1 to 5 (outline) | `RatingSubmissionTest` | `starsAreWholeNumbersFromOneToFive`, `starsOutsideOneToFiveAreRejected` |
| 137 | Rating a completed reservation | Written feedback is optional | `RatingSubmissionTest` | `writtenFeedbackIsOptional` |
| 138 | Rating a completed reservation | Provider category stars are optional | `RatingSubmissionTest` | `providerCategoryStarsAreOptional` |
| 139 | Rating a completed reservation | Any number of provider categories can be rated | `RatingSubmissionTest` | `anyNumberOfProviderCategoriesCanBeRated` |
| 140 | Rating a completed reservation | The provider rating needs overall stars | `RatingSubmissionTest` | `theProviderRatingNeedsOverallStars` |
| 141 | Rating a completed reservation | Written feedback has a length limit | `RatingSubmissionTest` | `writtenFeedbackHasALengthLimit` |
| 142 | Rating a completed reservation | Only helpers who were named can be rated | `RatingSubmissionTest` | `onlyHelpersWhoWereNamedCanBeRated` |
| 143 | Rating a completed reservation | Helper ratings are optional | `RatingSubmissionTest` | `helperRatingsAreOptional` |
| 144 | Rating a completed reservation | A helper can receive overall and category stars | `RatingSubmissionTest` | `aHelperCanReceiveOverallAndCategoryStars` |
| 145 | Rating a completed reservation | A rated helper needs overall stars | `RatingSubmissionTest` | `aRatedHelperNeedsOverallStars` |
| 146 | Rating a completed reservation | Helper category stars are optional | `RatingSubmissionTest` | `helperCategoryStarsAreOptional` |
| 147 | What can be rated | Only Completed reservations can be rated (outline) | `RatingEligibilityTest` | `onlyCompletedReservationsCanBeRated` |
| 148 | Hiding a rating | Homa hides a rating with a reason | `RatingHidingTest` | `homaHidesARatingWithAReason` |
| 149 | Hiding a rating | Hiding needs a reason | `RatingHidingTest` | `hidingNeedsAReason` |
| 150 | Hiding a rating | A rating is hidden once | `RatingHidingTest` | `aRatingIsHiddenOnce` |
| 151 | Rating summaries | The overall average is shown only after the minimum number of ratings (outline) | `RatingSummaryAveragingTest` | `theOverallAverageIsShownOnlyAfterTheMinimumNumberOfRatings` |
| 152 | Rating summaries | The average is rounded to one decimal place | `RatingSummaryAveragingTest` | `theAverageIsRoundedToOneDecimalPlace` |
| 153 | Rating summaries | Withdrawing a hidden rating removes it from the averages | `RatingSummaryAveragingTest` | `withdrawingAHiddenRatingRemovesItFromTheAverages` |
| 154 | Rating summaries | Category averages count only the ratings that rated that category | `RatingSummaryAveragingTest` | `categoryAveragesCountOnlyTheRatingsThatRatedThatCategory` |
| 155 | Rating summaries | A category average is shown once that category has the minimum ratings | `RatingSummaryAveragingTest` | `aCategoryAverageIsShownOnceThatCategoryHasTheMinimumRatings` |
| 156 | Rating summaries | A summary cannot withdraw more ratings than it has | `RatingSummaryAveragingTest` | `aSummaryCannotWithdrawMoreRatingsThanItHas` |
| 157 | Periodic re-verification | Re-verification is due 3 months after the last verification | — | retired: the KYC provider's rule |
| 158 | Periodic re-verification | The re-check opens 14 days before the due date (outline) | — | retired: the KYC provider's rule |
| 159 | Periodic re-verification | Passing the re-check resets the due date | `TeamMemberReverificationTest` | `aPassedReverificationKeepsTheMemberVerifiedUntilTheNewDate` |
| 160 | Periodic re-verification | A failed re-check can be retried before the due date | `TeamMemberReverificationTest` | `aFailedReverificationChangesNothing` |
| 161 | Periodic re-verification | A repeated VERIFIED result with no re-check open changes nothing | `TeamMemberReverificationTest` | `aRepeatedPassChangesNothing` |
| 162 | Periodic re-verification | A REJECTED result with no re-check open is refused | `TeamMemberReverificationTest` | `aFailedReverificationChangesNothing` |
| 163 | Periodic re-verification | Missing the due date lapses the member (outline) | `TeamMemberReverificationTest` | `aLapsedMemberLeavesTheTeam` |
| 164 | Periodic re-verification | A lapsed member who passes rejoins the team | `TeamMemberReverificationTest` | `aLapsedMemberWhoPassesRejoinsTheTeam` |
| 165 | Periodic re-verification | An early re-check moves the due date sooner | — | retired: the KYC provider's rule |
| 166 | Periodic re-verification | An early re-check never moves the due date later | — | retired: the KYC provider's rule |
| 167 | Periodic re-verification | An early re-check needs a reason | — | retired: the KYC provider's rule |
| 168 | Provider re-verification | A lapsed provider is not ready and takes no new requests | `ProviderReverificationTest` | `aLapsedBusinessIsNotReadyAndTakesNoNewRequests` |
| 169 | Provider re-verification | A paused listing refuses new requests but keeps accepted ones | `ReservationRequestTest` | `aPausedListingRefusesNewRequestsButKeepsAcceptedOnes` |
| 170 | Provider re-verification | A paused listing resumes when the provider passes | `ProviderListingPauseTest` | `aPausedListingResumesWhenTheProviderPasses` |
| 171 | Verified for the whole job | A job must end by the provider's due date (outline) | `ReservationProviderVerificationTest` | `aJobEndingExactlyAtTheDueDateCanBeRequested`, `aJobEndingAfterTheProvidersDueDateCannotBeRequested` |
| 172 | Verified for the whole job | An edit cannot move the end past the due date | `ReservationProviderVerificationTest` | `anEditCannotMoveTheEndPastTheDueDate` |
| 173 | Verified for the whole job | The provider cannot accept a job ending after their due date as it is now | `ReservationProviderVerificationTest` | `theProviderCannotAcceptAJobEndingAfterTheirDueDateAsItIsNow` |
| 174 | Verified for the whole job | The provider can accept once a passed re-check moves the due date later | `ReservationProviderVerificationTest` | `theProviderCanAcceptOnceAPassedRecheckMovesTheDueDateLater` |
| 175 | Verified for the whole job | Search leaves out a provider whose due date comes before the job ends | `MatchClassifierMatchingTest` | `aJobEndingAfterTheProvidersDueDateIsNoMatch` |
| 176 | Provider types and documents | The provider must be Malaysian (outline) | — | retired: the KYC provider's rule |
| 177 | Provider types and documents | Only a business provider can have foreign team members (outline) | — | retired: the KYC provider's rule |
| 178 | Provider types and documents | Team members can only be invited after the lawful-employment confirmation | `TeamMemberAddingTest` | `theAddFormNeedsTheConfirmation` |
| 179 | Provider types and documents | The confirmation is recorded with its time and terms version | `TeamMemberAddingTest` | `theConfirmationRecordsItsTimeAndTermsVersion` |
| 180 | Provider types and documents | The due date follows the cycle only | — | retired: the KYC provider's rule |
| 181 | Provider types and documents | The registered name comes from the eKYC result | `TeamMemberAddingTest` | `aConfirmedAndVerifiedPersonIsAdded` |
| 182 | Provider types and documents | A business cannot go live before its registration is checked | `ProviderAccountReadinessTest` | `aBusinessIsReadyWhenVerifiedAndPayoutReady` |
| 183 | Supervisors | A supervisor is required from either threshold (outline) | `ProviderListingSupervisorTest` | `aSupervisorIsRequiredFromEitherThreshold` |
| 184 | Supervisors | Naming the team without a required supervisor is refused | `ReservationTeamNamingTest` | `namingTheTeamWithoutARequiredSupervisorIsRefused` |
| 185 | Supervisors | Supervisors do not count towards the helpers range | `ProviderListingSetupTest` | `supervisorsDoNotCountTowardsTheHelpersRange` |
| 186 | Supervisors | Supervisors are not charged | `ReservationTeamNamingTest` | `supervisorsAreNotCharged` |
| 187 | Supervisors | A supervisor rule needs a supervisor on the team | `ProviderListingSupervisorTest` | `aSupervisorRuleNeedsASupervisorOnTheTeam` |
| 188 | Face check at check-in | Check-in completes when the first person passes | `ReservationFaceCheckTest` | `checkInCompletesWhenTheFirstPersonPasses` |
| 189 | Face check at check-in | Location alone is not a check-in | `ReservationFaceCheckTest` | `locationAloneIsNotACheckIn` |
| 190 | Face check at check-in | A person who fails every attempt cannot work | `ReservationFaceCheckTest` | `aPersonWhoseFaceCheckFailedCannotWork` |
| 191 | Face check at check-in | A face check outage never makes a no-show | `ReservationFaceCheckTest` | `aFaceCheckOutageNeverMakesANoShow` |
| 192 | Face check at check-in | No face pass by the end of the check-in period is a provider no-show | `ReservationFaceCheckTest` | `noFacePassByTheEndOfTheCheckInPeriodIsAProviderNoShow` |
| 193 | Adding a team member | A confirmed and verified person is added | `TeamMemberAddingTest` | `aConfirmedAndVerifiedPersonIsAdded` |
| 194 | Adding a team member | The add form needs the confirmation | `TeamMemberAddingTest` | `theAddFormNeedsTheConfirmation` |
| 195 | Adding a team member | Only a business chooses the issuing country (outline) | — | retired: the KYC provider's rule |
| 196 | Adding a team member | A person cannot be added before verification | `TeamMemberAddingTest` | `aPersonCannotBeAddedBeforeVerification` |
| 197 | Adding a team member | A rejected eKYC adds nothing | `TeamMemberAddingTest` | `aRejectedVerificationAddsNothing` |
| 198 | Adding a team member | A verification is used once | `TeamMemberAddingTest` | `aVerificationIsUsedOnce` |
| 199 | Business verification through eKYB | A business is verified through eKYB | `ProviderAccountReadinessTest` | `aBusinessIsVerifiedThroughKyb` |
| 200 | Business verification through eKYB | A business has no team members after eKYB | — | retired: every provider is a business, verified only through KYB |
| 201 | Business verification through eKYB | A rejected eKYB can be redone | `ProviderAccountReadinessTest` | `aRejectedKybCanBeRedone` |
| 202 | Business verification through eKYB | The owner or director must be Malaysian | — | retired: the KYC provider's rule |
| 203 | Business verification through eKYB | A business does not take a separate eKYC | — | retired: every provider is a business, verified only through KYB |
| 204 | Business verification through eKYB | A business cannot go live before its eKYB passes | `ProviderAccountReadinessTest` | `aBusinessIsReadyWhenVerifiedAndPayoutReady` |
| 205 | Business verification through eKYB | A business is re-verified through eKYB | `ProviderReverificationTest` | `aPassedReverificationKeepsTheBusinessVerifiedUntilTheNewDate` |
| 206 | A business provider is verified by the KYC provider | A business is verified through KYB | `ProviderAccountReadinessTest` | `aBusinessIsVerifiedThroughKyb` |
| 207 | A business provider is verified by the KYC provider | A rejected KYB can be redone | `ProviderAccountReadinessTest` | `aRejectedKybCanBeRedone` |
| 208 | A business provider is verified by the KYC provider | A business is ready for go-live only when verified and payout-ready | `ProviderAccountReadinessTest` | `aBusinessIsReadyWhenVerifiedAndPayoutReady` |
| 209 | The KYC provider re-verifies a business | A passed re-verification keeps the business verified until the new date | `ProviderReverificationTest` | `aPassedReverificationKeepsTheBusinessVerifiedUntilTheNewDate` |
| 210 | The KYC provider re-verifies a business | A repeated pass changes nothing | `ProviderReverificationTest` | `aRepeatedPassChangesNothing` |
| 211 | The KYC provider re-verifies a business | A failed re-verification changes nothing | `ProviderReverificationTest` | `aFailedReverificationChangesNothing` |
| 212 | The KYC provider re-verifies a business | A lapsed business takes no new requests until it passes | `ProviderReverificationTest` | `aLapsedBusinessIsNotReadyAndTakesNoNewRequests`, `aLapsedBusinessThatPassesIsVerifiedAgain` |
| 213 | The KYC provider re-verifies a team member | A passed re-verification keeps the member verified until the new date | `TeamMemberReverificationTest` | `aPassedReverificationKeepsTheMemberVerifiedUntilTheNewDate` |
| 214 | The KYC provider re-verifies a team member | A lapsed member leaves the team until they pass | `TeamMemberReverificationTest` | `aLapsedMemberLeavesTheTeam`, `aLapsedMemberWhoPassesRejoinsTheTeam` |
| 215 | Face check at check-in by the KYC provider | A person whose face check failed cannot work | `ReservationFaceCheckTest` | `aPersonWhoseFaceCheckFailedCannotWork` |
| 216 | Face check at check-in by the KYC provider | Each person has one face check result | `ReservationFaceCheckTest` | `eachPersonHasOneFaceCheckResult` |
| 217 | Ongoing monitoring by the KYC provider | A required re-verification moves the date sooner | `OngoingMonitoringTest` | `aRequiredReverificationMovesTheDateSooner` |
| 218 | Ongoing monitoring by the KYC provider | A required re-verification never moves the date later | `OngoingMonitoringTest` | `aRequiredReverificationNeverMovesTheDateLater` |
| 219 | Ongoing monitoring by the KYC provider | An expiring document moves a team member's date sooner | `OngoingMonitoringTest` | `anExpiringDocumentMovesTheDateSooner` |
| 220 | Ongoing monitoring by the KYC provider | A serious flag lapses the business at once | `OngoingMonitoringTest` | `aSeriousFlagLapsesTheBusinessAtOnce` |
| 221 | Ongoing monitoring by the KYC provider | A pass after a flag clears it | `OngoingMonitoringTest` | `aPassAfterAFlagClearsIt` |
| 222 | A seriously flagged provider | An accepted reservation is disputed when the provider is flagged | `ReservationProviderFlagTest` | `anAcceptedReservationIsDisputedWhenTheProviderIsFlagged` |
| 223 | A seriously flagged provider | A flagged reservation is cancelled with a full refund and no fine | `ReservationProviderFlagTest` | `aFlaggedReservationIsCancelledWithAFullRefundAndNoFine` |
| 224 | A seriously flagged provider | A completed job is not disputed for a flag | `ReservationProviderFlagTest` | `aCompletedJobIsNotDisputedForAFlag` |
| 225 | A seriously flagged provider | A job under way stops when the provider is flagged | `ReservationProviderFlagTest` | `aJobUnderWayStopsWhenTheProviderIsFlagged` |
| 226 | A seriously flagged provider | A cleared flag restores the reservation before its start | `ReservationProviderFlagTest` | `aClearedFlagRestoresTheReservationBeforeItsStart` |
| 227 | A seriously flagged provider | A flagged reservation is not restored from its start | `ReservationProviderFlagTest` | `aFlaggedReservationIsNotRestoredFromItsStart` |
| 228 | A seriously flagged provider | An undecided flagged reservation is cancelled at its start | `ReservationProviderFlagTest` | `anUndecidedFlaggedReservationIsCancelledAtItsStart` |
| 229 | A seriously flagged provider's payouts | A flagged provider's payout is held | `PayoutHoldTest` | `aFlaggedProvidersPayoutIsHeld` |
| 230 | A seriously flagged provider's payouts | The founders release a held payout | `PayoutHoldTest` | `theFoundersReleaseAHeldPayout` |
| 231 | A seriously flagged provider's payouts | The founders withhold a held payout | `PayoutHoldTest` | `theFoundersWithholdAHeldPayout` |
| 232 | Retired jobs and tools | A retired job comes off the listing | `ProviderListingRetiredCatalogueTest` | `aRetiredJobComesOffTheListing` |
| 233 | Retired jobs and tools | A live listing left with no job goes offline | `ProviderListingRetiredCatalogueTest` | `aLiveListingLeftWithNoJobGoesOffline` |
| 234 | Retired jobs and tools | The listing goes live again once the provider offers another job | `ProviderListingRetiredCatalogueTest` | `theListingGoesLiveAgainOnceTheProviderOffersAnotherJob` |
| 235 | Retired jobs and tools | A request already made keeps the retired job | `ProviderListingRetiredCatalogueTest` | `aRequestAlreadyMadeKeepsTheRetiredJob` |
| 236 | Retired jobs and tools | A retired tool comes off the listing and it stays live | `ProviderListingRetiredCatalogueTest` | `aRetiredToolComesOffTheListingAndItStaysLive` |
| 237 | A payment that fails at request | A failed payment ends the request | `ReservationPaymentFailureTest` | `aFailedPaymentEndsTheRequest` |
| 238 | A payment that fails at request | The provider cannot accept a request whose payment failed | `ReservationPaymentFailureTest` | `theProviderCannotAcceptARequestWhosePaymentFailed` |
| 239 | A payment that fails at request | Only a request ends for a failed payment | `ReservationPaymentFailureTest` | `onlyARequestEndsForAFailedPayment` |
| 240 | A request is paid before the provider answers | The provider cannot accept a request before its payment is secured | `ReservationPaymentConfirmationTest` | `theProviderCannotAcceptARequestBeforeItsPaymentIsSecured` |
| 241 | A request is paid before the provider answers | The provider accepts once the payment is secured | `ReservationPaymentConfirmationTest` | `theProviderAcceptsOnceThePaymentIsSecured` |
| 242 | A request is paid before the provider answers | A payment below the total is not enough | `ReservationPaymentConfirmationTest` | `aPaymentBelowTheTotalIsNotEnough` |
| 243 | A request is paid before the provider answers | An edit that raises the total waits for payment again | `ReservationPaymentConfirmationTest` | `anEditThatRaisesTheTotalWaitsForPaymentAgain`, `theProviderAcceptsAnEditedRequestOnceTheNewTotalIsSecured` |
| 244 | A request is paid before the provider answers | An edit that lowers the total stays paid | `ReservationPaymentConfirmationTest` | `anEditThatLowersTheTotalStaysPaid` |

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
| policy | 3.0 | 100% | 100% | 59 | `PolicyArchitectureTest` |
| account | 5.0 | 100% | 100% | 191 | `AccountArchitectureTest` |
| offering | 5.0 | 100% | 100% | 209 | `OfferingArchitectureTest` |
| availability | 4.0 | 100% | 100% | 66 | `AvailabilityArchitectureTest` |
| reservation | 5.0 | 100% | 100% | 397 | `ReservationArchitectureTest` |
| payment | 5.0 | 100% | 100% | 143 | `PaymentArchitectureTest` |
| resolution | 3.0 | 100% | 100% | 40 | `ResolutionArchitectureTest` |
| conversation | 3.0 | 100% | 100% | 38 | `ConversationArchitectureTest` |
| discovery | 5.0 | 100% | 100% | 74 | `DiscoveryArchitectureTest` |
| feedback | 4.0 | 100% | 100% | 105 | `FeedbackArchitectureTest` |
| ie | 1.0 | n/a (exempt in the plan) | n/a | – | `IntegrationEventArchitectureTest` |

- **CRAP below 6:** passes in every context. The worst is 5.0, in account, offering, reservation,
  payment and discovery.
- **Mutation at least 90% for kernel and domain:** every context is at 100% score and test strength,
  1,439 of 1,439 mutants killed. The ratchet in `quality/ratchet.json` holds each context at 100%.
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
- **Later changes:**
  - `JobAddedIE`, `ToolAddedIE`, `RatingSubmittedIE` and `RatingHiddenIE` were added.
  - With team membership, `HelperVerifiedIE` became `HelperJoinedTeamIE`,
    `HelperVerificationRejectedIE` became `HelperInviteRejectedIE`, and `HelperInvitedIE` was added.
  - With re-verification, `RecheckOpenedIE`, `HelperReverifiedIE`, `HelperLapsedIE`,
    `ProviderReverifiedIE` and `ProviderVerificationLapsedIE` were added from the plan. Two more were
    added while building: `ListingPausedIE` and `ListingResumedIE`, so search can leave a paused
    listing out. Then `RecheckFailedIE`, so the provider is asked to have the person retry.
    `HomaPolicyChangedIE` now carries the three re-verification values.
  - For "verified for the whole job", `ProviderVerifiedIE` and `ProviderReverifiedIE` carry
    `reverificationDueAt`.
  - With provider types and adding a team member:
    - The three helper invite events gave way to `MemberVerificationStartedIE`,
      `MemberVerificationRejectedIE` and `TeamMemberAddedIE`. A `MemberVerificationExpiredIE` was
      added and then removed with the time limit for adding a team member.
    - The other helper events became team member events.
    - `ProviderRegisteredIE`, `BusinessRegistrationCheckedIE`, `FaceCheckRecordedIE` and
      `CheckInCompletedIE` were added.
    - The listing, policy and team-naming events carry roles, supervisor thresholds, the face check
      values and the accepted documents.
  - With business verification through eKYB, `BusinessRegistrationCheckedIE` was removed;
    `ProviderVerifiedIE` and `ProviderVerificationRejectedIE` cover eKYB too.
  - With verification by a KYC provider, `ProviderRegisteredIE` and `RecheckOpenedIE` were removed;
    `ProviderVerifiedIE` carries the SSM number and `verifiedUntil`, and the policy and listing events
    drop the verification values and the provider type.
  - With ongoing monitoring, `ReverificationRequiredIE`, `ProviderFlaggedIE` and `ReservationFlaggedIE`
    were added.
  - For a flagged provider, `ReservationRestoredIE`, `PayoutHeldIE`, `PayoutHoldReleasedIE` and
    `PayoutWithheldIE` were added.
  - While reviewing the usecase plan, `ServiceAreaPublishedIE`, `RequestCancelledIE`, `PaymentFailedIE`,
    `PaymentSecuredIE` and `SearchPerformedIE` were added, with a new event interface, `DiscoveryIE`.
  - The contract test now covers 62 events.
