package account.domain;

import account.domain.exception.InvalidPhoneNumberException;

/** A Malaysian mobile number, kept in international form: +60 followed by the number without its leading 0. */
public record PhoneNumber(String value) {

    public PhoneNumber {
        String national = national(value);
        if (!national.matches("01\\d{8,9}")) {
            throw new InvalidPhoneNumberException(value);
        }
        value = "+6" + national;
    }

    /** The number as dialled inside Malaysia, starting with 0, whatever form it was typed in. */
    private static String national(String typed) {
        if (typed == null) {
            throw new InvalidPhoneNumberException(null);
        }
        String digits = typed.replaceAll("[\\s-]", "");
        if (digits.startsWith("+60")) {
            return digits.substring(2);
        }
        if (digits.startsWith("60")) {
            return digits.substring(1);
        }
        return digits;
    }
}
