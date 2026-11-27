package com.homa.payment.domain;

/**
 * The bank details of a payout account: the bank, the account number and the name the account is held in. Kept while
 * the account is in use and until its last payout is released (DAT-13). {@link #toString()} shows only the last four
 * characters of the account number, so the number cannot leak into a log.
 */
public record BankAccount(String bank, String accountNumber, String holderName) {

    private static final int SHOWN_CHARACTERS = 4;

    public BankAccount {
        Ids.requireText(bank, "The bank");
        Ids.requireText(accountNumber, "The account number");
        Ids.requireText(holderName, "The account holder's name");
    }

    /** The bank and holder, with only the last four characters of the account number. */
    @Override
    public String toString() {
        String shown = accountNumber.substring(Math.max(0, accountNumber.length() - SHOWN_CHARACTERS));
        return "BankAccount[" + bank + " ****" + shown + ", " + holderName + "]";
    }
}
