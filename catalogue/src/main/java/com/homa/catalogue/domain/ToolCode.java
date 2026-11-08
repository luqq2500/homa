package com.homa.catalogue.domain;

/** Identifies a tool on Homa's tool list, such as {@code STEAM_MOP}. */
public record ToolCode(String value) {

    public ToolCode {
        CatalogueText.requireCode(value);
    }

    public static ToolCode of(String value) {
        return new ToolCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
