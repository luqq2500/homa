# 0003. Product owner answers, 21–22 November 2026

Status: accepted. These answer open questions that the rules page (v1.6) and the development plan don't yet state.
Each belongs on the Notion rules page; until it is there, this record is the source.

| Question | Answer | Where it lands |
| --- | --- | --- |
| Can a business that fails verification retry? | Yes. A failed business verification can be resubmitted, with corrected details, and gets a new result. | PRV-7 and MON-5: `BusinessVerification` in the Verification context (slice 6). Until then Provider holds no business registration (rules v1.1 moved it out). |
| AD: who declares when a verification-service outage starts and ends (EXT-5, MON-1, MON-2)? | The plan's default: record it automatically from failed calls and the service's status endpoint; Support can correct an interval before a suspension runs. | Verification's outage log (slice 10). |
| AG: how fast must Support answer a data request (DAT-12, §14)? | The plan's default: within 21 days, as Malaysia's Personal Data Protection Act requires for access and correction; held as a §14 country-policy item. | Support's `DataRequest` and Verification's `CountryPolicy` (slice 9). |
| AC: retries before a failed transfer goes to Support (EXT-3)? | Settled by the rules (v1.6): one retry, then Support. | Payment's `RetryFailedTransfers` (slice 10). |
| How long are delivered outbox rows and processed-message keys kept? | Settled by the plan: 30 days, by an infra job. | `TechnicalRowPurge` in `app/outbox` (done). |
