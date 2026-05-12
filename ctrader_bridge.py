import asyncio
import json
import os
import threading
import time
from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Set

import websockets
from ctrader_open_api import Client, EndPoints, Protobuf, TcpProtocol
from ctrader_open_api.messages.OpenApiCommonMessages_pb2 import ProtoHeartbeatEvent
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor


@dataclass
class SymbolState:
    symbol_id: int
    broker_symbol: str
    app_symbol: str
    category: str
    digits: int = 5
    last_bid: Optional[float] = None
    last_ask: Optional[float] = None


APP_CLIENT_ID = os.getenv("CTRADER_CLIENT_ID", "").strip()
APP_CLIENT_SECRET = os.getenv("CTRADER_CLIENT_SECRET", "").strip()
ACCESS_TOKEN = os.getenv("CTRADER_ACCESS_TOKEN", "").strip()
ACCOUNT_ID_TEXT = os.getenv("CTRADER_ACCOUNT_ID", "").strip()
HOST_TYPE = os.getenv("CTRADER_HOST_TYPE", "demo").strip().lower()
BRIDGE_HOST = os.getenv("CTRADER_BRIDGE_HOST", "0.0.0.0").strip()
BRIDGE_PORT = int(os.getenv("CTRADER_BRIDGE_PORT", "8082"))
PRICE_SCALE = float(os.getenv("CTRADER_PRICE_SCALE", "100000"))

DEFAULT_SYMBOL_MAP = {
    "EURUSD": ["EURUSD", "EUR/USD"],
    "GBPUSD": ["GBPUSD", "GBP/USD"],
    "USDJPY": ["USDJPY", "USD/JPY"],
    "USDCHF": ["USDCHF", "USD/CHF"],
    "AUDUSD": ["AUDUSD", "AUD/USD"],
    "XAUUSD": ["XAUUSD", "XAU/USD", "GOLD"],
    "XAGUSD": ["XAGUSD", "XAG/USD", "SILVER"],
    "USOIL": ["USOIL", "Crude-F", "XTIUSD", "WTI", "WTIUSD", "US OIL", "USOil"],
    "DXY": ["DXY", "USDX", "US Dollar Index"],
    "NAS100": ["NAS100", "US100", "USTEC", "US Tech 100", "US 100"],
    "US30": ["US30", "DJI", "Wall Street 30", "US Dow 30"],
    "SPX500": ["SPX500", "US500", "US SPX 500", "US 500"],
    "AAPL": ["AAPL.US", "AAPL", "APPLE"],
    "MSFT": ["MSFT.US", "MSFT", "MICROSOFT"],
    "AMZN": ["AMZN.US", "AMZN", "AMAZON"],
    "NVDA": ["NVDA.US", "NVDA", "NVIDIA"],
    "TSLA": ["TSLA.US", "TSLA", "TESLA"],
    "BTCUSD": ["BTCUSD", "BTC/USD", "BITCOIN"],
    "ETHUSD": ["ETHUSD", "ETH/USD", "ETHEREUM"],
}

APP_CATEGORY = {
    "EURUSD": "FOREX",
    "GBPUSD": "FOREX",
    "USDJPY": "FOREX",
    "USDCHF": "FOREX",
    "AUDUSD": "FOREX",
    "XAUUSD": "COMMODITIES",
    "XAGUSD": "COMMODITIES",
    "USOIL": "COMMODITIES",
    "DXY": "COMMODITIES",
    "NAS100": "INDICES",
    "US30": "INDICES",
    "SPX500": "INDICES",
    "AAPL": "STOCKS",
    "MSFT": "STOCKS",
    "AMZN": "STOCKS",
    "NVDA": "STOCKS",
    "TSLA": "STOCKS",
    "BTCUSD": "CRYPTO",
    "ETHUSD": "CRYPTO",
}

account_id: Optional[int] = int(ACCOUNT_ID_TEXT) if ACCOUNT_ID_TEXT.isdigit() else None
client: Optional[Client] = None
ws_loop: Optional[asyncio.AbstractEventLoop] = None
android_clients: Set[object] = set()
state_lock = threading.RLock()

application_authed = False
account_authed = False
symbols_loaded = False
broker_symbols_by_normalized: Dict[str, object] = {}
symbol_states_by_id: Dict[int, SymbolState] = {}
subscribed_symbol_ids: Set[int] = set()
pending_app_symbols: Set[str] = set()
last_status = "starting"
last_status_message = "Starting cTrader bridge"


