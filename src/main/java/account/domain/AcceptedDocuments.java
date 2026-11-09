package account.domain;

import account.domain.exception.DocumentNotAcceptedException;
import java.util.Set;

/** account's view of the Homa-set accepted documents list. */
public record AcceptedDocuments(Set<AcceptedDocument> documents) {

    public AcceptedDocuments {
        documents = Set.copyOf(documents);
    }

    public boolean accepts(DocumentHolder holder, DocumentType type, Country country) {
        return documents.stream().anyMatch(document -> document.accepts(holder, type, country));
    }

    void require(DocumentHolder holder, DocumentType type, Country country) {
        if (!accepts(holder, type, country)) {
            throw new DocumentNotAcceptedException(holder, type, country);
        }
    }
}
