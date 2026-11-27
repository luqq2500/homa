package com.homa.provider.usecase.port.in;

import com.homa.provider.usecase.dto.OpenProviderRequest;
import com.homa.provider.usecase.dto.OpenProviderResponse;

/** A verified person opens a provider account as its owner (PRV-1). */
public interface ProviderOpener {

    OpenProviderResponse open(OpenProviderRequest request);
}
