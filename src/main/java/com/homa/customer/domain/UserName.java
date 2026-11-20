package com.homa.customer.domain;

/** The name a customer goes by on Homa, such as "Aisyah" (CUS-1). */
public record UserName(String value) {

    public UserName {
        Required.text(value, "a user name");
    }
}
