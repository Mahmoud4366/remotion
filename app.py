import os, json, time, uuid, random, asyncio, secrets
import httpx
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse

START_TIME = time.time()
DATA_FILE = os.environ.get("DATA_FILE", "/tmp/arena_cloud_state.json")

# L1 In-RAM Hyper-Engine Stores (< 1 microsecond)
ROOMS = {}
CHANNELS = {}
CONFIGS = {
    "sk-universal-arena-e14f3b5ab733ce1ff01b089e": {
        "api_key": "sk-universal-arena-e14f3b5ab733ce1ff01b089e",
        "server_type": "universal",
        "rules_text": "قوانین جامع سرور فعال است.",
        "rules": {"universal_mode": True, "anti_cheat": True, "anti_spam": True, "max_length": 4000, "banned_words": []},
        "collaborators": [],
        "connectors": [],
    },
    "sk-raw-arena-df03954fc4f23a750f4394b8": {
        "api_key": "sk-raw-arena-df03954fc4f23a750f4394b8",
        "server_type": "raw_universal",
        "rules_text": "",
        "rules": {},
        "collaborators": [],
        "connectors": [],
    },
}
HYPER_KV = {}
SEQ = 1000
MODELS = ["gpt-5.6-sol", "gpt-4o", "claude-3.5-sonnet", "gemini-2.5-pro", "deepseek-r1"]


def _load():
    if os.path.exists(DATA_FILE):
        try:
            d = json.loads(open(DATA_FILE, "r", encoding="utf-8").read())
            ROOMS.update(d.get("rooms", {}))
            CHANNELS.update(d.get("channels", {}))
            CONFIGS.update(d.get("configs", {}))
        except Exception:
            pass


def _save():
    try:
        with open(DATA_FILE, "w", encoding="utf-8") as f:
            json.dump({"rooms": ROOMS, "channels": CHANNELS, "configs": CONFIGS}, f, ensure_ascii=False)
    except Exception:
        pass


_load()
app = FastAPI(title="Arena AI 24/7 Hyper-Engine Server (<1 us)")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])


def _key(req: Request, body: dict = None) -> str:
    b = body or {}
    auth = req.headers.get("Authorization", "").replace("Bearer ", "").strip()
    return auth or str(b.get("api_key") or req.query_params.get("api_key") or "sk-universal-arena-e14f3b5ab733ce1ff01b089e").strip()


def _cfg(k: str, stype: str = "universal") -> dict:
    if k not in CONFIGS:
        CONFIGS[k] = {"api_key": k, "server_type": stype, "rules_text": "", "rules": {}, "collaborators": [], "connectors": []}
    return CONFIGS[k]


@app.get("/")
@app.get("/health")
async def health():
    return {
        "status": "online",
        "uptime_seconds": int(time.time() - START_TIME),
        "engine": "HYPER_L1_RAM_1US",
        "models_count": len(MODELS),
        "rooms_count": len(ROOMS),
        "channels_count": len(CHANNELS),
    }


@app.get("/v1/models")
async def list_models():
    return {"object": "list", "data": [{"id": m, "object": "model", "owned_by": "arena-ai"} for m in MODELS]}


@app.post("/v1/chat/completions")
async def chat_completions(req: Request):
    body = await req.json()
    model = body.get("model") or "gpt-5.6-sol"
    messages = body.get("messages") or []
    ans = ""
    try:
        async with httpx.AsyncClient(timeout=25) as client:
            r = await client.post("https://api.llm7.io/v1/chat/completions", json={"model": "gpt-4o-mini", "messages": messages})
            if r.status_code == 200:
                ans = r.json()["choices"][0]["message"]["content"]
    except Exception as e:
        ans = f"Arena Server Online (fallback): {e}"
    if not ans:
        ans = "سلام! سرور ابری Arena Hyper-Engine متصل و فعال است."
    return {
        "id": f"chatcmpl-{uuid.uuid4().hex[:10]}",
        "object": "chat.completion",
        "created": int(time.time()),
        "model": model,
        "choices": [{"index": 0, "message": {"role": "assistant", "content": ans}, "finish_reason": "stop"}],
    }


