# 0004. Google sign-in; Verification saves a pass in one transaction

- Status: accepted
- Date: 2026-11-27
- Decided by: the product owner

## Sign-in provider: Google

Customers, owners, members and Support sign in through **Google** (rules CUS-7: one identity-provider account holds one
customer account). The plan's "Identity: buy, Cognito or similar" is settled as Google sign-in.

- A customer's `SignInId` is Google's `iss` and `sub` claims. `sub` is Google's stable account id, never the email,
  which can change.
- `infra/security` in each context verifies the Google ID token (signature, issuer, audience, expiry) and maps its
  claims to a `SignInId`, a personId or customerId, and a role. Roles beyond "signed-in person" (owner, member,
  Support, admin) are assigned by Homa from its own data, never read from the token.
- Apple and others stay possible later: CUS-7 says "an identity provider, such as Google or Apple", so only
  `infra/security` changes.
- EXT-6 stays as written: if Google is unavailable, nobody can sign in or register, and reservations carry on.
- The REST controllers and `infra/security` are the work after slice 1's persistence and events.

## Verification saves an attempt and a verified person in one transaction

When a verification passes, `RecordVerificationResult` saves the `VerificationAttempt` and the `VerifiedPerson` together.
This is a second exception to "one transaction changes one aggregate", beside checkout's three aggregates.
Accepted because the attempt carries no events, so event ordering is unaffected, and the two must agree: an attempt
marked passed without its person (or the reverse) would leave a customer unable to reserve with no way to retry.
