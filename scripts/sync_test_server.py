#!/usr/bin/env python3
"""
Ncy sync — reference test server.

Verifies the phase-1 wire protocol end-to-end. Run on a laptop on the same LAN as
the phone, paste the pairing JSON it prints into the Ncy "Sync devices" screen,
keep Ncy in the foreground, and watch events stream into the terminal.

Requirements:
  pip install websockets qrcode

Usage:
  python sync_test_server.py [--host 0.0.0.0] [--port 8765] [--name "My Laptop"]

  # Phase-2 inbound test: every TEXT the phone sends triggers a reply event from
  # the desktop. Use to verify the phone's receive path end-to-end.
  python sync_test_server.py --reply "hi from desktop"

The server is intentionally minimal: it authenticates clients via HMAC of a server-
issued nonce and prints every event it receives. It is *not* a real desktop client
and stores nothing.
"""

from __future__ import annotations

import argparse
import asyncio
import base64
import hmac
import json
import secrets
import socket
import sys
import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

import websockets

try:
    import qrcode  # optional, for terminal QR
except ImportError:
    qrcode = None  # type: ignore


WIRE_VERSION = 1


@dataclass
class ServerIdentity:
    device_id: str
    name: str
    secret_b64: str
    host: str
    port: int


@dataclass
class HlcClock:
    """Phase 2 helper: tracks the server's monotonic HLC across outbound events."""
    node_id: str
    physical_ms: int = 0
    logical: int = 0

    def tick(self, now_ms: int) -> dict:
        if now_ms > self.physical_ms:
            self.physical_ms = now_ms
            self.logical = 0
        else:
            self.logical += 1
        return {
            "physicalMs": self.physical_ms,
            "logical": self.logical,
            "nodeId": self.node_id,
        }

    def receive(self, remote: dict, now_ms: int) -> None:
        """Absorb a remote HLC so subsequent ticks stay causally ahead."""
        max_p = max(now_ms, self.physical_ms, int(remote.get("physicalMs", 0)))
        if max_p == self.physical_ms == int(remote.get("physicalMs", 0)):
            self.logical = max(self.logical, int(remote.get("logical", 0))) + 1
        elif max_p == self.physical_ms:
            self.logical += 1
        elif max_p == int(remote.get("physicalMs", 0)):
            self.logical = int(remote.get("logical", 0)) + 1
        else:
            self.logical = 0
        self.physical_ms = max_p


@dataclass
class ReplyConfig:
    """Echo-bot mode: when phone sends a TEXT message, server replies with this text."""
    text: str
    clock: HlcClock


def _ip_rank(ip: str) -> int:
    """Lower is better. Prefers RFC1918 home ranges over uncommon LAN/VPN ranges."""
    if ip.startswith("192.168."):
        return 0
    parts = ip.split(".")
    if (
        ip.startswith("172.")
        and len(parts) == 4
        and parts[1].isdigit()
        and 16 <= int(parts[1]) <= 31
    ):
        return 1
    if ip.startswith("10."):
        # 10.x is RFC1918 too, but Tailscale/many VPNs live here, so rank below 192.168/172.16.
        return 2
    return 3


def get_all_lan_ips() -> list[str]:
    """Enumerate every non-loopback IPv4 visible to this host, ranked by likely LAN-ness."""
    ips: set[str] = set()
    try:
        for ip in socket.gethostbyname_ex(socket.gethostname())[2]:
            if ip and not ip.startswith("127."):
                ips.add(ip)
    except OSError:
        pass
    # Also include the address used to reach the public internet — often the same one,
    # but this catches multi-homed hosts where gethostbyname_ex misses an interface.
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        if ip and not ip.startswith("127."):
            ips.add(ip)
        s.close()
    except OSError:
        pass
    return sorted(ips, key=lambda ip: (_ip_rank(ip), ip))


def detect_local_ip() -> str:
    candidates = get_all_lan_ips()
    return candidates[0] if candidates else "127.0.0.1"


def build_pairing_payload(identity: ServerIdentity) -> str:
    return json.dumps(
        {
            "v": WIRE_VERSION,
            "deviceId": identity.device_id,
            "name": identity.name,
            "host": identity.host,
            "port": identity.port,
            "secret": identity.secret_b64,
        }
    )


def print_qr(text: str) -> None:
    if qrcode is None:
        return
    q = qrcode.QRCode(border=1)
    q.add_data(text)
    q.make(fit=True)
    q.print_ascii(invert=True)


