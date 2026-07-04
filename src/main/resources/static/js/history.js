// Dashboard — per-agent screening stats + recent list.
(() => {
  const $ = (id) => document.getElementById(id);
  const inr0 = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 0 });
  const inr2 = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 });

  const VERDICT = {
    ELIGIBLE: { cls: "tag--eligible", label: "Eligible" },
    BORDERLINE: { cls: "tag--borderline", label: "Borderline" },
    NOT_ELIGIBLE: { cls: "tag--reject", label: "Not eligible" },
  };

  const when = (iso) =>
    new Date(iso).toLocaleString("en-IN", {
      day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit",
    });

  fetch("/api/me").then((r) => (r.ok ? r.json() : null)).then((u) => {
    if (u) $("who").textContent = "Hi, " + u.name;
  }).catch(() => {});

  function rowHtml(s) {
    const v = VERDICT[s.verdict] || VERDICT.NOT_ELIGIBLE;
    return `<tr>
      <td>${when(s.createdAt)}</td>
      <td class="num">₹${inr0.format(s.income)}</td>
      <td class="num">₹${inr0.format(s.loanAmount)}</td>
      <td class="num">${s.annualRate}%</td>
      <td class="num">₹${inr2.format(s.newEmi)}</td>
      <td class="num">${s.foir.toFixed(1)}%</td>
      <td><span class="tag ${v.cls}">${v.label}</span></td>
    </tr>`;
  }

  async function load() {
    let data;
    try {
      const res = await fetch("/api/history");
      if (res.status === 401 || res.status === 403 || res.redirected) {
        location.href = "/login";
        return;
      }
      data = await res.json();
    } catch {
      $("hist-rows").innerHTML = `<tr><td colspan="7" class="table-empty">Couldn't load history.</td></tr>`;
      return;
    }

    $("t-total").textContent = data.stats.total;
    $("t-rate").innerHTML = data.stats.approvalRate + '<span class="tile__u">%</span>';
    $("t-avg").innerHTML = data.stats.avgFoir.toFixed(1) + '<span class="tile__u">%</span>';
    $("t-elig").textContent = data.stats.eligible;
    $("t-brd").textContent = data.stats.borderline;
    $("t-rej").textContent = data.stats.notEligible;

    const rows = data.screenings;
    $("hist-rows").innerHTML = rows.length
      ? rows.map(rowHtml).join("")
      : `<tr><td colspan="7" class="table-empty">No screenings yet — run one on the <a href="/app">screener</a>.</td></tr>`;
  }

  load();
})();
