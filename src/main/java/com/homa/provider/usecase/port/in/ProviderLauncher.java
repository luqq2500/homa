package com.homa.provider.usecase.port.in;

import com.homa.provider.usecase.dto.GoLiveRequest;
import com.homa.provider.usecase.dto.GoLiveResponse;

/** The owner takes the provider live with a verified payout account and at least one active helper (PRV-2, PRV-3). */
public interface ProviderLauncher {

    GoLiveResponse goLive(GoLiveRequest request);
}
