# LoanLens

**LoanLens** is a small web app that tells a retail bank's call-centre agents, in one
second, whether a loan applicant **can** qualify — using the **FOIR** rule (Fixed
Obligation to Income Ratio). It returns an instant **Eligible / Borderline / Not
Eligible** verdict, the proposed EMI, and a one-line reason to read to the customer —
*before* any full credit check. If it's a "no", it offers the largest loan they *can* get.

> *LoanLens is the product name; **FOIR** is the banking metric it's built on.*

**Stack:** Java 21 · Spring Boot 3 (Maven) · Spring Security + H2 (user accounts) ·
vanilla HTML/CSS/JS front-end (Claude-inspired design).

**Pages:** `/` landing · `/signup` · `/login` · `/app` (screener) · `/history` (dashboard) — all app pages login-required.

---

## What it does (the 4 use cases)

1. **Instant verdict** from monthly income, existing EMIs, loan amount, rate and tenure.
2. **Computes the proposed EMI** (reducing-balance formula) and adds it to existing obligations.
3. **Flags borderline cases** (FOIR 50–55%) for manual review instead of rejecting them.
4. **Generates a reason code** (e.g. `ELIG_FOIR_OK`) the agent can read out.

FOIR = (existing EMIs + new EMI) / monthly income.
Bands: **≤ 50% Eligible · 50–55% Borderline · > 55% Not eligible** (configurable in one place).

## ⭐ Smart Counter-Offer (the headline feature)

A rejection is a wasted call. So the tool never just says "no" — it inverts the
EMI/FOIR maths to tell the agent **what the applicant *can* get**:

- the **largest loan** they qualify for at the same rate & tenure, and
- the **tenure** they'd need to still get the amount they asked for.

One click applies either counter-offer and re-screens. **What-if sliders** (loan
amount + tenure) let the agent drag and watch the dial move live while on the call.
For an already-eligible applicant it shows the lending **headroom** (an upsell).
The solver lives in `Advisor.java` and is unit-tested.

## Plus

- **Multi-product comparison** — one click screens the applicant against *every*
  loan product at once, sorted best-first, so the agent sees which loans they qualify for.
- **Dashboard** (`/history`) — every screening is saved per agent; the dashboard shows
  total screened, approval rate, average FOIR, the eligible/borderline/rejected split,
  and a table of recent screenings.

---

## Run it

Requires **JDK 21** and **Maven** (both already installed on this machine).

### Option A — command line

```bash
# from the project root
mvn spring-boot:run
```

Then open **http://localhost:8081** → **Get started** to create an account → log in →
you land on the screener. Accounts are stored (BCrypt-hashed) in a local H2 file under
`./data/`, so they persist across restarts.

Build a runnable jar instead:

```bash
mvn clean package
java -jar target/prescreener-1.0.0.jar
```

Run the tests:

```bash
mvn test        # 24 JUnit tests: FOIR engine + counter-offer solver
```

### Option B — Eclipse

1. **File → Import… → Maven → Existing Maven Projects**.
2. Select this folder (the one containing `pom.xml`) → **Finish**. Let Eclipse download dependencies.
3. Open `src/main/java/com/bank/prescreener/PreScreenerApplication.java`.
4. Right-click it → **Run As → Java Application** (or **Spring Boot App** if you have the Spring Tools plugin).
5. Open **http://localhost:8081** and sign up / log in.

Run the tests in Eclipse: right-click `src/test/java` → **Run As → JUnit Test**.

> Tip: if port 8080 is busy, start with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
> (or set `PORT=8081`), then use that port.

---

## How the "watch-outs" from the brief are handled

| Watch-out | Where it's solved |
|---|---|
| Integer division / precision loss | `FoirEngine` computes everything in `double`; FOIR is rounded only for display, never truncated. |
| Banker's vs simple rounding | `Money.round2(value, mode)` — `BigDecimal` + `RoundingMode` (default HALF_EVEN). |
| Zero / negative inputs | `FoirEngine.validate()` guards every field before arithmetic and returns a clean error. |
| Hard-coded interest slabs | `TariffConfig` — all rates + thresholds in one class; one edit updates the whole tool. |

---

## Project layout

```
src/main/java/com/bank/prescreener/
  foir/            ← pure FOIR engine + Advisor (counter-offer solver) + value types
  config/          ← TariffConfig: central rate slabs + FOIR policy
  service/         ← ScreeningService: wires HTTP request → engine
  web/             ← REST controllers (screen, rate-slabs, policy) + DTOs
src/main/resources/
  static/          ← index.html, css/app.css, js/screener.js (the dark UI + FOIR dial)
src/test/java/…    ← JUnit tests (FoirEngineTest, MoneyTest)
```

## API

All `/api/**` endpoints require a logged-in session (use the site to sign up / log in).

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/screen` | `{income, existingEmi, loanAmount, annualRatePct, tenureMonths}` → verdict + EMI + reason **+ counter-offer `advice`** (also saved to history) |
| POST | `/api/compare` | `{income, existingEmi, loanAmount, tenureMonths}` → verdict per product |
| GET | `/api/history` | The agent's screening stats + recent screenings |
| GET | `/api/rate-slabs` | Interest products for the dropdown |
| GET | `/api/policy` | FOIR thresholds + rounding mode |
| GET | `/api/me` | The logged-in agent's name/email |

Auth: `POST /signup` (name, email, password) · `POST /login` (email, password) · `POST /logout`.
