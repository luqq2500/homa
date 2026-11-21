package com.homa.customer.domain;

/** The name a customer registers with and goes by on Homa, such as "Aisyah" (CUS-1). */
public record CustomerName(String value) {

    public CustomerName {
        Required.text(value, "a name");
    }
}
