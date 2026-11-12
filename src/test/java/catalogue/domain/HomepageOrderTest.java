package catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.InvalidShelfOrderException;
import catalogue.domain.exception.ShelfAlreadyOnHomepageException;
import catalogue.domain.exception.ShelfNotOnHomepageException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: The order of the shelves on the homepage. Positions run from 1 with no gaps and no two shelves in one place. */
class HomepageOrderTest {

    /** A homepage with the shelves "a", "b" and "c", in that order. */
    private static Homepage abc() {
        Homepage homepage = Homepage.create();
        homepage.place("a", 1);
        homepage.place("b", 2);
        homepage.place("c", 3);
        return homepage;
    }

    @Test
    void aNewHomepageHasNoShelves() {
        Homepage homepage = Homepage.create();

        assertThat(homepage.shelfIds()).isEmpty();
        assertThat(homepage.id()).isNotBlank().isNotEqualTo(Homepage.create().id());
    }

    @Nested
    class Placing {

        @Test
        void aShelfPlacedAtTheEndGoesLast() {
            assertThat(abc().shelfIds()).containsExactly("a", "b", "c");
        }

        @Test
        void aShelfPlacedInTheMiddleMovesTheRestDownOne() {
            Homepage homepage = abc();

            homepage.place("new", 2);

            assertThat(homepage.shelfIds()).containsExactly("a", "new", "b", "c");
            assertThat(homepage.positionOf("b")).isEqualTo(3);
        }

        @Test
        void aShelfPlacedFirstMovesEveryOtherShelfDown() {
            Homepage homepage = abc();

            homepage.place("new", 1);

            assertThat(homepage.shelfIds()).containsExactly("new", "a", "b", "c");
        }

        @ParameterizedTest(name = "position {0} is outside 1 to 4")
        @ValueSource(ints = {0, -1, 5})
        void aShelfIsPlacedFromTheFirstPlaceToOnePastTheLast(int position) {
            Homepage homepage = abc();

            assertThatThrownBy(() -> homepage.place("new", position))
                    .isInstanceOf(InvalidShelfOrderException.class)
                    .hasMessage("A shelf's place is from 1 to 4, was " + position);
            assertThat(homepage.shelfIds()).containsExactly("a", "b", "c");
        }

        @Test
        void aShelfHasOnlyOnePlace() {
            Homepage homepage = abc();

            assertThatThrownBy(() -> homepage.place("b", 1))
                    .isInstanceOf(ShelfAlreadyOnHomepageException.class)
                    .hasMessage("Shelf b is already on the homepage");
            assertThat(homepage.shelfIds()).containsExactly("a", "b", "c");
        }

        @Test
        void aShelfNeedsAnId() {
            assertThatThrownBy(() -> Homepage.create().place(null, 1)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Moving {

        @Test
        void aShelfMovedUpMovesTheShelvesInBetweenDown() {
            Homepage homepage = abc();

            boolean moved = homepage.move("c", 1);

            assertThat(moved).isTrue();
            assertThat(homepage.shelfIds()).containsExactly("c", "a", "b");
        }

        @Test
        void aShelfMovedDownMovesTheShelvesInBetweenUp() {
            Homepage homepage = abc();

            boolean moved = homepage.move("a", 3);

            assertThat(moved).isTrue();
            assertThat(homepage.shelfIds()).containsExactly("b", "c", "a");
        }

        @Test
        void movingAShelfToItsOwnPlaceChangesNothing() {
            Homepage homepage = abc();

            assertThat(homepage.move("b", 2)).isFalse();
            assertThat(homepage.shelfIds()).containsExactly("a", "b", "c");
        }

        @ParameterizedTest(name = "position {0} is outside 1 to 3")
        @ValueSource(ints = {0, 4})
        void aShelfMovesOnlyBetweenTheFirstAndTheLastPlace(int position) {
            Homepage homepage = abc();

            assertThatThrownBy(() -> homepage.move("a", position))
                    .isInstanceOf(InvalidShelfOrderException.class)
                    .hasMessage("A shelf's place is from 1 to 3, was " + position);
            assertThat(homepage.shelfIds()).containsExactly("a", "b", "c");
        }

        @Test
        void aShelfMovesToTheLastPlace() {
            Homepage homepage = abc();

            homepage.move("b", 3);

            assertThat(homepage.shelfIds()).containsExactly("a", "c", "b");
        }

        @Test
        void onlyAShelfOnTheHomepageMoves() {
            assertThatThrownBy(() -> abc().move("z", 1))
                    .isInstanceOf(ShelfNotOnHomepageException.class)
                    .hasMessage("Shelf z is not on the homepage");
        }
    }

    @Nested
    class Removing {

        @Test
        void aRemovedShelfLeavesNoGap() {
            Homepage homepage = abc();

            homepage.remove("a");

            assertThat(homepage.shelfIds()).containsExactly("b", "c");
            assertThat(homepage.positionOf("b")).isEqualTo(1);
        }

        @Test
        void onlyAShelfOnTheHomepageIsRemoved() {
            Homepage homepage = abc();

            assertThatThrownBy(() -> homepage.remove("z")).isInstanceOf(ShelfNotOnHomepageException.class);
            assertThat(homepage.shelfIds()).containsExactly("a", "b", "c");
        }
    }

    @Test
    void theOrderShownCannotBeChangedFromOutside() {
        Homepage homepage = abc();

        assertThatThrownBy(() -> homepage.shelfIds().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aShelfsPositionStartsAtOne() {
        assertThat(abc().positionOf("a")).isEqualTo(1);
        assertThat(abc().positionOf("c")).isEqualTo(3);
    }
}