def normalize_symbol(symbol: str) -> str:
    return "".join(ch for ch in str(symbol).upper() if ch.isalnum())


def display_symbol(symbol: str) -> str:
    normalized = normalize_symbol(symbol)
    slash_map = {
        "EURUSD": "EUR/USD",
        "GBPUSD": "GBP/USD",
        "USDJPY": "USD/JPY",
        "USDCHF": "USD/CHF",
        "AUDUSD": "AUD/USD",
        "XAUUSD": "XAU/USD",
        "XAGUSD": "XAG/USD",
    }
    return slash_map.get(normalized, normalized)


def load_symbol_map() -> Dict[str, List[str]]:
    mapping = {key: list(values) for key, values in DEFAULT_SYMBOL_MAP.items()}
    raw = os.getenv("CTRADER_SYMBOL_MAP", "").strip()
    if not raw:
        return mapping
    try:
        parsed = json.loads(raw)
        if isinstance(parsed, dict):
            for key, values in parsed.items():
                normalized_key = normalize_symbol(key)
                if isinstance(values, list):
                    mapping[normalized_key] = [str(item) for item in values]
                else:
                    mapping[normalized_key] = [str(values)]
    except Exception as exc:
        print(f"Invalid CTRADER_SYMBOL_MAP JSON: {exc}")
    return mapping


SYMBOL_MAP = load_symbol_map()


def send_request(request) -> None:
    if client is None:
        print("cTrader client is not initialized")
        return
    deferred = client.send(request)
    deferred.addErrback(on_error)


def set_status(status: str, message: str) -> None:
    global last_status, last_status_message
    last_status = status
    last_status_message = message
    print(f"[cTrader] {status}: {message}")
    broadcast({"type": "status", "source": "pepperstone_ctrader", "state": status, "message": message})


def status_payload() -> dict:
    return {
        "type": "status",
        "source": "pepperstone_ctrader",
        "state": last_status,
        "message": last_status_message,
        "applicationAuthed": application_authed,
        "accountAuthed": account_authed,
        "symbolsLoaded": symbols_loaded,
        "accountId": account_id,
    }


async def send_to_android_clients(text: str) -> None:
    stale = []
    for socket in list(android_clients):
        try:
            await socket.send(text)
        except Exception:
            stale.append(socket)
    for socket in stale:
        android_clients.discard(socket)


def broadcast(payload: dict) -> None:
    loop = ws_loop
    if loop is None or loop.is_closed():
        return
    text = json.dumps(payload, separators=(",", ":"))
    loop.call_soon_threadsafe(lambda: asyncio.create_task(send_to_android_clients(text)))


def connected(_client) -> None:
    set_status("connected", "Connected to cTrader Open API demo endpoint")
    request = ProtoOAApplicationAuthReq()
    request.clientId = APP_CLIENT_ID
    request.clientSecret = APP_CLIENT_SECRET
    send_request(request)


def disconnected(_client, reason) -> None:
    global application_authed, account_authed, symbols_loaded
    application_authed = False
    account_authed = False
    symbols_loaded = False
    set_status("disconnected", str(reason))


def on_error(failure) -> None:
    set_status("error", str(failure))


def on_message_received(_client, message) -> None:
    global application_authed, account_authed, account_id, symbols_loaded
    if message.payloadType == ProtoHeartbeatEvent().payloadType:
        return

    payload = Protobuf.extract(message)

    if message.payloadType == ProtoOAApplicationAuthRes().payloadType:
        application_authed = True
        set_status("application_authenticated", "cTrader application authenticated")
        if account_id is None:
            request = ProtoOAGetAccountListByAccessTokenReq()
            request.accessToken = ACCESS_TOKEN
            send_request(request)
        else:
            send_account_auth()
        return

    if message.payloadType == ProtoOAGetAccountListByAccessTokenRes().payloadType:
        accounts = list(getattr(payload, "ctidTraderAccount", []))
        if not accounts:
            set_status("error", "No cTrader accounts returned for access token")
            return
        account_id = int(getattr(accounts[0], "ctidTraderAccountId"))
        set_status("account_selected", f"Selected cTrader account {account_id}")
        send_account_auth()
        return

    if message.payloadType == ProtoOAAccountAuthRes().payloadType:
        account_authed = True
        set_status("account_authenticated", f"cTrader account {account_id} authenticated")
        request_symbols()
        return

    if message.payloadType == ProtoOASymbolsListRes().payloadType:
        load_broker_symbols(payload)
        symbols_loaded = True
        set_status("symbols_loaded", f"Loaded {len(broker_symbols_by_normalized)} broker symbols")
        subscribe_pending_symbols()
        return

    if message.payloadType == ProtoOASubscribeSpotsRes().payloadType:
        set_status("subscribed", f"Subscribed to {len(subscribed_symbol_ids)} Pepperstone cTrader symbols")
        return

    if message.payloadType == ProtoOASpotEvent().payloadType:
        handle_spot_event(payload)
        return

    if message.payloadType == ProtoOAErrorRes().payloadType:
        set_status("error", str(payload))
        return


