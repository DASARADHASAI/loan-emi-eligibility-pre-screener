// Screener — talks to the Java API, drives the FOIR dial, counter-offer & what-if.
(() => {
  const $ = (id) => document.getElementById(id);
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const inr0 = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 0 });
  const inr2 = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 });
  const rupees = (n) => "₹" + inr0.format(Math.round(n));
  const money = (n) => "₹" + inr2.format(n);

  const VERDICT = {
    ELIGIBLE: { label: "Eligible", color: "var(--eligible)" },
    BORDERLINE: { label: "Borderline · review", color: "var(--borderline)" },
    NOT_ELIGIBLE: { label: "Not eligible", color: "var(--reject)" },
  };

  const result = $("result");
  const progress = $("gauge-progress");
  const dialValue = $("dial-value");
  let slabs = [];
  const state = { loanMax: 0, slidersReady: false };

  // ---------- data loading ----------
  async function loadSlabs() {
    try {
      slabs = await fetch("/api/rate-slabs").then((r) => r.json());
      const sel = $("product");
      for (const s of slabs) {
        const opt = document.createElement("option");
        opt.value = s.product;
        opt.textContent = `${s.product} · ${s.annualRate}% p.a.`;
        opt.dataset.rate = s.annualRate;
        sel.appendChild(opt);
      }
    } catch {
      $("product-note").textContent = "Product list unavailable — enter the rate manually.";
    }
  }

  $("product").addEventListener("change", (e) => {
    const opt = e.target.selectedOptions[0];
    if (opt && opt.dataset.rate) {
      $("annualRatePct").value = opt.dataset.rate;
      $("product-note").textContent = `${opt.value}: rate applied. Adjust if needed.`;
    }
  });

  async function loadPolicy() {
    let elig = 50, brd = 55;
    try {
      const p = await fetch("/api/policy").then((r) => r.json());
      elig = Number(p.eligibleMax);
      brd = Number(p.borderlineMax);
    } catch { /* defaults */ }

    $("zone-green").style.strokeDasharray = `${elig} 100`;
    $("zone-green").style.strokeDashoffset = "0";
    $("zone-amber").style.strokeDasharray = `${brd - elig} 100`;
    $("zone-amber").style.strokeDashoffset = `-${elig}`;
    $("zone-red").style.strokeDasharray = `${100 - brd} 100`;
    $("zone-red").style.strokeDashoffset = `-${brd}`;
    $("lg-elig").textContent = elig;
    $("lg-brd-lo").textContent = elig;
    $("lg-brd-hi").textContent = brd;
    $("lg-rej").textContent = brd;
    $("policy-hint").textContent = `policy ${elig} / ${brd}`;
  }

  // ---------- dial ----------
  function sweep(foir, color) {
    const capped = Math.min(Math.max(foir, 0), 100);
    result.style.setProperty("--verdict", color);
    progress.style.strokeDashoffset = String(100 - capped);
    if (reduceMotion) {
      dialValue.textContent = foir.toFixed(1);
      return;
    }
    const start = performance.now();
    const from = parseFloat(dialValue.textContent) || 0;
    const tick = (now) => {
      const t = Math.min((now - start) / 900, 1);
      const eased = 1 - Math.pow(1 - t, 3);
      dialValue.textContent = (from + (foir - from) * eased).toFixed(1);
      if (t < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }

  // ---------- screening ----------
  const numOrNull = (el) => (el.value.trim() === "" ? null : Number(el.value));

  function readPayload() {
    return {
      income: numOrNull($("income")),
      existingEmi: numOrNull($("existingEmi")),
      loanAmount: numOrNull($("loanAmount")),
      annualRatePct: numOrNull($("annualRatePct")),
      tenureMonths: numOrNull($("tenureMonths")),
    };
  }

  async function runScreen(syncSliders) {
    const payload = readPayload();
    let resp;
    try {
      resp = await fetch("/api/screen", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }).then((r) => r.json());
    } catch {
      showToast("Network error — is the server running?");
      return;
    }
    if (resp.ok) renderOk(resp, payload, syncSliders);
    else renderError(resp.error);
  }

  function renderOk(resp, payload, syncSliders) {
    result.classList.remove("is-idle");
    const r = resp.result;
    const v = VERDICT[r.verdict] || VERDICT.NOT_ELIGIBLE;
    sweep(r.foir, v.color);

    $("verdict-chip").textContent = v.label;
    $("reason-code").textContent = r.reasonCode;
    $("reason-text").textContent = r.reasonText;
    $("out-emi").textContent = money(r.newEmi);
    $("out-total").textContent = money(r.totalObligations);
    $("out-foir").textContent = r.foir.toFixed(1) + "%";

    renderAdvice(r, resp.advice, payload);
    if (syncSliders) setupSliders(payload, resp.advice);
    else refreshSliderFills();
  }

  function renderError(err) {
    result.classList.remove("is-idle");
    sweep(0, "var(--reject)");
    $("verdict-chip").textContent = "Check inputs";
    $("reason-code").textContent = err.code;
    $("reason-text").textContent = err.message;
    $("out-emi").textContent = "—";
    $("out-total").textContent = "—";
    $("out-foir").textContent = "—";
    $("offer").hidden = true;
    $("whatif").hidden = true;
  }

  // ---------- counter-offer card ----------
  function renderAdvice(r, advice, payload) {
    const offer = $("offer");
    const badge = $("offer-badge");
    const body = $("offer-body");
    offer.classList.remove("offer--warn");

    if (!advice) {
      offer.hidden = true;
      return;
    }

    if (r.verdict === "ELIGIBLE") {
      if (advice.feasible && advice.maxEligibleLoan > payload.loanAmount) {
        badge.textContent = "Headroom";
        body.innerHTML = line(
          `Approved with room to spare — this profile supports up to <b>${rupees(advice.maxEligibleLoan)}</b> (EMI <b>${money(advice.maxEligibleEmi)}</b>) at the same terms.`,
          `Offer ${rupees(advice.maxEligibleLoan)}`,
          "loan",
          advice.maxEligibleLoan,
        );
        offer.hidden = false;
      } else {
        offer.hidden = true;
      }
      return;
    }

    // Borderline / Not eligible → counter-offer
    if (!advice.feasible) {
      offer.classList.add("offer--warn");
      badge.textContent = "Action needed";
      body.innerHTML =
        `<div class="offer__warn">Existing EMIs alone reach the FOIR limit — advise reducing current obligations before any new loan.</div>`;
      offer.hidden = false;
      return;
    }

    badge.textContent = "Counter-offer";
    let html = line(
      `Largest loan they qualify for now: <b>${rupees(advice.maxEligibleLoan)}</b> · EMI <b>${money(advice.maxEligibleEmi)}</b>.`,
      `Offer ${rupees(advice.maxEligibleLoan)}`,
      "loan",
      advice.maxEligibleLoan,
    );
    if (advice.tenureToQualify > 0 && advice.tenureToQualify !== payload.tenureMonths) {
      html += line(
        `Or keep <b>${rupees(payload.loanAmount)}</b> and extend the tenure to <b>${advice.tenureToQualify} months</b>.`,
        `Extend to ${advice.tenureToQualify} mo`,
        "tenure",
        advice.tenureToQualify,
      );
    }
    body.innerHTML = html;
    offer.hidden = false;
  }

  function line(desc, btn, act, val) {
    return `<div class="offer__line">
      <div class="offer__desc">${desc}</div>
      <button class="offer__btn" type="button" data-act="${act}" data-val="${val}">${btn}</button>
    </div>`;
  }

  $("offer-body").addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-act]");
    if (!btn) return;
    const val = Number(btn.dataset.val);
    if (btn.dataset.act === "loan") $("loanAmount").value = val;
    else $("tenureMonths").value = val;
    runScreen(true);
  });

  // ---------- what-if sliders ----------
  function niceMax(x) {
    return Math.max(500000, Math.ceil(x / 100000) * 100000);
  }

  function setupSliders(payload, advice) {
    const loan = payload.loanAmount || 0;
    const wanted = Math.max(loan, advice ? advice.maxEligibleLoan : 0, 200000) * 1.5;
    state.loanMax = Math.max(state.loanMax, niceMax(wanted));

    const wiLoan = $("wi-loan");
    const wiTenure = $("wi-tenure");
    wiLoan.max = state.loanMax;
    wiLoan.value = Math.min(Math.max(loan, 0), state.loanMax);
    wiTenure.value = Math.min(Math.max(payload.tenureMonths || 6, 6), 360);

    $("whatif").hidden = false;
    state.slidersReady = true;
    updateSliderLabels();
  }

  function updateSliderLabels() {
    $("wi-loan-val").textContent = rupees(Number($("wi-loan").value));
    $("wi-tenure-val").textContent = $("wi-tenure").value + " mo";
    refreshSliderFills();
  }

  function refreshSliderFills() {
    fillRange($("wi-loan"));
    fillRange($("wi-tenure"));
  }

  function fillRange(el) {
    const min = Number(el.min), max = Number(el.max), val = Number(el.value);
    const pct = max > min ? ((val - min) / (max - min)) * 100 : 0;
    el.style.background = `linear-gradient(90deg, var(--brand) ${pct}%, rgba(255,255,255,0.1) ${pct}%)`;
  }

  const debounce = (fn, ms) => {
    let t;
    return (...a) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...a), ms);
    };
  };
  const debouncedScreen = debounce(() => runScreen(false), 160);

  $("wi-loan").addEventListener("input", (e) => {
    $("loanAmount").value = e.target.value;
    $("wi-loan-val").textContent = rupees(Number(e.target.value));
    fillRange(e.target);
    debouncedScreen();
  });

  $("wi-tenure").addEventListener("input", (e) => {
    $("tenureMonths").value = e.target.value;
    $("wi-tenure-val").textContent = e.target.value + " mo";
    fillRange(e.target);
    debouncedScreen();
  });

  // ---------- misc ----------
  $("screen-form").addEventListener("submit", (e) => {
    e.preventDefault();
    runScreen(true);
  });

  // ---------- multi-product comparison ----------
  const VERDICT_TAG = {
    ELIGIBLE: { cls: "tag--eligible", label: "Eligible" },
    BORDERLINE: { cls: "tag--borderline", label: "Borderline" },
    NOT_ELIGIBLE: { cls: "tag--reject", label: "Not eligible" },
  };
  const rank = { ELIGIBLE: 0, BORDERLINE: 1, NOT_ELIGIBLE: 2 };

  $("compare-btn").addEventListener("click", async () => {
    const payload = {
      income: numOrNull($("income")),
      existingEmi: numOrNull($("existingEmi")),
      loanAmount: numOrNull($("loanAmount")),
      tenureMonths: numOrNull($("tenureMonths")),
    };
    let resp;
    try {
      resp = await fetch("/api/compare", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }).then((r) => r.json());
    } catch {
      return showToast("Network error — is the server running?");
    }
    if (!resp.ok) {
      return showToast(resp.error ? resp.error.message : "Enter income, loan amount and tenure first.");
    }

    const items = [...resp.items].sort(
      (a, b) => rank[a.verdict] - rank[b.verdict] || a.annualRate - b.annualRate,
    );
    const eligible = items.filter((i) => i.verdict === "ELIGIBLE").length;
    $("compare-hint").textContent = `${items.length} products · ${eligible} eligible`;
    $("compare-rows").innerHTML = items
      .map((i) => {
        const v = VERDICT_TAG[i.verdict] || VERDICT_TAG.NOT_ELIGIBLE;
        const range = i.withinRange
          ? ""
          : ` <span class="out-note">outside ₹${inr0.format(i.minAmount)}–₹${inr0.format(i.maxAmount)}</span>`;
        return `<tr>
          <td><b>${i.product}</b>${range}</td>
          <td class="num">${i.annualRate}%</td>
          <td class="num">${money(i.newEmi)}</td>
          <td class="num">${i.foir.toFixed(1)}%</td>
          <td><span class="tag ${v.cls}">${v.label}</span></td>
        </tr>`;
      })
      .join("");
    $("compare-panel").hidden = false;
    $("compare-panel").scrollIntoView({ behavior: reduceMotion ? "auto" : "smooth", block: "nearest" });
  });

  let toastTimer;
  function showToast(msg) {
    const t = $("toast");
    t.textContent = msg;
    t.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove("show"), 3200);
  }

  loadSlabs();
  loadPolicy();
})();