def save_qr_png(text: str, path: str) -> bool:
    """Write the pairing QR to a PNG file. Returns True on success."""
    if qrcode is None:
        return False
    try:
        img = qrcode.make(text)
        img.save(path)
        return True
    except Exception:
        return False


def verify_auth(secret_b64: str, nonce_b64: str, signature_b64: str) -> bool:
    try:
        secret = base64.b64decode(secret_b64)
        nonce = base64.b64decode(nonce_b64)
        provided = base64.b64decode(signature_b64)
    except (ValueError, TypeError):
        return False
    expected = hmac.new(secret, nonce, "sha256").digest()
    return hmac.compare_digest(expected, provided)


def build_reply_event(server_id: str, chat_id: str, text: str, clock: HlcClock) -> dict:
    """Author a desktop-side MessageCreated event in response to an inbound message."""
    now_ms = int(time.time() * 1000)
    msg_id = str(uuid.uuid4())
    return {
        "v": 1,
        "type": "MessageCreated",
        "eventId": str(uuid.uuid4()),
        "hlc": clock.tick(now_ms),
        "originDevice": server_id,
        "chatId": chat_id,
        "payload": {
            "id": msg_id,
            "chatId": chat_id,
            "content": [
                {
                    "type": "PARAGRAPH",
                    "spans": [{"text": text}],
                }
            ],
            "voiceFile": None,
            "voiceDurationMs": None,
            "createdAt": now_ms,
            "editedAt": None,
            "type": "TEXT",
            "isStarred": False,
            "imageFiles": [],
            "tags": [],
            "reminderAt": None,
            "checklistItems": [],
        },
    }


def format_event_summary(event: dict) -> str:
    op_type = event.get("type", "?")
    chat_id = event.get("chatId", "?")[:8]
    hlc = event.get("hlc", {})
    hlc_str = f"{hlc.get('physicalMs', 0)}.{hlc.get('logical', 0)}"
    payload = event.get("payload", {}) or {}
    detail = ""
    if op_type == "MessageCreated" or op_type == "MessageEdited":
        msg = payload
        text_blocks = msg.get("content", [])
        flat = " ".join(
            span.get("text", "")
            for block in text_blocks
            for span in block.get("spans", [])
        )
        detail = flat[:60]
    elif op_type == "MessageTombstoned":
        detail = f"id={payload.get('messageId', '?')[:8]}"
    elif op_type == "ChatMetadataChanged":
        detail = payload.get("name", "?")
    return f"  [{hlc_str}] chat={chat_id} {op_type:24s} {detail}"


async def session(
    websocket,
    identity: ServerIdentity,
    peer_label: str,
    reply: Optional[ReplyConfig] = None,
) -> None:
    nonce = base64.b64encode(secrets.token_bytes(16)).decode("ascii")
    await websocket.send(json.dumps({"type": "Challenge", "nonce": nonce}))

    hello_raw = await websocket.recv()
    try:
        hello = json.loads(hello_raw)
    except json.JSONDecodeError:
        await websocket.send(
            json.dumps({"type": "Error", "code": "bad_hello", "message": "not JSON"})
        )
        return

    if hello.get("type") != "Hello":
        await websocket.send(
            json.dumps(
                {"type": "Error", "code": "bad_hello", "message": "expected Hello"}
            )
        )
        return

    if not verify_auth(identity.secret_b64, nonce, hello.get("auth", "")):
        await websocket.send(
            json.dumps(
                {"type": "Error", "code": "bad_auth", "message": "HMAC mismatch"}
            )
        )
        return

    phone_id = hello.get("deviceId", "<unknown>")
    since = hello.get("sinceHlc")
    print(
        f"\n=== Phone {phone_id[:8]} ({peer_label}) authenticated. since={since}",
        flush=True,
    )
    if reply is not None:
        print(
            f"    Echo-reply ON: every TEXT message → desktop sends back '{reply.text}'",
            flush=True,
        )

    await websocket.send(
        json.dumps(
            {"type": "Welcome", "deviceId": identity.device_id, "name": identity.name}
        )
    )

    async for raw in websocket:
        try:
            msg = json.loads(raw)
        except json.JSONDecodeError:
            print(f"  [bad frame] {raw!r}", flush=True)
            continue

        msg_type = msg.get("type")
        if msg_type == "Event":
            event = msg.get("event", {})
            print(format_event_summary(event), flush=True)
            hlc = event.get("hlc")
            if hlc is not None:
                await websocket.send(json.dumps({"type": "Ack", "upToHlc": hlc}))
                if reply is not None:
                    reply.clock.receive(hlc, int(time.time() * 1000))

            # Echo back when phone sends a text message; demonstrates phase-2 inbound.
            if (
                reply is not None
                and event.get("type") == "MessageCreated"
                and (event.get("payload") or {}).get("type") == "TEXT"
            ):
                chat_id = event.get("chatId")
                if chat_id:
                    reply_event = build_reply_event(
                        identity.device_id, chat_id, reply.text, reply.clock
                    )
                    await websocket.send(
                        json.dumps({"type": "Event", "event": reply_event})
                    )
                    print(
                        f"  → sent reply event {reply_event['eventId'][:8]}",
                        flush=True,
                    )
        elif msg_type == "Ack":
            up = msg.get("upToHlc", {})
            print(
                f"  ← phone acked {up.get('physicalMs', 0)}.{up.get('logical', 0)}",
                flush=True,
            )
        else:
            print(f"  [ignored] {msg_type}", flush=True)


