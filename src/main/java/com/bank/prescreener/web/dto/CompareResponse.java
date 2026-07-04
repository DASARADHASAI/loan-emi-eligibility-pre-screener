package com.bank.prescreener.web.dto;

import com.bank.prescreener.foir.ScreenError;
import java.util.List;

/** Result of comparing across all products, or a single validation error. */
public record CompareResponse(boolean ok, ScreenError error, List<CompareItem> items) {
}
