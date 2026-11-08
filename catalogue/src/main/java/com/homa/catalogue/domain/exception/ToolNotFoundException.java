package com.homa.catalogue.domain.exception;

/** No tool with this code is on the tool list. */
public class ToolNotFoundException extends CatalogueRuleViolationException {

    public ToolNotFoundException(String code) {
        super("The tool list has no tool with code " + code);
    }
}
