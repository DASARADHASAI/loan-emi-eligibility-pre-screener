# Loan EMI Eligibility Pre-Screener — Design Spec

**Date:** 2026-07-03 · **Stack:** Java 21 + Spring Boot (Maven)

## Problem

A retail bank's call-centre agents waste time on applicants who can never qualify
for a loan. They need a fast tool that takes an applicant's financials and instantly
returns a **pre-eligibility verdict** based on the **Fixed-Obligation-to-Income-Ratio
(FOIR)** rule — *before* a full credit check is triggered.

## Use cases (all 4)

1. Agent enters applicant financials → instant **Eligible / Borderline / Not Eligible**.
2. Tool **computes the proposed EMI** from principal, rate and tenure and adds it to existing obligations.
3. **Borderline** cases (FOIR 50–55%) are flagged for **manual review**, not rejection.
4. A one-line **reason code** is generated that the agent reads to the customer.
5. **Smart Counter-Offer** *(added feature)* — on a Borderline/Not-Eligible result the tool
   inverts the EMI/FOIR maths to offer the **max loan they qualify for** and the **tenure**
   needed for the requested amount; live what-if sliders let the agent explore on the call.
   See `Advisor.java`.

## Watch-outs → how the design solves each

| Watch-out | Mitigation |
|---|---|
| Integer division / precision loss | All arithmetic in `double`. FOIR computed at full precision; only rounded **for display**, never truncated. See `FoirEngine`. |
| Banker's vs simple rounding | EMI rounding funnels through `Money.round2(value, mode)` using `BigDecimal` + `RoundingMode`. Default **HALF_EVEN** (banker's), matching core-banking ledgers. |
| Zero / negative inputs | `FoirEngine.validate()` runs before any arithmetic and returns a structured `ScreenError` — never throws, never returns `NaN`. |
| Hard-coded interest slabs | Rates + FOIR thresholds live in one `TariffConfig` class — a single edit updates the whole tool. |

## Concepts exercised (from the brief)

Primitive `double`/`int` types, explicit casting (`(long) value`), arithmetic
operators (reducing-balance EMI), an **if-else-if ladder** for the verdict band, a
**`switch`** on the computed band for the reason text, and input guards.

## Architecture

A single Spring Boot app. No database is required by the brief, so there is none —
the tool is a stateless calculator with a premium web UI.

```
Browser (static UI)                 Spring Boot (Java)
┌───────────────────────────┐       ┌──────────────────────────────┐
│ index.html  (screener)    │  POST │ ScreenController → Screening   │
│  css/app.css              │──────►│   Service → FoirEngine (pure)  │
│  js/screener.js (dial)    │       │ RateSlabController (GET)       │
│                           │◄──────│ PolicyController   (GET)       │
└───────────────────────────┘  JSON │ TariffConfig (central rates)   │
                                     └──────────────────────────────┘
```

### The engine — `com.bank.prescreener.foir` (pure, unit-tested)

- `computeEmi(P, ratePct, months)` — reducing-balance EMI; falls back to `P/n` when rate = 0.
- `validate(inputs)` — guards income/EMI/loan/rate/tenure.
- `screen(inputs, policy)` — validate → EMI → FOIR → classify; returns `ScreenOutcome`
  (`ok` + `ScreenResult`, or an `error`).
- Verdict bands: `≤ eligibleMax` → ELIGIBLE, `≤ borderlineMax` → BORDERLINE, else NOT_ELIGIBLE.
- Reason codes: `ELIG_FOIR_OK`, `BORDERLINE_REVIEW`, `REJECT_FOIR_HIGH`, and the
  `ERR_*` validation codes.

### HTTP API

- `POST /api/screen` — body `{income, existingEmi, loanAmount, annualRatePct, tenureMonths}` → `ScreenOutcome`.
- `GET /api/rate-slabs` — interest products for the dropdown.
- `GET /api/policy` — FOIR thresholds + rounding mode (the dial paints its zones from this).

### UI — "cockpit" dark fintech

Two-column console: controls on the left, a **270° FOIR risk dial** on the right that
sweeps to the computed ratio and colour-shifts across green/amber/red zones. Deep navy
canvas, glass surfaces, periwinkle brand accent kept separate from the traffic-light
verdict palette. Type: Space Grotesk (display) / Inter (body) / JetBrains Mono (readouts).
Vanilla HTML/CSS/JS — no framework, no build step. Respects reduced motion; responsive to mobile.

## Testing

JUnit 5 on `FoirEngine` + `Money` (19 tests): golden EMI values, zero-rate branch,
each verdict band + exact boundaries (50%, 55%), banker's-vs-half-up divergence, and
every validation guard.

## Out of scope (YAGNI)

Database, persistence/history, admin config UI, authentication, credit scoring.
This is a stateless pre-screen. (Deployment to Vercel: to be revisited later per the user.)