def send_account_auth() -> None:
    if account_id is None:
        set_status("error", "CTRADER_ACCOUNT_ID is missing and account auto-discovery failed")
        return
    request = ProtoOAAccountAuthReq()
    request.ctidTraderAccountId = account_id
    request.accessToken = ACCESS_TOKEN
    send_request(request)


def request_symbols() -> None:
    if account_id is None:
        return
    request = ProtoOASymbolsListReq()
    request.ctidTraderAccountId = account_id
    request.includeArchivedSymbols = False
    send_request(request)


def load_broker_symbols(payload) -> None:
    with state_lock:
        broker_symbols_by_normalized.clear()
        for item in getattr(payload, "symbol", []):
            symbol_name = str(
                getattr(item, "symbolName", "")
                or getattr(item, "displayName", "")
                or getattr(item, "name", "")
            ).strip()
            symbol_id = int(getattr(item, "symbolId", 0) or 0)
            if symbol_id <= 0 or not symbol_name:
                continue
            broker_symbols_by_normalized[normalize_symbol(symbol_name)] = item


def resolve_broker_symbol(app_symbol: str):
    normalized_app_symbol = normalize_symbol(app_symbol)
    candidates = [app_symbol] + SYMBOL_MAP.get(normalized_app_symbol, [])
    with state_lock:
        for candidate in candidates:
            item = broker_symbols_by_normalized.get(normalize_symbol(candidate))
            if item is not None:
                return item
    return None


def subscribe_app_symbols(symbols: Iterable[str]) -> None:
    normalized = [normalize_symbol(symbol) for symbol in symbols if normalize_symbol(symbol)]
    with state_lock:
        pending_app_symbols.update(normalized)
    subscribe_pending_symbols()


def subscribe_pending_symbols() -> None:
    if not account_authed or not symbols_loaded or account_id is None:
        return

    new_ids: List[int] = []
    unresolved: List[str] = []

    with state_lock:
        for app_symbol in sorted(pending_app_symbols):
            item = resolve_broker_symbol(app_symbol)
            if item is None:
                unresolved.append(app_symbol)
                continue

            symbol_id = int(getattr(item, "symbolId", 0) or 0)
            if symbol_id <= 0 or symbol_id in subscribed_symbol_ids:
                continue

            broker_symbol = str(getattr(item, "symbolName", "") or getattr(item, "displayName", "") or app_symbol)
            digits = int(getattr(item, "digits", 5) or 5)
            symbol_states_by_id[symbol_id] = SymbolState(
                symbol_id=symbol_id,
                broker_symbol=broker_symbol,
                app_symbol=app_symbol,
                category=APP_CATEGORY.get(app_symbol, "FOREX"),
                digits=digits,
            )
            subscribed_symbol_ids.add(symbol_id)
            new_ids.append(symbol_id)

    if unresolved:
        print(f"Unresolved cTrader symbols: {', '.join(unresolved)}")

    if not new_ids:
        return

    request = ProtoOASubscribeSpotsReq()
    request.ctidTraderAccountId = account_id
    for symbol_id in new_ids:
        request.symbolId.append(symbol_id)
    request.subscribeToSpotTimestamp = True
    send_request(request)


def first_present(payload, names: Iterable[str]) -> Optional[float]:
    for name in names:
        if hasattr(payload, name):
            value = getattr(payload, name)
            try:
                numeric = float(value)
            except Exception:
                continue
            if numeric > 0:
                return numeric
    return None


def decode_price(raw: Optional[float], digits: int) -> Optional[float]:
    if raw is None or raw <= 0:
        return None
    if raw > 10000:
        return raw / PRICE_SCALE
    if digits > 0 and raw > 1000 and PRICE_SCALE <= 0:
        return raw / (10 ** digits)
    return raw