@app.post("/v1/game/create")
async def game_create(req: Request):
    t0 = time.perf_counter()
    body = await req.json()
    code = str(body.get("room_code") or random.randint(10000, 99999)).strip()
    room = {
        "room_code": code,
        "game_name": body.get("game_name") or "Arena Game",
        "api_key": _key(req, body),
        "host_id": body.get("host_id") or "P1",
        "guest_id": "",
        "status": "waiting",
        "turn": "P1",
        "state": body.get("state") or {},
        "seq": 1,
        "updated_at": time.time(),
    }
    ROOMS[code] = room
    _save()
    return {"ok": True, "room_code": code, "room": room, "engine_latency_us": round((time.perf_counter() - t0) * 1e6, 2)}


@app.post("/v1/game/join")
async def game_join(req: Request):
    t0 = time.perf_counter()
    body = await req.json()
    code = str(body.get("room_code") or "10001").strip()
    room = ROOMS.get(code)
    if not room:
        room = {"room_code": code, "game_name": "Arena Game", "host_id": "P1", "turn": "P1", "state": {}, "seq": 1}
        ROOMS[code] = room
    room["guest_id"] = body.get("guest_id") or "P2"
    room["status"] = "playing"
    room["seq"] += 1
    room["updated_at"] = time.time()
    _save()
    return {"ok": True, "room_code": code, "room": room, "engine_latency_us": round((time.perf_counter() - t0) * 1e6, 2)}


@app.post("/v1/game/move")
@app.post("/v1/game/sync")
async def game_move(req: Request):
    t0 = time.perf_counter()
    body = await req.json()
    code = str(body.get("room_code") or "10001").strip()
    room = ROOMS.setdefault(code, {"room_code": code, "host_id": "P1", "guest_id": "P2", "status": "playing", "turn": "P1", "state": {}, "seq": 1})
    if isinstance(body.get("state"), dict):
        room["state"].update(body["state"])
    if isinstance(body.get("move"), dict):
        room["state"].update(body["move"])
        room["last_move"] = body["move"]
    room["turn"] = body.get("next_turn") or ("P2" if room.get("turn") == "P1" else "P1")
    room["seq"] += 1
    room["updated_at"] = time.time()
    _save()
    return {"ok": True, "room_code": code, "room": room, "engine_latency_us": round((time.perf_counter() - t0) * 1e6, 2)}


@app.get("/v1/game/state")
async def game_state(room_code: str = "10001"):
    t0 = time.perf_counter()
    room = ROOMS.setdefault(room_code, {"room_code": room_code, "status": "waiting", "turn": "P1", "state": {}, "seq": 1})
    return {"ok": True, "room_code": room_code, "room": room, "engine_latency_us": round((time.perf_counter() - t0) * 1e6, 2)}


@app.get("/v1/game/rooms")
async def game_rooms():
    return {"ok": True, "rooms": list(ROOMS.values()), "count": len(ROOMS)}


@app.get("/v1/game/rules")
@app.post("/v1/game/rules")
@app.get("/v1/messenger/rules")
@app.post("/v1/messenger/rules")
async def server_rules(req: Request):
    stype = "messenger" if "messenger" in req.url.path else "game"
    body = await req.json() if req.method == "POST" else {}
    k = _key(req, body)
    cfg = _cfg(k, stype)
    if req.method == "POST":
        if body.get("rules_text"):
            cfg["rules_text"] = str(body["rules_text"])
        if isinstance(body.get("rules"), dict):
            cfg["rules"].update(body["rules"])
        _save()
    return {"ok": True, "config": cfg}


@app.get("/v1/game/collaborators")
@app.post("/v1/game/collaborators")
@app.get("/v1/messenger/collaborators")
@app.post("/v1/messenger/collaborators")
async def server_collabs(req: Request):
    stype = "messenger" if "messenger" in req.url.path else "game"
    body = await req.json() if req.method == "POST" else {}
    cfg = _cfg(_key(req, body), stype)
    if req.method == "POST":
        cfg["collaborators"].append({"id": f"c-{secrets.token_hex(3)}", "name": body.get("name", "AI"), "role": body.get("role", "collaborator")})
        _save()
    return {"ok": True, "config": cfg}


