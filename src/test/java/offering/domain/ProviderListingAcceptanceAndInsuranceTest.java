package offering.domain;

import static offering.domain.OfferingBuilders.aLiveListing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import offering.domain.exception.InsuranceExpiredException;
import offering.domain.exception.OfferingDetailRequiredException;
import org.junit.jupiter.api.Test;

/** Features: Instant book, and the Insured badge. */
class ProviderListingAcceptanceAndInsuranceTest {

    private static final Instant FEBRUARY_28 = Instant.parse("2027-02-28T02:00:00Z");
    private static final Instant MARCH_1 = Instant.parse("2027-03-01T00:00:00Z");

    @Test
    void aListingAcceptsByRequestUnlessTheProviderChoosesInstantBook() {
        ProviderListing listing = ProviderListing.draft("provider-1");

        assertThat(listing.acceptancePolicy()).isEqualTo(AcceptancePolicy.REQUEST_AND_ACCEPT);
        assertThat(aLiveListing().snapshot().acceptancePolicy()).isEqualTo(AcceptancePolicy.REQUEST_AND_ACCEPT);
    }

    @Test
    void theSnapshotCarriesInstantBook() {
        ProviderListing listing = aLiveListing();

        listing.chooseAcceptancePolicy(AcceptancePolicy.INSTANT_BOOK);

        assertThat(listing.acceptancePolicy()).isEqualTo(AcceptancePolicy.INSTANT_BOOK);
        assertThat(listing.snapshot().acceptancePolicy()).isEqualTo(AcceptancePolicy.INSTANT_BOOK);
        assertThatThrownBy(() -> listing.chooseAcceptancePolicy(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void declaredInsuranceShowsUntilItExpires() {
        ProviderListing listing = aLiveListing();

        listing.declareInsurance(InsuranceDeclaration.of(" Etiqa ", MARCH_1), FEBRUARY_28);

        assertThat(listing.isInsured(FEBRUARY_28)).isTrue();
        assertThat(listing.isInsured(MARCH_1.minusSeconds(1))).isTrue();
        assertThat(listing.isInsured(MARCH_1)).isFalse();
        assertThat(listing.insurance()).contains(new InsuranceDeclaration("Etiqa", MARCH_1));
    }

    @Test
    void insuranceThatHasAlreadyExpiredCannotBeDeclared() {
        ProviderListing listing = aLiveListing();

        assertThatThrownBy(() -> listing.declareInsurance(InsuranceDeclaration.of("Etiqa", MARCH_1), MARCH_1))
                .isInstanceOf(InsuranceExpiredException.class)
                .hasMessageContaining(MARCH_1.toString());
        assertThat(listing.insurance()).isEmpty();
        assertThat(listing.isInsured(FEBRUARY_28)).isFalse();
    }

    @Test
    void clearingInsuranceRemovesTheBadge() {
        ProviderListing listing = aLiveListing();
        listing.declareInsurance(InsuranceDeclaration.of("Etiqa", MARCH_1), FEBRUARY_28);

        listing.clearInsurance();

        assertThat(listing.insurance()).isEmpty();
        assertThat(listing.isInsured(FEBRUARY_28)).isFalse();
    }

    @Test
    void aDeclarationNamesTheInsurerAndTheDate() {
        assertThatThrownBy(() -> InsuranceDeclaration.of(" ", MARCH_1)).isInstanceOf(OfferingDetailRequiredException.class);
        assertThatThrownBy(() -> InsuranceDeclaration.of("Etiqa", null)).isInstanceOf(NullPointerException.class);
    }
}
