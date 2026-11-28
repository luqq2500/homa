package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.AISYAH_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.rating.domain.exception.InvalidStarsException;
import com.homa.rating.domain.exception.MediaNotAllowedException;
import com.homa.rating.domain.exception.RatingCategoryNotForAuthorException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingTest {

    private static final RatingId ID = new RatingId("rating-1");

    private static Rating byAisyah(String description, Map<RatingCategory, Stars> categories, List<String> media) {
        return Rating.by(ID, AISYAH_PARTY, new Stars(4), description, categories, media, AISYAH_RATES_AT);
    }

    private static Rating byKemasHome(String description, Map<RatingCategory, Stars> categories, List<String> media) {
        return Rating.by(ID, KEMAS_HOME_PARTY, new Stars(4), description, categories, media, AISYAH_RATES_AT);
    }

    @Test
    @Tag("RTG-4")
    @DisplayName("Aisyah gives 4 stars with no text: a rating needs only its stars.")
    void aRatingNeedsOnlyItsStars() {
        Rating rating = byAisyah(null, Map.of(), List.of());

        assertThat(rating.id()).isEqualTo(ID);
        assertThat(rating.author()).isEqualTo(AISYAH_PARTY);
        assertThat(rating.stars()).isEqualTo(new Stars(4));
        assertThat(rating.description()).isEmpty();
        assertThat(rating.categoryStars()).isEmpty();
        assertThat(rating.mediaRefs()).isEmpty();
        assertThat(rating.submittedAt()).isEqualTo(AISYAH_RATES_AT);
    }

    @Test
    @Tag("RTG-4")
    @DisplayName("A rating has 1 to 5 stars: 0 and 6 are refused, 1 and 5 are fine.")
    void starsRunFromOneToFive() {
        assertThat(new Stars(1).value()).isEqualTo(1);
        assertThat(new Stars(5).value()).isEqualTo(5);
        assertThatThrownBy(() -> new Stars(0)).isInstanceOf(InvalidStarsException.class)
                .hasMessageContaining("RTG-4").hasMessageContaining("0 stars");
        assertThatThrownBy(() -> new Stars(6)).isInstanceOf(InvalidStarsException.class)
                .hasMessageContaining("6 stars");
    }

    @Test
    @Tag("RTG-4")
    @DisplayName("A description is optional: a blank one is no description.")
    void aDescriptionIsOptional() {
        assertThat(byAisyah("Kitchen spotless, thank you.", Map.of(), List.of()).description())
                .contains("Kitchen spotless, thank you.");
        assertThat(byAisyah("   ", Map.of(), List.of()).description()).isEmpty();
        assertThat(byAisyah("", Map.of(), List.of()).description()).isEmpty();
    }

    @Test
    @Tag("RTG-7")
    @DisplayName("Aisyah gives Quality of work 5, Punctuality 4 and Professionalism 5.")
    void customersRateProvidersOnTheirThreeCategories() {
        Map<RatingCategory, Stars> categories = Map.of(RatingCategory.QUALITY_OF_WORK, new Stars(5),
                RatingCategory.PUNCTUALITY, new Stars(4), RatingCategory.PROFESSIONALISM, new Stars(5));

        Rating rating = byAisyah(null, categories, List.of());

        assertThat(rating.categoryStars()).containsExactlyInAnyOrderEntriesOf(categories);
        assertThat(RatingCategory.of(PartyRole.CUSTOMER)).containsExactly(RatingCategory.QUALITY_OF_WORK,
                RatingCategory.PUNCTUALITY, RatingCategory.PROFESSIONALISM);
    }

    @Test
    @Tag("RTG-7")
    @DisplayName("Aisyah can't give stars on Access and readiness: that is a provider's category.")
    void customersCantUseTheProvidersCategories() {
        assertThatThrownBy(() -> byAisyah(null, Map.of(RatingCategory.ACCESS_AND_READINESS, new Stars(3)), List.of()))
                .isInstanceOf(RatingCategoryNotForAuthorException.class)
                .hasMessageContaining("RTG-7")
                .hasMessageContaining("ACCESS_AND_READINESS")
                .hasMessageContaining("PROVIDER")
                .hasMessageContaining("CUSTOMER");
    }

    @Test
    @Tag("RTG-8")
    @DisplayName("Kemas Home gives Access and readiness 3, because the guardhouse code was missing.")
    void providersRateCustomersOnTheirThreeCategories() {
        Map<RatingCategory, Stars> categories = Map.of(RatingCategory.ACCURATE_REQUEST, new Stars(5),
                RatingCategory.ACCESS_AND_READINESS, new Stars(3), RatingCategory.RESPECT, new Stars(5));

        Rating rating = byKemasHome("Guardhouse code was missing.", categories, List.of());

        assertThat(rating.categoryStars()).containsExactlyInAnyOrderEntriesOf(categories);
        assertThat(RatingCategory.of(PartyRole.PROVIDER)).containsExactly(RatingCategory.ACCURATE_REQUEST,
                RatingCategory.ACCESS_AND_READINESS, RatingCategory.RESPECT);
    }

    @Test
    @Tag("RTG-8")
    @DisplayName("Kemas Home can't give stars on Punctuality: that is a customer's category.")
    void providersCantUseTheCustomersCategories() {
        assertThatThrownBy(() -> byKemasHome(null, Map.of(RatingCategory.PUNCTUALITY, new Stars(4)), List.of()))
                .isInstanceOf(RatingCategoryNotForAuthorException.class)
                .hasMessageContaining("RTG-8");
    }

    @Test
    @Tag("RTG-5")
    @DisplayName("Aisyah adds a photo of her kitchen to her rating.")
    void aCustomersRatingCanIncludeMedia() {
        Rating rating = byAisyah(null, Map.of(), List.of("photo-kitchen", "photo-vase"));

        assertThat(rating.mediaRefs()).containsExactly("photo-kitchen", "photo-vase");
    }

    @Test
    @Tag("RTG-5")
    @DisplayName("Kemas Home can't add photos of her home: a provider's rating of a customer is text only.")
    void aProvidersRatingIsTextOnly() {
        assertThatThrownBy(() -> byKemasHome("Lovely home", Map.of(), List.of("photo-kitchen")))
                .isInstanceOf(MediaNotAllowedException.class)
                .hasMessageContaining("RTG-5");
        assertThat(byKemasHome("Lovely home", Map.of(), List.of()).mediaRefs()).isEmpty();
    }

    @Test
    @Tag("RTG-5")
    @DisplayName("A media reference is never blank.")
    void mediaReferencesAreNeverBlank() {
        assertThatThrownBy(() -> byAisyah(null, Map.of(), List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Tag("RTG-4")
    @DisplayName("A rating can't change once written: its lists and maps are frozen.")
    void aRatingCantChangeOnceWritten() {
        Rating rating = byAisyah(null, Map.of(RatingCategory.PUNCTUALITY, new Stars(4)), List.of("photo-kitchen"));

        assertThatThrownBy(() -> rating.mediaRefs().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rating.categoryStars().put(RatingCategory.RESPECT, new Stars(1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @Tag("RTG-4")
    @DisplayName("A rating has an id, an author, stars and a time.")
    void aRatingNeedsItsParts() {
        assertThatThrownBy(() -> Rating.by(null, AISYAH_PARTY, new Stars(4), null, Map.of(), List.of(), AISYAH_RATES_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Rating.by(ID, null, new Stars(4), null, Map.of(), List.of(), AISYAH_RATES_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Rating.by(ID, AISYAH_PARTY, null, null, Map.of(), List.of(), AISYAH_RATES_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Rating.by(ID, AISYAH_PARTY, new Stars(4), null, Map.of(), List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("RTG-5")
    @DisplayName("A stored rating comes back as it was written, and a stored shape that breaks RTG-5, RTG-7 or RTG-8 "
            + "is refused.")
    void aStoredRatingComesBackAsWritten() {
        Map<RatingCategory, Stars> categories = Map.of(RatingCategory.PUNCTUALITY, new Stars(4));
        Rating restored = Rating.restore(ID, AISYAH_PARTY, new Stars(4), "Good", categories, List.of("photo-kitchen"),
                AISYAH_RATES_AT);

        assertThat(restored.id()).isEqualTo(ID);
        assertThat(restored.description()).contains("Good");
        assertThat(restored.categoryStars()).isEqualTo(categories);
        assertThat(restored.mediaRefs()).containsExactly("photo-kitchen");
        assertThat(Rating.restore(ID, KEMAS_HOME_PARTY, new Stars(3), null, Map.of(RatingCategory.RESPECT, new Stars(3)),
                List.of(), AISYAH_RATES_AT).author()).isEqualTo(KEMAS_HOME_PARTY);
        assertThatThrownBy(() -> Rating.restore(ID, AISYAH_PARTY, new Stars(4), null,
                Map.of(RatingCategory.RESPECT, new Stars(4)), List.of(), AISYAH_RATES_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("categories");
        assertThatThrownBy(() -> Rating.restore(ID, KEMAS_HOME_PARTY, new Stars(4), null, Map.of(),
                List.of("photo"), AISYAH_RATES_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("media");
    }
}
