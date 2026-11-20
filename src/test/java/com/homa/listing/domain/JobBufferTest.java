package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.JOB_BUFFER;
import static com.homa.listing.domain.ListingFixtures.kemasHomeListing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.listing.domain.exception.InvalidJobBufferException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-10")
class JobBufferTest {

    @Test
    @DisplayName("Kemas Home keeps the 30-minute default. A provider covering a wider area sets 60 minutes.")
    void aProviderKeepsTheThirtyMinuteDefaultOrSetsItsOwnBuffer() {
        ProviderListing kemasHome = kemasHomeListing();
        ProviderListing widerArea = kemasHomeListing();

        widerArea.setJobBuffer(JobBuffer.of(Duration.ofMinutes(60)));

        assertThat(kemasHome.terms().buffer()).isEqualTo(JobBuffer.DEFAULT);
        assertThat(JobBuffer.DEFAULT.duration()).isEqualTo(JOB_BUFFER);
        assertThat(widerArea.terms().buffer().duration()).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    @DisplayName("A buffer can be zero but can't be negative")
    void aBufferCantBeNegative() {
        assertThat(JobBuffer.of(Duration.ZERO).duration()).isZero();
        assertThatThrownBy(() -> JobBuffer.of(Duration.ofMinutes(-1))).isInstanceOf(InvalidJobBufferException.class);
        assertThatThrownBy(() -> JobBuffer.of(null)).isInstanceOf(NullPointerException.class);
    }
}
