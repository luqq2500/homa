package policy.usecase;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kernel.Percentage;
import policy.usecase.exception.InvalidPolicyAmountException;

/**
 * How the admin console writes the amount of a Homa-set value: a percentage such as {@code 17.5%}, a
 * period of whole minutes in ISO-8601 such as {@code PT2H} or {@code P30D}, a count such as {@code 300},
 * or a range of hours such as {@code 1-6}, all without leading zeros. Reading and writing round-trip, so the console can show a value and send it
 * back changed.
 */
final class PolicyAmounts {

    private static final Pattern PERCENTAGE = Pattern.compile("((0|[1-9]\\d{0,2})(\\.\\d{1,2})?)%?");
    private static final Pattern RANGE = Pattern.compile("([1-9]\\d{0,3})-([1-9]\\d{0,3})");
    private static final Pattern COUNT = Pattern.compile("0|[1-9]\\d{0,8}");
    private static final int BASIS_POINTS_PER_PERCENT = 100;
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    private PolicyAmounts() {
    }

    static Percentage percentage(String value, String amount) {
        Matcher matcher = PERCENTAGE.matcher(trimmed(amount));
        if (!matcher.matches()) {
            throw new InvalidPolicyAmountException(value, amount, "a percentage such as 17.5%");
        }
        int basisPoints = new BigDecimal(matcher.group(1)).movePointRight(2).intValueExact();
        if (basisPoints > 100 * BASIS_POINTS_PER_PERCENT) {
            throw new InvalidPolicyAmountException(value, amount, "a percentage from 0% to 100%");
        }
        return Percentage.ofBasisPoints(basisPoints);
    }

    /** A period in whole minutes; Homa sets no value to the second. */
    static Duration period(String value, String amount) {
        Duration period = parsedPeriod(value, amount);
        if (period.toSeconds() % SECONDS_PER_MINUTE != 0 || period.getNano() != 0) {
            throw notAPeriod(value, amount);
        }
        return period;
    }

    private static Duration parsedPeriod(String value, String amount) {
        try {
            return Duration.parse(trimmed(amount));
        } catch (DateTimeParseException notWritten) {
            throw notAPeriod(value, amount);
        }
    }

    private static InvalidPolicyAmountException notAPeriod(String value, String amount) {
        return new InvalidPolicyAmountException(value, amount, "a period of whole minutes such as PT2H or P30D");
    }

    static Integer count(String value, String amount) {
        String count = trimmed(amount);
        if (!COUNT.matcher(count).matches()) {
            throw new InvalidPolicyAmountException(value, amount, "a whole number such as 300");
        }
        return Integer.valueOf(count);
    }

    static int[] range(String value, String amount) {
        Matcher matcher = RANGE.matcher(trimmed(amount));
        if (!matcher.matches()) {
            throw new InvalidPolicyAmountException(value, amount, "a range of hours such as 1-6");
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    static String write(Percentage percentage) {
        return percentage.toString();
    }

    /** Whole days as days, such as {@code P30D}; anything else in hours and minutes, such as {@code PT2H}. */
    static String write(Duration period) {
        if (period.toSeconds() % SECONDS_PER_DAY == 0) {
            return "P" + period.toDays() + "D";
        }
        return period.toString();
    }

    static String write(int count) {
        return Integer.toString(count);
    }

    static String range(int min, int max) {
        return min + "-" + max;
    }

    private static String trimmed(String amount) {
        return amount == null ? "" : amount.strip();
    }
}