def handle_spot_event(payload) -> None:
    symbol_id = int(getattr(payload, "symbolId", 0) or 0)
    with state_lock:
        state = symbol_states_by_id.get(symbol_id)
    if state is None:
        return

    raw_bid = first_present(payload, ["bid", "relativeBid", "currentBid"])
    raw_ask = first_present(payload, ["ask", "relativeAsk", "currentAsk"])
    bid = decode_price(raw_bid, state.digits) if raw_bid is not None else state.last_bid
    ask = decode_price(raw_ask, state.digits) if raw_ask is not None else state.last_ask

    if bid is None and ask is None:
        return
    if bid is None:
        bid = ask
    if ask is None:
        ask = bid
    if bid is None or ask is None or bid <= 0 or ask <= 0:
        return

    state.last_bid = bid
    state.last_ask = ask
    price = (bid + ask) / 2.0
    event_time = int(
        getattr(payload, "timestamp", 0)
        or getattr(payload, "spotTimestamp", 0)
        or int(time.time() * 1000)
    )

    broadcast(
        {
            "type": "tick",
            "source": "pepperstone_ctrader",
            "symbol": state.app_symbol,
            "displaySymbol": display_symbol(state.app_symbol),
            "brokerSymbol": state.broker_symbol,
            "category": state.category,
            "bid": bid,
            "ask": ask,
            "price": price,
            "time": event_time,
        }
    )


def run_ctrader_client() -> None:
    global client
    if not APP_CLIENT_ID or not APP_CLIENT_SECRET or not ACCESS_TOKEN:
        set_status("error", "Set CTRADER_CLIENT_ID, CTRADER_CLIENT_SECRET and CTRADER_ACCESS_TOKEN before starting")
        return

    print(f"[DEBUG] CLIENT_ID length: {len(APP_CLIENT_ID)}")
    print(f"[DEBUG] CLIENT_SECRET length: {len(APP_CLIENT_SECRET)}")
    print(f"[DEBUG] ACCESS_TOKEN length: {len(ACCESS_TOKEN)}")
    print(f"[DEBUG] ACCOUNT_ID: {account_id}")
    print(f"[DEBUG] HOST_TYPE: {HOST_TYPE}")

    host = EndPoints.PROTOBUF_LIVE_HOST if HOST_TYPE == "live" else EndPoints.PROTOBUF_DEMO_HOST
    print(f"[DEBUG] Connecting to: {host}:{EndPoints.PROTOBUF_PORT}")
    client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
    client.setConnectedCallback(connected)
    client.setDisconnectedCallback(disconnected)
    client.setMessageReceivedCallback(on_message_received)
    client.startService()
    reactor.run(installSignalHandlers=False)


async def handle_android_client(websocket, *_args) -> None:
    android_clients.add(websocket)
    try:
        await websocket.send(json.dumps(status_payload(), separators=(",", ":")))
    except Exception:
        android_clients.discard(websocket)
        return
    
    try:
        async for message in websocket:
            try:
                payload = json.loads(message)
            except Exception:
                try:
                    await websocket.send(json.dumps({"type": "error", "message": "Invalid JSON"}))
                except Exception:
                    break
                continue

            action = str(payload.get("action", "")).lower()
            if action == "subscribe":
                symbols = payload.get("symbols")
                if not isinstance(symbols, list):
                    symbol = payload.get("symbol")
                    symbols = [symbol] if symbol else []
                reactor.callFromThread(subscribe_app_symbols, [str(symbol) for symbol in symbols])
                try:
                    await websocket.send(json.dumps({"type": "ack", "action": "subscribe", "symbols": symbols}))
                except Exception:
                    break
            elif action == "ping":
                try:
                    await websocket.send(json.dumps({"type": "pong", "time": int(time.time() * 1000)}))
                except Exception:
                    break
            else:
                try:
                    await websocket.send(json.dumps({"type": "error", "message": f"Unsupported action: {action}"}))
                except Exception:
                    break
    except Exception:
        pass
    finally:
        android_clients.discard(websocket)


async def main() -> None:
    global ws_loop
    ws_loop = asyncio.get_running_loop()
    threading.Thread(target=run_ctrader_client, daemon=True).start()
    print(f"Pepperstone cTrader bridge listening on ws://{BRIDGE_HOST}:{BRIDGE_PORT}")
    async with websockets.serve(handle_android_client, BRIDGE_HOST, BRIDGE_PORT):
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
