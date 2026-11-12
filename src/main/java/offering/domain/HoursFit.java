package offering.domain;

/** What fitting a listing's hours into Homa's duration range did, so the provider can be told. */
public enum HoursFit {
    /** No hours were set, or they were already inside Homa's range; nothing changed. */
    NOT_AFFECTED,
    /** The hours shrank to the part inside Homa's range; the listing is as live, paused or draft as it was. */
    NARROWED,
    /** None of the hours were inside Homa's range, so they were cleared; the listing needs them again. */
    CLEARED,
    /** The same, for a live listing, which is no longer live until the provider sets hours and goes live again. */
    CLEARED_AND_OFFLINE
}
