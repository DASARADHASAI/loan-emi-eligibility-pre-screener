package com.bank.prescreener.web.dto;

/** The FOIR policy exposed to the UI (drives the dial zones + legend). */
public record PolicyView(double eligibleMax, double borderlineMax, String roundingMode) {
}
