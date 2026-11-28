package com.homa.rating.domain;

import com.homa.rating.domain.exception.MediaNotAllowedException;
import com.homa.rating.domain.exception.RatingCategoryNotForAuthorException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What one party said about the other after a Completed job (RTG-1): 1 to 5 stars, and optionally a description and
 * stars on the categories of the author's side (RTG-4). The categories are the author's own (RTG-7, RTG-8), and only
 * a customer's rating can include media (RTG-5): a provider's rating of a customer is text only. A rating never
 * changes once written; Support can remove it (RTG-11).
 */
public final class Rating {

    private final RatingId id;
    private final Party author;
    private final Stars stars;
    private final String description;
    private final Map<RatingCategory, Stars> categoryStars;
    private final List<String> mediaRefs;
    private final Instant submittedAt;

    private Rating(RatingId id, Party author, Stars stars, String description,
            Map<RatingCategory, Stars> categoryStars, List<String> mediaRefs, Instant submittedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.author = Objects.requireNonNull(author, "author");
        this.stars = Objects.requireNonNull(stars, "stars");
        this.description = description == null || description.isBlank() ? null : description;
        EnumMap<RatingCategory, Stars> given = new EnumMap<>(RatingCategory.class);
        given.putAll(categoryStars);
        this.categoryStars = Collections.unmodifiableMap(given);
        this.mediaRefs = List.copyOf(new ArrayList<>(mediaRefs));
        this.mediaRefs.forEach(ref -> Ids.requireText(ref, "Media reference"));
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
    }

    /**
     * A new rating by {@code author}, with the category stars the author's side can give (RTG-7, RTG-8) and media
     * only when the author is a customer (RTG-5). A blank description is no description.
     */
    public static Rating by(RatingId id, Party author, Stars stars, String description,
            Map<RatingCategory, Stars> categoryStars, List<String> mediaRefs, Instant submittedAt) {
        Objects.requireNonNull(author, "author");
        for (RatingCategory category : categoryStars.keySet()) {
            if (category.ratedBy() != author.role()) {
                throw new RatingCategoryNotForAuthorException(category, author.role());
            }
        }
        if (author.role() != PartyRole.CUSTOMER && !mediaRefs.isEmpty()) {
            throw new MediaNotAllowedException(author.role());
        }
        return new Rating(id, author, stars, description, categoryStars, mediaRefs, submittedAt);
    }

    /**
     * Rebuilds a rating from what was stored. Only the shape is checked: the categories are the author's side's and
     * only a customer's rating has media. Nothing else is decided again.
     */
    public static Rating restore(RatingId id, Party author, Stars stars, String description,
            Map<RatingCategory, Stars> categoryStars, List<String> mediaRefs, Instant submittedAt) {
        Objects.requireNonNull(author, "author");
        boolean ownCategories = categoryStars.keySet().stream().allMatch(c -> c.ratedBy() == author.role());
        if (!ownCategories) {
            throw new IllegalArgumentException("A stored rating needs the categories of its author's side");
        }
        if (author.role() != PartyRole.CUSTOMER && !mediaRefs.isEmpty()) {
            throw new IllegalArgumentException("A stored rating needs no media when its author is not a customer");
        }
        return new Rating(id, author, stars, description, categoryStars, mediaRefs, submittedAt);
    }

    public RatingId id() {
        return id;
    }

    /** Who wrote it: the customer rating the provider, or the provider rating the customer (RTG-1). */
    public Party author() {
        return author;
    }

    /** The overall stars, 1 to 5 (RTG-4). */
    public Stars stars() {
        return stars;
    }

    /** The optional description (RTG-4). */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /** The optional stars on the author's categories (RTG-4, RTG-7, RTG-8). */
    public Map<RatingCategory, Stars> categoryStars() {
        return categoryStars;
    }

    /** References to the media a customer added (RTG-5); always empty for a provider's rating. */
    public List<String> mediaRefs() {
        return mediaRefs;
    }

    public Instant submittedAt() {
        return submittedAt;
    }
}
