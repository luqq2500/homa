package com.homa.catalogue.domain;

/** A display group, such as Kitchen or Living room, so jobs are easy to scan on the provider page. */
public record JobGroup(String name) {

    public JobGroup {
        name = CatalogueText.requireName(name, "A job group");
    }

    public static JobGroup of(String name) {
        return new JobGroup(name);
    }
}