async def serve(identity: ServerIdentity, reply: Optional[ReplyConfig]) -> None:
    async def handler(websocket):
        peer = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
        try:
            await session(websocket, identity, peer, reply)
        except websockets.ConnectionClosed:
            pass
        finally:
            print(f"=== {peer} disconnected", flush=True)

    async with websockets.serve(handler, identity.host, identity.port):
        print(f"Listening on ws://{identity.host}:{identity.port}/sync", flush=True)
        print("Press Ctrl+C to stop.\n", flush=True)
        await asyncio.Future()  # run forever


def main() -> int:
    parser = argparse.ArgumentParser(description="Ncy sync test server")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument(
        "--name", default=socket.gethostname() or "Test Server",
        help="Friendly name shown on the phone"
    )
    parser.add_argument(
        "--ip", default=None,
        help="Override the IP advertised in the pairing JSON (defaults to detected LAN IP)"
    )
    parser.add_argument(
        "--qr-png", default="pairing-qr.png",
        help="Path to save the pairing QR as a PNG. Pass '' to skip."
    )
    parser.add_argument(
        "--reply", default=None,
        help=(
            "Phase-2 echo-bot mode: when the phone sends a TEXT message, the server "
            "authors a reply event with this string and pushes it back. Verifies the "
            "phone's inbound (receive) path."
        ),
    )
    args = parser.parse_args()

    candidates = get_all_lan_ips()
    advertised_ip = args.ip or (candidates[0] if candidates else "127.0.0.1")

    if candidates:
        print("Detected local IPv4 addresses (best guess first):", flush=True)
        for ip in candidates:
            mark = "  ← using"
            if ip != advertised_ip:
                mark = ""
                if ip.startswith("10."):
                    mark = "  (likely VPN — Tailscale/ZeroTier/etc.)"
            print(f"  {ip}{mark}", flush=True)
        if args.ip:
            print(f"  (overridden by --ip {args.ip})", flush=True)
        elif len(candidates) > 1:
            print(
                "  Wrong one selected? Re-run with --ip <addr> from the list above.",
                flush=True,
            )
        print("", flush=True)

    identity = ServerIdentity(
        device_id=str(uuid.uuid4()),
        name=args.name,
        secret_b64=base64.b64encode(secrets.token_bytes(32)).decode("ascii"),
        host=advertised_ip,
        port=args.port,
    )

    payload_json = build_pairing_payload(identity)
    print("Pairing JSON (or scan the QR below):\n", flush=True)
    print(payload_json + "\n", flush=True)
    if qrcode is not None:
        print_qr(payload_json)
        if args.qr_png:
            if save_qr_png(payload_json, args.qr_png):
                print(
                    f"\nQR also saved as {args.qr_png} — open it for an easier scan.\n",
                    flush=True,
                )
            else:
                print(
                    "\n(install Pillow — `pip install qrcode[pil]` — to also save the QR as a PNG)\n",
                    flush=True,
                )
    else:
        print("(install 'qrcode' to also render a QR here)\n", flush=True)

    listen_identity = ServerIdentity(
        device_id=identity.device_id,
        name=identity.name,
        secret_b64=identity.secret_b64,
        host=args.host,           # bind on whatever the user asked
        port=args.port,
    )

    reply: Optional[ReplyConfig] = None
    if args.reply is not None:
        reply = ReplyConfig(text=args.reply, clock=HlcClock(node_id=identity.device_id))

    try:
        asyncio.run(serve(listen_identity, reply))
    except KeyboardInterrupt:
        print("\nStopped.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
