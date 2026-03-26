#!/usr/bin/env python3
"""
immudb ledger viewer for OpenDebt TB-028 spike.
Fetches all KV entries from immudb via REST API and renders a static HTML report.

Usage: python immudb-view.py [--open]
"""
import base64, json, sys, webbrowser, urllib.request, urllib.error
from datetime import datetime
from pathlib import Path

IMMUDB_URL = "http://localhost:8091/api"

def b64d(s: str) -> bytes:
    return base64.b64decode(s)

def post(path: str, body: dict, token: str = None) -> dict:
    data = json.dumps(body).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(f"{IMMUDB_URL}{path}", data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.loads(r.read())

def login() -> str:
    resp = post("/login", {"user": base64.b64encode(b"immudb").decode(), "password": base64.b64encode(b"immudb").decode()})
    return resp["token"]

def scan_all(token: str) -> list:
    resp = post("/db/scan", {"limit": 500, "desc": False}, token)
    return resp.get("entries", [])

def render(entries: list) -> str:
    rows = []
    for e in sorted(entries, key=lambda x: x["tx"]):
        key = b64d(e["key"]).decode("utf-8", errors="replace")
        try:
            val = json.loads(b64d(e["value"]).decode("utf-8"))
        except Exception:
            continue
        rows.append({"tx": e["tx"], "key": key, "val": val})

    txn_ids = {r["val"].get("transactionId","") for r in rows}
    debits = [r for r in rows if r["val"].get("entryType") == "DEBIT"]
    total = sum(float(r["val"].get("amount", 0)) for r in debits)

    def badge(txn_id):
        if txn_id.startswith("a0"): return '<span class="badge badge-a">Debt A</span>'
        if txn_id.startswith("b0"): return '<span class="badge badge-b">Debt B</span>'
        return ""

    def fmt(n):
        try: return f"{float(n):,.2f}"
        except: return str(n)

    row_html = "\n".join(f"""
      <tr>
        <td>{r['tx']}</td>
        <td class="{'debit' if r['val'].get('entryType')=='DEBIT' else 'credit'}">{r['val'].get('entryType','')}</td>
        <td class="tx-id">{r['val'].get('transactionId','')[:8]}…</td>
        <td>{badge(r['val'].get('transactionId',''))}</td>
        <td>{r['val'].get('accountCode','-')}</td>
        <td class="amount">{fmt(r['val'].get('amount',0))}</td>
        <td>{r['val'].get('currency','DKK')}</td>
        <td class="key" title="{r['key']}">{r['key'][:18]}…</td>
      </tr>""" for r in rows)

    generated = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>immudb Ledger — OpenDebt TB-028</title>
  <style>
    body {{ font-family: system-ui, sans-serif; background: #0f172a; color: #e2e8f0; margin: 0; padding: 24px; }}
    h1 {{ color: #38bdf8; margin-bottom: 4px; }}
    .subtitle {{ color: #64748b; margin-bottom: 24px; font-size: 13px; }}
    .stats {{ display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }}
    .stat {{ background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 16px 24px; }}
    .stat-value {{ font-size: 28px; font-weight: 700; color: #38bdf8; }}
    .stat-label {{ font-size: 12px; color: #64748b; margin-top: 4px; }}
    table {{ width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 8px; overflow: hidden; font-size: 13px; }}
    th {{ background: #0f172a; color: #94a3b8; font-size: 11px; text-transform: uppercase; letter-spacing:.05em; padding: 10px 12px; text-align: left; }}
    td {{ padding: 9px 12px; border-top: 1px solid #334155; font-family: monospace; }}
    tr:hover td {{ background: #263348; }}
    .debit  {{ color: #f87171; font-weight: 600; }}
    .credit {{ color: #4ade80; font-weight: 600; }}
    .amount {{ text-align: right; }}
    .tx-id, .key {{ color: #94a3b8; font-size: 11px; }}
    .badge {{ display: inline-block; padding: 2px 8px; border-radius: 9999px; font-size: 11px; }}
    .badge-a {{ background: #1e3a5f; color: #7dd3fc; }}
    .badge-b {{ background: #1a3a2e; color: #86efac; }}
    .note {{ margin-top: 24px; color: #475569; font-size: 12px; }}
  </style>
</head>
<body>
  <h1>🔐 immudb Financial Ledger</h1>
  <div class="subtitle">OpenDebt · TB-028 spike · KV tamper-evidence layer · defaultdb · Generated {generated}</div>
  <div class="stats">
    <div class="stat"><div class="stat-value">{len(rows)}</div><div class="stat-label">KV Entries</div></div>
    <div class="stat"><div class="stat-value">{len(rows)//2}</div><div class="stat-label">Double-Entry Pairs</div></div>
    <div class="stat"><div class="stat-value">{len(txn_ids)}</div><div class="stat-label">Unique Transactions</div></div>
    <div class="stat"><div class="stat-value">{fmt(total)} DKK</div><div class="stat-label">Total Debited</div></div>
  </div>
  <table>
    <thead><tr>
      <th>Tx#</th><th>Type</th><th>Transaction ID</th><th>Debt</th>
      <th>Account</th><th class="amount">Amount</th><th>Currency</th><th>Entry UUID</th>
    </tr></thead>
    <tbody>{row_html}</tbody>
  </table>
  <div class="note">
    ℹ Each row is a cryptographically committed KV entry in immudb (Merkle tree proof available via <code>POST /api/db/verified-get</code>).
    Keys are LedgerEntry UUIDs from PostgreSQL. Values are LedgerImmuRecord JSON payloads (ADR-0029).
  </div>
</body>
</html>"""

if __name__ == "__main__":
    print("Connecting to immudb at localhost:8091…")
    try:
        token = login()
        print("✓ Logged in")
        entries = scan_all(token)
        print(f"✓ {len(entries)} KV entries fetched")
        html = render(entries)
        out = Path(__file__).parent / "immudb-report.html"
        out.write_text(html, encoding="utf-8")
        print(f"✓ Report written to {out}")
        if "--open" in sys.argv or True:
            webbrowser.open(str(out))
    except urllib.error.URLError as e:
        print(f"✗ Cannot reach immudb at {IMMUDB_URL}: {e}")
        sys.exit(1)
