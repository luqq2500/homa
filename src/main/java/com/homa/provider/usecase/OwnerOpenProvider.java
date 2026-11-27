package com.homa.provider.usecase;

import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.usecase.dto.OpenProviderRequest;
import com.homa.provider.usecase.dto.OpenProviderResponse;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.port.in.ProviderOpener;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import com.homa.provider.usecase.port.out.VerifiedPersonReplica;
import java.util.Objects;

/**
 * A verified person opens a provider account as its owner (PRV-1), from what Verification decided about them
 * ({@code PersonVerifiedIE}): the domain refuses an owner who isn't local or didn't pass the age check. Opening
 * publishes nothing; the provider is announced when it goes live.
 *
 * <p>Hooks for later slices, both before {@link Provider#open}: slice 4 looks up the person's {@code Affiliation} and
 * passes it in, so a person who already owns or belongs to a provider is refused (HLP-10); slice 6 does the same with
 * the {@code RegistrationDebt} replica for the owner's personId and business (PAY-9, PAY-11).
 */
public final class OwnerOpenProvider implements ProviderOpener {

    private final VerifiedPersonReplica verifiedPeople;
    private final ProviderRepository providers;

    public OwnerOpenProvider(VerifiedPersonReplica verifiedPeople, ProviderRepository providers) {
        this.verifiedPeople = Objects.requireNonNull(verifiedPeople, "verifiedPeople");
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    @Override
    public OpenProviderResponse open(OpenProviderRequest request) {
        PersonId owner = new PersonId(request.actingPersonId());
        VerifiedPerson person = verifiedPeople.findByPersonId(owner)
                .orElseThrow(() -> new PersonNotVerifiedException(owner.value()));
        Provider provider = Provider.open(ProviderId.generate(), person.facts());
        providers.save(provider);
        return new OpenProviderResponse(
                provider.id().value(), provider.owner().personId().value(), provider.status().name());
    }
}
