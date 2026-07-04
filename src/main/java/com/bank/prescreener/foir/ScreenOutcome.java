package com.bank.prescreener.foir;

/**
 * Result of a pre-screen: either a {@link ScreenResult} or a {@link ScreenError},
 * never both. Modelled as a value object so callers never have to catch exceptions
 * for the expected "invalid input" path.
 */
public record ScreenOutcome(boolean ok, ScreenResult result, ScreenError error) {

    public static ScreenOutcome ok(ScreenResult result) {
        return new ScreenOutcome(true, result, null);
    }

    public static ScreenOutcome failure(ScreenError error) {
        return new ScreenOutcome(false, null, error);
    }
}
