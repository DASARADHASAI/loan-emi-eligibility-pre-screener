package com.bank.prescreener.web.dto;

import com.bank.prescreener.foir.Advice;
import com.bank.prescreener.foir.ScreenError;
import com.bank.prescreener.foir.ScreenResult;

/**
 * Full screening response: the verdict (or a validation error), plus a counter-offer
 * ({@code advice}) whenever the screen succeeded.
 */
public record ScreenResponse(boolean ok, ScreenResult result, ScreenError error, Advice advice) {
}
