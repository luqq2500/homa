package account.domain;

import account.domain.exception.DocumentNotAcceptedException;

/**
 * The document rules for team members: the document must be on the accepted list for the provider's type,
 * and an independent provider's team members are always Malaysian, whatever the list says.
 */
final class DocumentRules {

    private DocumentRules() {
    }

    static void requireForTeamMember(ProviderType providerType, DocumentType type, Country country, AcceptedDocuments accepted) {
        DocumentHolder holder = DocumentHolder.teamMemberOf(providerType);
        if (providerType == ProviderType.INDEPENDENT && !Country.MALAYSIA.equals(country)) {
            throw new DocumentNotAcceptedException(holder, type, country);
        }
        accepted.require(holder, type, country);
    }
}
