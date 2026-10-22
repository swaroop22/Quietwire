"""
Quietwire tiny admin API — wraps add-client.sh / remove-client.sh over HTTP.

Run ONLY on localhost or behind a VPN/SSH tunnel + a bearer token.
Never expose this directly on the public internet without a reverse proxy,
TLS, and real auth — this is a convenience tool for YOU, not a public API.

Install:
    pip install fastapi uvicorn
Run:
    sudo ADMIN_TOKEN=supersecret uvicorn admin_api:app --host 127.0.0.1 --port 8787

Then from your laptop, over an SSH tunnel:
    ssh -L 8787:127.0.0.1:8787 root@YOUR_VPS_IP
    curl -H "Authorization: Bearer supersecret" \
         -X POST "http://127.0.0.1:8787/clients/firetv-livingroom"
"""
import os
import re
import subprocess
from pathlib import Path

from fastapi import FastAPI, Header, HTTPException

ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN")
if not ADMIN_TOKEN:
    raise RuntimeError("Set ADMIN_TOKEN env var before starting this API.")

SCRIPT_DIR = Path(__file__).parent.resolve()
NAME_RE = re.compile(r"^[a-zA-Z0-9_-]{1,32}$")

app = FastAPI(title="Quietwire Admin API")


def _check_auth(authorization: str | None):
    if authorization != f"Bearer {ADMIN_TOKEN}":
        raise HTTPException(status_code=401, detail="Unauthorized")


def _check_name(name: str):
    if not NAME_RE.match(name):
        raise HTTPException(status_code=400, detail="Invalid client name")


@app.post("/clients/{name}")
def add_client(name: str, authorization: str | None = Header(None)):
    _check_auth(authorization)
    _check_name(name)
    result = subprocess.run(
        ["sudo", "bash", str(SCRIPT_DIR / "add-client.sh"), name],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        raise HTTPException(status_code=500, detail=result.stderr[-2000:])
    conf_path = SCRIPT_DIR.parent / "clients" / f"{name}.conf"
    conf_text = Path(f"/etc/wireguard/clients/{name}.conf").read_text()
    return {"name": name, "config": conf_text}


@app.delete("/clients/{name}")
def remove_client(name: str, authorization: str | None = Header(None)):
    _check_auth(authorization)
    _check_name(name)
    result = subprocess.run(
        ["sudo", "bash", str(SCRIPT_DIR / "remove-client.sh"), name],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        raise HTTPException(status_code=500, detail=result.stderr[-2000:])
    return {"name": name, "removed": True}
