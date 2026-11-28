package com.homa.rating.domain.exception;

import com.homa.rating.domain.IncidentKind;
import com.homa.rating.domain.Party;

/**
 * RTG-10: reliability shows a provider's cancellations, no-rosters, no-shows and members who left, and a customer's
 * fee-paying cancellations and no-shows. A customer has no no-rosters and no members who left.
 */
public class IncidentNotCountedException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public IncidentNotCountedException(Party party, IncidentKind kind) {
        super("RTG-10: reliability doesn't count " + kind + " for " + party.role() + " " + party.id());
    }
}
