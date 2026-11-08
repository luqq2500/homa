package reservation.domain;

/** How the customer pays: a card, authorised at request and charged on acceptance, or FPX, paid upfront. */
public enum PaymentMethod {
    CARD,
    FPX
}
