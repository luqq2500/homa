package com.homa.verification.domain;

/**
 * The role a person is verified for. Each role names the rule that sets who may hold it, and whether its holder
 * re-verifies every cycle (MON-1 covers owners and members; a customer verifies once, CUS-3).
 */
public enum PersonRole {
    /** CUS-10: aged 18 or above, with a status the country policy allows for customers. */
    CUSTOMER("CUS-10", "a customer", false),
    /** PRV-1: aged 18 or above, with a status the country policy allows for owners. */
    OWNER("PRV-1", "an owner", true),
    /** HLP-1 to HLP-3: aged 18 or above, and local, or foreign with a work permit. */
    MEMBER("HLP-1", "a member", true);

    private final String ruleId;
    private final String description;
    private final boolean reverifies;

    PersonRole(String ruleId, String description, boolean reverifies) {
        this.ruleId = ruleId;
        this.description = description;
        this.reverifies = reverifies;
    }

    /** The rule that sets who may hold the role. */
    public String ruleId() {
        return ruleId;
    }

    /** The role in a sentence, for example "a customer". */
    public String description() {
        return description;
    }

    /** True when the role's holder re-verifies every cycle (MON-1). */
    public boolean reverifies() {
        return reverifies;
    }
}
