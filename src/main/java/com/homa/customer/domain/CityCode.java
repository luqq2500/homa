package com.homa.customer.domain;

/** The city of a home address, as a Homa city code such as "SALAK_TINGGI" (CUS-1, RES-1). */
public record CityCode(String value) {

    public CityCode {
        Required.text(value, "a city in the home address");
    }
}
