package com.bank.prescreener.foir;

/**
 * A validation failure — a machine code plus a human-readable message.
 *
 * @param code    e.g. "ERR_INCOME_NONPOSITIVE"
 * @param message explanation for the agent
 */
public record ScreenError(String code, String message) {
}