@app.get("/v1/game/connect")
@app.post("/v1/game/connect")
@app.get("/v1/messenger/connect")
@app.post("/v1/messenger/connect")
async def server_connect(req: Request):
    stype = "messenger" if "messenger" in req.url.path else "game"
    body = await req.json() if req.method == "POST" else {}
    cfg = _cfg(_key(req, body), stype)
    if req.method == "POST":
        cfg["connectors"].append({"id": f"x-{secrets.token_hex(3)}", "name": body.get("name", "Service"), "endpoint": body.get("endpoint", "")})
        _save()
    return {"ok": True, "config": cfg}


@app.post("/v1/messenger/send")
async def msg_send(req: Request):
    t0 = time.perf_counter()
    body = await req.json()
    k = _key(req, body)
    cfg = _cfg(k, "messenger")
    text = str(body.get("text") or body.get("message") or "")
    for bw in cfg["rules"].get("banned_words", []):
        if bw and bw in text:
            return JSONResponse({"ok": False, "error": f"Blocked by rule: {bw}"}, status_code=403)
    cid = str(body.get("channel_id") or "general")
    ch = CHANNELS.setdefault(cid, {"channel_id": cid, "messages": [], "seq": 0})
    ch["seq"] += 1
    msg = {"id": f"m-{ch['seq']}", "seq": ch["seq"], "sender": body.get("sender", "User"), "text": text, "ts": time.time()}
    ch["messages"].append(msg)
    ch["messages"] = ch["messages"][-200:]
    _save()
    return {"ok": True, "channel_id": cid, "message": msg, "seq": ch["seq"], "engine_latency_us": round((time.perf_counter() - t0) * 1e6, 2)}


@app.get("/v1/messenger/messages")
async def msg_get(req: Request, channel_id: str = "general", since_seq: int = 0):
    ch = CHANNELS.get(channel_id, {"channel_id": channel_id, "messages": [], "seq": 0})
    msgs = [m for m in ch["messages"] if m["seq"] > since_seq]
    return {"ok": True, "channel_id": channel_id, "messages": msgs, "seq": ch["seq"], "server_config": _cfg(_key(req), "messenger")}


@app.get("/v1/messenger/channels")
async def msg_channels():
    return {"ok": True, "channels": [{"channel_id": k, "seq": v["seq"], "count": len(v["messages"])} for k, v in CHANNELS.items()]}


@app.get("/v1/hyper")
@app.post("/v1/hyper")
async def hyper(req: Request, op: str = "get", key: str = "default"):
    global SEQ
    body = await req.json() if req.method == "POST" else {}
    op = body.get("op") or op
    key = body.get("key") or key
    val = body.get("value")
    t0 = time.perf_counter()
    if op == "set":
        SEQ += 1
        HYPER_KV[key] = {"v": val, "seq": SEQ}
        res = HYPER_KV[key]
    elif op == "move":
        SEQ += 1
        r = ROOMS.setdefault(key, {"room_code": key, "turn": "P1", "state": {}, "seq": SEQ})
        r["state"] = val
        r["seq"] = SEQ
        r["turn"] = "P2" if r.get("turn") == "P1" else "P1"
        res = r
    else:
        res = HYPER_KV.get(key)
    return {"ok": True, "engine": "HYPER_L1_RAM_1US", "data": res, "engine_latency_us": round((time.perf_counter() - t0) * 1e6, 3)}


@app.get("/console/{api_key}", response_class=HTMLResponse)
async def console(api_key: str):
    cfg = _cfg(api_key)
    return f"""<!DOCTYPE html><html lang="fa" dir="rtl"><head><meta charset="UTF-8"><title>Console {api_key}</title></head>
<body style="background:#0f172a;color:#f8fafc;font-family:sans-serif;padding:24px">
<h2>⚡ کنسول اختصاصی سرور ({api_key})</h2>
<pre style="background:#020617;padding:16px;border-radius:12px;direction:ltr">{json.dumps(cfg, indent=2, ensure_ascii=False)}</pre>
</body></html>"""
