package com.homa.listing.usecase;

import com.homa.listing.usecase.port.out.ProviderNotifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class RecordingProviderNotifier implements ProviderNotifier {

    record NoticeRaised(String providerId, Duration minimumNotice) {
    }

    private final List<NoticeRaised> told = new ArrayList<>();

    @Override
    public void noticeRaised(String providerId, Duration minimumNotice) {
        told.add(new NoticeRaised(providerId, minimumNotice));
    }

    List<NoticeRaised> told() {
        return List.copyOf(told);
    }
}
