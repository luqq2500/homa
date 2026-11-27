package com.homa.listing.usecase;

import com.homa.listing.domain.City;
import com.homa.listing.domain.JobType;
import com.homa.listing.domain.Offerings;
import com.homa.listing.domain.Tool;
import java.util.List;
import java.util.stream.Collectors;

/** A request's job, tool and city codes as the domain's {@link Offerings} (OFR-1). */
final class OfferingCodes {

    private OfferingCodes() {
    }

    static Offerings offerings(List<String> jobCodes, List<String> toolCodes, List<String> cityCodes) {
        return new Offerings(
                jobCodes.stream().map(JobType::new).collect(Collectors.toSet()),
                toolCodes.stream().map(Tool::new).collect(Collectors.toSet()),
                cityCodes.stream().map(City::new).collect(Collectors.toSet()));
    }
}
