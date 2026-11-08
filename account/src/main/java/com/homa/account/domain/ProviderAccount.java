package com.homa.account.domain;

/**
 * An individual provider. A provider verifies through eKYC and adds a payout account before going live.
 * By default the provider is their own helper; a provider who runs a team can choose not to be.
 */
public final class ProviderAccount {

    private final String id;
    private final PhoneNumber phone;
    private final String legalName;
    private VerificationStatus ekyc = VerificationStatus.PENDING;
    private boolean payoutAccountReady;
    private boolean includesSelfAsHelper = true;

    private ProviderAccount(String id, PhoneNumber phone, String legalName) {
        this.id = id;
        this.phone = phone;
        this.legalName = legalName;
    }

    public static ProviderAccount register(PhoneVerification verified, String legalName) {
        verified.requireConfirmed();
        return new ProviderAccount(
                new ProviderId().generate(), verified.phone(), AccountText.require(legalName, "A provider's legal name"));
    }

    /** Records Kenal's result. A provider rejected before can be verified on a later attempt. */
    public void recordEkycResult(VerificationStatus result) {
        ekyc = EkycResult.require(result);
    }

    /** Records that Stripe reports the provider's payout account as ready. */
    public void recordPayoutAccountReady() {
        payoutAccountReady = true;
    }

    public void includeSelfAsHelper(boolean include) {
        includesSelfAsHelper = include;
    }

    /** The provider's own part of going live: their eKYC is verified and their payout account is ready. */
    public boolean isReadyForGoLive() {
        return ekyc == VerificationStatus.VERIFIED && payoutAccountReady;
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public String legalName() {
        return legalName;
    }

    public VerificationStatus ekycStatus() {
        return ekyc;
    }

    public boolean isPayoutAccountReady() {
        return payoutAccountReady;
    }

    public boolean includesSelfAsHelper() {
        return includesSelfAsHelper;
    }
}
