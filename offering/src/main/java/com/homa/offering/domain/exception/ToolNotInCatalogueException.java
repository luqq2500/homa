package com.homa.offering.domain.exception;

/** A provider can only bring tools from Homa's tool list. */
public class ToolNotInCatalogueException extends OfferingRuleViolationException {

    public ToolNotInCatalogueException(Object tool) {
        super("Tool " + tool + " is not an active tool on Homa's tool list");
    }
}
