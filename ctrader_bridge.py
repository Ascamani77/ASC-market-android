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
from ctrader_open_api.messages.OpenApiModelMessages_pb2 import (
    ProtoOATrendbarPeriod,
    ProtoOAOrderType,
    ProtoOATradeSide
)
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

# Track which endpoint we're using for fallback
current_endpoint_type = HOST_TYPE

DEFAULT_SYMBOL_MAP = {
    "EURUSD": ["EURUSD", "EUR/USD"],
    "GBPUSD": ["GBPUSD", "GBP/USD"],
    "USDJPY": ["USDJPY", "USD/JPY"],
    "USDCHF": ["USDCHF", "USD/CHF"],
    "AUDUSD": ["AUDUSD", "AUD/USD"],
    "XAUUSD": ["XAUUSD", "XAU/USD", "GOLD"],
    "XAGUSD": ["XAGUSD", "XAG/USD", "SILVER"],
    "USOIL": ["USOIL", "Crude-F", "XTIUSD", "WTI", "WTIUSD", "US OIL", "USOil"],
    "UKOIL": ["UKOIL", "Brent-F", "BRENTCMDUSD", "BRENT", "UK OIL", "UKOil"],
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
    "UKOIL": "COMMODITIES",
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
subscribed_live_trendbars: Dict[int, int] = {}
pending_app_symbols: Set[str] = set()
last_status = "starting"
last_status_message = "Starting cTrader bridge"
last_account_message: Optional[dict] = None  # Store last account message
pending_balance_snapshot: Optional[float] = None  # Balance before last execution event

# Reconnection and Watchdog state
reconnecting = False
last_tick_received_time = time.time()
heartbeat_interval = 25  # seconds
watchdog_interval = 60   # seconds


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
    print(f"[DEBUG] Sending request: {type(request).__name__}")
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
    if "POSITIONS" in text:
        print(f"[DEBUG] Broadcasting to {len(android_clients)} clients: {text[:200]}...")
    stale = []
    for socket in list(android_clients):
        try:
            await socket.send(text)
        except Exception as e:
            print(f"[DEBUG] Failed to send to client: {e}")
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
    set_status("connected", f"Connected to cTrader Open API {current_endpoint_type} endpoint")
    print(f"[DEBUG] Sending application auth request with clientId (len={len(APP_CLIENT_ID)}) and clientSecret (len={len(APP_CLIENT_SECRET)})")
    request = ProtoOAApplicationAuthReq()
    request.clientId = APP_CLIENT_ID
    request.clientSecret = APP_CLIENT_SECRET
    send_request(request)
    
    # Start heartbeat and watchdog if not already started
    reactor.callLater(heartbeat_interval, send_proactive_heartbeat)
    reactor.callLater(watchdog_interval, run_watchdog)


def send_proactive_heartbeat() -> None:
    """Send a heartbeat to keep the connection alive"""
    if client and client.protocol:
        # ProtoHeartbeatEvent is actually an event, but we can send a simple request to keep connection alive
        # A ProtoOATraderReq is a good lightweight request for this
        if account_authed and account_id:
            request_trader_info()
        else:
            # If not authed yet, just send a simple ping-like request
            request = ProtoOAVersionReq()
            send_request(request)
    
    # Schedule next heartbeat
    reactor.callLater(heartbeat_interval, send_proactive_heartbeat)


def run_watchdog() -> None:
    """Check if we've received ticks recently, if not, reconnect"""
    global last_tick_received_time, reconnecting
    
    # Only run watchdog if we are supposedly "live"
    if account_authed and symbols_loaded and not reconnecting:
        now = time.time()
        # If no ticks for 2 minutes, assume connection is dead
        if now - last_tick_received_time > 120:
            print(f"[WATCHDOG] DATA FREEZE DETECTED: No ticks for {int(now - last_tick_received_time)}s. Reconnecting...")
            perform_reconnect()
            return

    # Schedule next check
    reactor.callLater(watchdog_interval, run_watchdog)


def disconnected(_client, reason) -> None:
    global application_authed, account_authed, symbols_loaded, reconnecting
    application_authed = False
    account_authed = False
    symbols_loaded = False
    set_status("disconnected", str(reason))
    
    if not reconnecting:
        reconnecting = True
        print(f"[RECONNECT] Connection lost: {reason}. Retrying in 5 seconds...")
        reactor.callLater(5, perform_reconnect)


def perform_reconnect() -> None:
    global reconnecting
    reconnecting = False
    reconnect_with_endpoint(current_endpoint_type)


def on_error(failure) -> None:
    error_str = str(failure)
    print(f"[ERROR] Request failed: {error_str}")
    
    # Check for timeout errors
    if "TimeoutError" in error_str or "timed out" in error_str.lower():
        print("[ERROR] Request timed out - this may indicate:")
        print("  1. ACCESS_TOKEN is invalid or expired")
        print("  2. ACCOUNT_ID (47312778) doesn't match the access token")
        print("  3. Network connectivity issues to cTrader")
        print("  4. cTrader API is slow or unresponsive")
        if not account_authed:
            print("[ERROR] Account auth timed out - verify ACCESS_TOKEN and ACCOUNT_ID")
    
    set_status("error", error_str)


def on_message_received(_client, message) -> None:
    global application_authed, account_authed, account_id, symbols_loaded, current_endpoint_type
    if message.payloadType == ProtoHeartbeatEvent().payloadType:
        # On heartbeat, request updated account info if we have positions
        if account_authed and account_id:
            request_trader_info()
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
    
    if message.payloadType == ProtoOAErrorRes().payloadType:
        error_code = getattr(payload, "errorCode", "UNKNOWN")
        description = getattr(payload, "description", "")
        print(f"[ERROR] cTrader API Error: errorCode={error_code}, description={description}")
        print(f"[ERROR] Full error payload: {payload}")
        
        # Handle ALREADY_LOGGED_IN - we're already authenticated, proceed
        if error_code == "ALREADY_LOGGED_IN":
            print("[INFO] Application already authenticated, proceeding to account auth")
            application_authed = True
            if account_id is None:
                request = ProtoOAGetAccountListByAccessTokenReq()
                request.accessToken = ACCESS_TOKEN
                send_request(request)
            else:
                send_account_auth()
            return
        
        # If we get CANT_ROUTE_REQUEST on application auth, try switching endpoint
        if error_code == "CANT_ROUTE_REQUEST" and not application_authed:
            global current_endpoint_type
            if current_endpoint_type == "demo":
                print("[ERROR] CANT_ROUTE_REQUEST on demo endpoint - trying live endpoint as fallback")
                current_endpoint_type = "live"
                # Reconnect with live endpoint
                reactor.callLater(1.0, reconnect_with_endpoint, "live")
            elif current_endpoint_type == "live":
                print("[ERROR] CANT_ROUTE_REQUEST on live endpoint too - credentials may be invalid")
                print("[ERROR] Please verify your CTRADER_CLIENT_ID and CTRADER_CLIENT_SECRET in Pepperstone portal")
        
        set_status("error", f"{error_code}: {description}")
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
        request_trader_info()  # Request balance info
        return

    if message.payloadType == ProtoOASymbolsListRes().payloadType:
        load_broker_symbols(payload)
        symbols_loaded = True
        set_status("symbols_loaded", f"Loaded {len(broker_symbols_by_normalized)} broker symbols")
        subscribe_pending_symbols()
        process_pending_history_requests()
        return

    if message.payloadType == ProtoOASubscribeSpotsRes().payloadType:
        set_status("subscribed", f"Subscribed to {len(subscribed_symbol_ids)} Pepperstone cTrader symbols")
        return

    if message.payloadType == ProtoOASpotEvent().payloadType:
        handle_spot_event(payload)
        return

    if message.payloadType == ProtoOAGetTrendbarsRes().payloadType:
        handle_trendbar_response(payload)
        return
    
    if message.payloadType == ProtoOATraderRes().payloadType:
        handle_trader_response(payload)
        return
    
    if message.payloadType == ProtoOAExecutionEvent().payloadType:
        handle_order_response(payload)
        return
    
    if message.payloadType == ProtoOAReconcileRes().payloadType:
        handle_reconcile_response(payload)
        return

    # Log unrecognized messages so we can debug missing handlers
    print(f"[cTrader] Unrecognized message type: {message.payloadType}, payload: {payload}")


def send_account_auth() -> None:
    if account_id is None:
        set_status("error", "CTRADER_ACCOUNT_ID is missing and account auto-discovery failed")
        return
    print(f"[DEBUG] Sending account auth for account_id={account_id} with accessToken (len={len(ACCESS_TOKEN)})")
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


def request_trader_info() -> None:
    """Request account balance and margin info"""
    if account_id is None:
        return
    request = ProtoOATraderReq()
    request.ctidTraderAccountId = account_id
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


# Queue for pending history requests
pending_history_requests: List[tuple] = []  # [(symbol, timeframe, count), ...]


def request_historical_candles(symbol: str, timeframe: str = "1h", count: int = 500) -> None:
    """Request historical candles for a symbol"""
    if not account_authed or not symbols_loaded or account_id is None:
        # Queue the request for later
        with state_lock:
            pending_history_requests.append((symbol, timeframe, count))
        return
    
    normalized = normalize_symbol(symbol)
    item = resolve_broker_symbol(normalized)
    if item is None:
        return
    
    symbol_id = int(getattr(item, "symbolId", 0) or 0)
    if symbol_id <= 0:
        return
    
    # Map timeframe to cTrader period
    timeframe_map = {
        "1m": ProtoOATrendbarPeriod.M1,
        "5m": ProtoOATrendbarPeriod.M5,
        "15m": ProtoOATrendbarPeriod.M15,
        "30m": ProtoOATrendbarPeriod.M30,
        "1h": ProtoOATrendbarPeriod.H1,
        "4h": ProtoOATrendbarPeriod.H4,
        "1d": ProtoOATrendbarPeriod.D1,
        "1w": ProtoOATrendbarPeriod.W1,
    }
    period = timeframe_map.get(timeframe.lower(), ProtoOATrendbarPeriod.H1)
    
    # Calculate time range
    now_ms = int(time.time() * 1000)
    ms_per_bar = {
        "1m": 60 * 1000,
        "5m": 5 * 60 * 1000,
        "15m": 15 * 60 * 1000,
        "30m": 30 * 60 * 1000,
        "1h": 60 * 60 * 1000,
        "4h": 4 * 60 * 60 * 1000,
        "1d": 24 * 60 * 60 * 1000,
        "1w": 7 * 24 * 60 * 60 * 1000,
    }.get(timeframe.lower(), 60 * 60 * 1000)
    
    from_ms = now_ms - (count * ms_per_bar)
    
    request = ProtoOAGetTrendbarsReq()
    request.ctidTraderAccountId = account_id
    request.symbolId = symbol_id
    request.period = period
    request.fromTimestamp = from_ms
    request.toTimestamp = now_ms
    
    print(f"Requesting {count} {timeframe} candles for {symbol} (symbolId={symbol_id})")
    send_request(request)

    # Live trendbars are required for ongoing candle updates.
    # cTrader sends live trendbars via the spot stream after this subscription.
    current_period = int(period)
    with state_lock:
        previous_period = subscribed_live_trendbars.get(symbol_id)
        if previous_period == current_period:
            return
        subscribed_live_trendbars[symbol_id] = current_period

    live_request = ProtoOASubscribeLiveTrendbarReq()
    live_request.ctidTraderAccountId = account_id
    live_request.symbolId = symbol_id
    live_request.period = period
    print(f"Subscribing live trendbars for {symbol} (symbolId={symbol_id}, period={timeframe})")
    send_request(live_request)


def process_pending_history_requests() -> None:
    """Process any queued history requests after authentication"""
    with state_lock:
        requests = pending_history_requests.copy()
        pending_history_requests.clear()
    
    for symbol, timeframe, count in requests:
        request_historical_candles(symbol, timeframe, count)


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


def decode_price(raw: Optional[float], digits: int, category: str = "FOREX") -> Optional[float]:
    if raw is None or raw <= 0:
        return None
    # cTrader ProtoOASpotEvent sends ALL symbol prices in protocol units (scaled by PRICE_SCALE).
    # Divide whenever the raw value is large enough to be a protocol-scaled price.
    if raw > 10000 and PRICE_SCALE > 0:
        decoded = raw / PRICE_SCALE
        if decoded > 0:
            return decoded
    if digits > 0 and raw > 1000 and PRICE_SCALE <= 0:
        return raw / (10 ** digits)
    return raw


def decode_position_price(raw: Optional[float], digits: int, category: str = "FOREX") -> Optional[float]:
    if raw is None or raw <= 0:
        return None
    normalized_category = str(category or "FOREX").upper()
    # Pepperstone position prices may arrive either already-decoded, or still scaled.
    # For crypto CFDs and other high-priced symbols, values can be in the millions
    # when expressed in protocol units, so use a lower threshold than the old path.
    if normalized_category == "FOREX" and raw > 10000:
        if PRICE_SCALE > 0:
            decoded = raw / PRICE_SCALE
            if decoded > 0:
                return decoded
        if digits > 0:
            return raw / (10 ** digits)
    return raw


def handle_spot_event(payload) -> None:
    global last_tick_received_time
    last_tick_received_time = time.time()
    
    symbol_id = int(getattr(payload, "symbolId", 0) or 0)
    with state_lock:
        state = symbol_states_by_id.get(symbol_id)
    if state is None:
        return

    raw_bid = first_present(payload, ["bid", "relativeBid", "currentBid"])
    raw_ask = first_present(payload, ["ask", "relativeAsk", "currentAsk"])
    bid = decode_price(raw_bid, state.digits, state.category) if raw_bid is not None else state.last_bid
    ask = decode_price(raw_ask, state.digits, state.category) if raw_ask is not None else state.last_ask

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

    trendbars = getattr(payload, "trendbar", [])
    if trendbars:
        candles = []
        for bar in trendbars:
            timestamp = int(getattr(bar, "utcTimestampInMinutes", 0) or 0) * 60 * 1000
            if timestamp <= 0:
                continue

            low_raw = float(getattr(bar, "low", 0) or 0)
            delta_open = float(getattr(bar, "deltaOpen", 0) or 0)
            delta_high = float(getattr(bar, "deltaHigh", 0) or 0)
            delta_close = float(getattr(bar, "deltaClose", 0) or 0)
            volume = float(getattr(bar, "volume", 0) or 0)

            if low_raw <= 0:
                continue

            divisor = 10 ** state.digits if state.digits > 0 else 100000
            low_price = low_raw / divisor
            open_price = (low_raw + delta_open) / divisor
            high_price = (low_raw + delta_high) / divisor
            close_price = (low_raw + delta_close) / divisor

            if not all([open_price > 0, high_price > 0, low_price > 0, close_price > 0]):
                continue

            candles.append(
                {
                    "time": timestamp,
                    "open": open_price,
                    "high": high_price,
                    "low": low_price,
                    "close": close_price,
                    "volume": volume,
                }
            )

        if candles:
            broadcast(
                {
                    "type": "history",
                    "source": "pepperstone_ctrader",
                    "symbol": state.app_symbol,
                    "displaySymbol": display_symbol(state.app_symbol),
                    "data": candles,
                }
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


def handle_trendbar_response(payload) -> None:
    """Handle historical candle data response"""
    symbol_id = int(getattr(payload, "symbolId", 0) or 0)
    with state_lock:
        state = symbol_states_by_id.get(symbol_id)
    if state is None:
        print(f"Received trendbars for unknown symbol_id: {symbol_id}")
        return
    
    trendbars = getattr(payload, "trendbar", [])
    if not trendbars:
        print(f"No trendbars in response for {state.app_symbol}")
        return
    
    print(f"Processing {len(trendbars)} trendbars for {state.app_symbol}")
    
    candles = []
    for bar in trendbars:
        timestamp = int(getattr(bar, "utcTimestampInMinutes", 0) or 0) * 60 * 1000  # Convert minutes to milliseconds
        if timestamp <= 0:
            continue
        
        # cTrader uses delta encoding: low is absolute, others are deltas from low
        low_raw = float(getattr(bar, "low", 0) or 0)
        delta_open = float(getattr(bar, "deltaOpen", 0) or 0)
        delta_high = float(getattr(bar, "deltaHigh", 0) or 0)
        delta_close = float(getattr(bar, "deltaClose", 0) or 0)
        volume = float(getattr(bar, "volume", 0) or 0)
        
        if low_raw <= 0:
            continue
        
        # Decode prices: divide by 10^digits to get actual price
        divisor = 10 ** state.digits if state.digits > 0 else 100000
        low_price = low_raw / divisor
        open_price = (low_raw + delta_open) / divisor
        high_price = (low_raw + delta_high) / divisor
        close_price = (low_raw + delta_close) / divisor
        
        if not all([open_price > 0, high_price > 0, low_price > 0, close_price > 0]):
            continue
        
        candles.append({
            "time": timestamp,
            "open": open_price,
            "high": high_price,
            "low": low_price,
            "close": close_price,
            "volume": volume
        })
    
    if candles:
        print(f"Broadcasting {len(candles)} candles for {state.app_symbol}")
        broadcast({
            "type": "history",
            "source": "pepperstone_ctrader",
            "symbol": state.app_symbol,
            "displaySymbol": display_symbol(state.app_symbol),
            "data": candles
        })
    else:
        print(f"No valid candles decoded for {state.app_symbol}")


def handle_trader_response(payload) -> None:
    """Handle account balance and margin info"""
    global last_account_message, pending_balance_snapshot
    
    print(f"[DEBUG] Raw trader response: {payload}")
    
    # Get the trader object (some API versions nest it, some don't)
    trader = getattr(payload, "trader", None)
    data_source = trader if trader is not None else payload
    if trader is None:
        print("[Account] No nested trader object, reading fields directly from payload")
    
    # Get moneyDigits to calculate the divisor
    raw_money_digits = getattr(data_source, "moneyDigits", None)
    money_digits = int(raw_money_digits) if raw_money_digits is not None else 2
    divisor = 10 ** money_digits
    
    balance = float(getattr(data_source, "balance", 0) or 0) / divisor
    equity = float(getattr(data_source, "equity", 0) or 0) / divisor if hasattr(data_source, "equity") else balance
    margin_used = float(getattr(data_source, "usedMargin", 0) or 0) / divisor if hasattr(data_source, "usedMargin") else 0
    free_margin = float(getattr(data_source, "freeMargin", 0) or 0) / divisor if hasattr(data_source, "freeMargin") else balance
    margin_level = float(getattr(data_source, "marginLevel", 0) or 0) if hasattr(data_source, "marginLevel") else 0
    
    # Compute realized PnL from balance delta if an execution event just happened
    realized_pnl = 0.0
    if pending_balance_snapshot is not None:
        if balance != pending_balance_snapshot:
            realized_pnl = round(balance - pending_balance_snapshot, 2)
            print(f"[Account] Realized PnL from balance delta: {realized_pnl:,.2f} (before={pending_balance_snapshot:,.2f}, after={balance:,.2f})")
        else:
            print(f"[Account] Balance unchanged after execution: {balance:,.2f}")
        pending_balance_snapshot = None  # Consume the snapshot
    
    print(f"Account Balance: ${balance:,.2f}, Equity: ${equity:,.2f}, Margin: ${margin_used:,.2f}, RealizedPnL: ${realized_pnl:,.2f}")
    
    account_message = {
        "type": "ACCOUNT",
        "source": "pepperstone_ctrader",
        "accountId": account_id,
        "balance": balance,
        "equity": equity,
        "margin": margin_used,
        "marginUsed": margin_used,
        "freeMargin": free_margin,
        "marginLevel": margin_level,
        "realizedPnl": realized_pnl,
        "currency": "USD"
    }
    
    # Store the last account message
    last_account_message = account_message
    
    print(f"[DEBUG] Broadcasting account message: {json.dumps(account_message)}")
    broadcast(account_message)


def place_market_order(symbol: str, side: str, volume: float, stop_loss: Optional[float] = None, take_profit: Optional[float] = None) -> None:
    """Place a market order"""
    if not account_authed or account_id is None:
        print(f"Cannot place order: account not authenticated")
        return
    
    # Resolve symbol
    item = resolve_broker_symbol(symbol)
    if item is None:
        print(f"Cannot place order: symbol {symbol} not found")
        return
    
    symbol_id = int(getattr(item, "symbolId", 0) or 0)
    if symbol_id <= 0:
        print(f"Cannot place order: invalid symbol ID for {symbol}")
        return
    
    # Convert volume to cents (cTrader uses volume in cents; 1 lot = 100)
    # Minimum order size is 0.01 lots (= 1 cent)
    volume_in_cents = int(round(volume * 100))
    if volume_in_cents <= 0:
        print(f"Cannot place order: volume {volume} lots is below minimum 0.01 lots (centi-lot=0)")
        broadcast({
            "type": "ORDER_UPDATE",
            "source": "pepperstone_ctrader",
            "status": "rejected",
            "error": f"Volume {volume} below minimum 0.01 lots"
        })
        return
    
    # Create order request
    request = ProtoOANewOrderReq()
    request.ctidTraderAccountId = account_id
    request.symbolId = symbol_id
    request.orderType = ProtoOAOrderType.MARKET
    request.tradeSide = ProtoOATradeSide.BUY if side.lower() == "buy" else ProtoOATradeSide.SELL
    request.volume = volume_in_cents
    
    # Add stop loss if provided
    if stop_loss is not None and stop_loss > 0:
        with state_lock:
            state = symbol_states_by_id.get(symbol_id)
        if state and state.digits > 0:
            # Convert price to pips
            stop_loss_pips = int(stop_loss * (10 ** state.digits))
            request.stopLoss = stop_loss_pips
    
    # Add take profit if provided
    if take_profit is not None and take_profit > 0:
        with state_lock:
            state = symbol_states_by_id.get(symbol_id)
        if state and state.digits > 0:
            # Convert price to pips
            take_profit_pips = int(take_profit * (10 ** state.digits))
            request.takeProfit = take_profit_pips
    
    print(f"Placing {side.upper()} order: {symbol} (symbolId={symbol_id}), volume={volume}, SL={stop_loss}, TP={take_profit}")
    print(f"[Order] ProtoOANewOrderReq: ctidTraderAccountId={account_id}, symbolId={symbol_id}, orderType=MARKET, tradeSide={side.upper()}, volume={volume_in_cents}")
    send_request(request)
    
    # Schedule delayed reconcile so positions update even if execution event is lost
    reactor.callLater(2.0, request_positions)
    reactor.callLater(2.0, request_trader_info)
    reactor.callLater(5.0, request_positions)
    reactor.callLater(5.0, request_trader_info)


def close_position(position_id: int, volume: float) -> None:
    """Close or partially close an existing position"""
    if not account_authed or account_id is None:
        print("Cannot close position: account not authenticated")
        return

    if position_id <= 0 or volume <= 0:
        print(f"Cannot close position: invalid positionId={position_id} volume={volume}")
        return

    request = ProtoOAClosePositionReq()
    request.ctidTraderAccountId = account_id
    request.positionId = int(position_id)
    request.volume = int(round(volume * 100))

    print(f"Closing position {position_id} with volume={volume} (protocol volume={request.volume})")
    send_request(request)
    
    # Schedule delayed reconcile so positions update even if execution event is lost
    reactor.callLater(2.0, request_positions)
    reactor.callLater(2.0, request_trader_info)
    reactor.callLater(5.0, request_positions)
    reactor.callLater(5.0, request_trader_info)


def handle_order_response(payload) -> None:
    """Handle order execution response"""
    global last_account_message, pending_balance_snapshot
    # Snapshot current balance so handle_trader_response can compute realized PnL delta
    if last_account_message is not None:
        pending_balance_snapshot = last_account_message.get("balance")
        print(f"[Order] Snapshotted pre-execution balance: {pending_balance_snapshot}")
    # Try direct fields first, then nested objects
    order_id = (getattr(payload, "orderId", None) 
                or getattr(getattr(payload, "order", None), "orderId", None)
                or getattr(getattr(payload, "position", None), "positionId", None)
                or getattr(payload, "positionId", None))
    exec_type = getattr(payload, "executionType", None)
    error_code = getattr(payload, "errorCode", None)
    
    print(f"[Order] Execution response:")
    print(f"  - executionType: {exec_type}")
    print(f"  - orderId: {order_id}")
    print(f"  - positionId: {getattr(payload, 'positionId', None)}")
    print(f"  - errorCode: {error_code}")
    print(f"  - Full payload: {payload}")
    
    exec_type_str = str(exec_type).upper() if exec_type else ""
    is_rejected = error_code is not None or "REJECT" in exec_type_str or "CANCEL" in exec_type_str
    
    # Extract execution event balance to compute realized PnL from balance delta
    exec_balance = getattr(payload, "balance", None)
    exec_equity = getattr(payload, "equity", None)
    exec_used_margin = getattr(payload, "usedMargin", None)
    exec_free_margin = getattr(payload, "freeMargin", None)
    exec_margin_level = getattr(payload, "marginLevel", None)
    exec_money_digits = getattr(payload, "moneyDigits", None)
    
    md = int(exec_money_digits) if exec_money_digits is not None else 2
    div = 10 ** md
    
    deal_profit = None
    balance_before = None
    balance_after = None
    if exec_balance is not None:
        balance_after = float(exec_balance) / div
        prev_balance = last_account_message.get("balance") if last_account_message else None
        if prev_balance is not None and prev_balance != balance_after:
            deal_profit = balance_after - prev_balance
            balance_before = prev_balance
            print(f"[Order] Computed realized PnL from balance delta: {deal_profit:,.2f} (before={balance_before:,.2f}, after={balance_after:,.2f})")
    
    if is_rejected:
        print(f"[Order] ERROR: Order rejected with error code: {error_code}, executionType: {exec_type}")
        broadcast({
            "type": "ORDER_UPDATE",
            "source": "pepperstone_ctrader",
            "orderId": str(order_id) if order_id else None,
            "status": "rejected",
            "error": str(error_code) if error_code else exec_type_str
        })
    else:
        # Broadcast order update to Android clients
        order_message = {
            "type": "ORDER_UPDATE",
            "source": "pepperstone_ctrader",
            "orderId": str(order_id) if order_id else None,
            "status": "executed" if order_id else "pending"
        }
        if deal_profit is not None:
            order_message["profit"] = deal_profit
            order_message["balanceBefore"] = balance_before
            order_message["balanceAfter"] = balance_after
        broadcast(order_message)
    
    # Broadcast immediate account update from execution event
    if exec_balance is not None:
        updated_balance = float(exec_balance) / div
        updated_equity = float(exec_equity) / div if exec_equity is not None else updated_balance
        updated_margin = float(exec_used_margin) / div if exec_used_margin is not None else 0.0
        updated_free = float(exec_free_margin) / div if exec_free_margin is not None else updated_balance
        updated_margin_level = float(exec_margin_level) if exec_margin_level is not None else 0.0
        
        print(f"[Order] Immediate account update from execution event: balance={updated_balance:,.2f}, equity={updated_equity:,.2f}")
        
        account_message = {
            "type": "ACCOUNT",
            "source": "pepperstone_ctrader",
            "accountId": account_id,
            "balance": updated_balance,
            "equity": updated_equity,
            "margin": updated_margin,
            "marginUsed": updated_margin,
            "freeMargin": updated_free,
            "marginLevel": updated_margin_level,
            "currency": "USD"
        }
        last_account_message = account_message
        broadcast(account_message)
    
    # Always request updated account info and positions after any execution event
    print(f"[Order] Requesting updated account info and positions...")
    request_trader_info()
    request_positions()


def request_positions() -> None:
    """Request open positions"""
    if account_id is None:
        return
    request = ProtoOAReconcileReq()
    request.ctidTraderAccountId = account_id
    send_request(request)


def handle_reconcile_response(payload) -> None:
    """Handle positions list response"""
    positions = []
    
    for pos in getattr(payload, "position", []):
        position_id = str(getattr(pos, "positionId", ""))
        trade_data = getattr(pos, "tradeData", None)
        
        if trade_data is None:
            continue
        
        symbol_id = int(getattr(trade_data, "symbolId", 0) or 0)
        
        with state_lock:
            state = symbol_states_by_id.get(symbol_id)
        
        if state is None:
            continue
        
        trade_side = getattr(trade_data, "tradeSide", "")
        side = "buy" if trade_side == ProtoOATradeSide.BUY else "sell"
        
        volume = float(getattr(trade_data, "volume", 0) or 0) / 100.0  # Convert from cents
        
        # Get the entry price - it's stored in the position, not trade_data
        raw_price = float(getattr(pos, "entryPrice", 0) or 0)
        if raw_price <= 0:
            raw_price = float(getattr(pos, "price", 0) or 0)
        
        entry_price = decode_position_price(raw_price, state.digits, state.category)
        
        # Get unrealized P&L
        unrealized_pnl = float(getattr(pos, "grossProfit", 0) or 0)
        money_digits = int(getattr(pos, "moneyDigits", 2) or 2)
        if abs(unrealized_pnl) > 10000:
            unrealized_pnl = unrealized_pnl / (10 ** money_digits)
        
        positions.append({
            "id": position_id,
            "symbol": state.app_symbol,
            "side": side,
            "volume": volume,
            "entryPrice": entry_price,
            "unrealizedPnl": unrealized_pnl
        })
    
    if positions:
        print(f"[Positions] Received {len(positions)} open positions")
        for i, pos in enumerate(positions):
            print(f"  Position {i+1}: {pos}")
        broadcast({
            "type": "POSITIONS",
            "source": "pepperstone_ctrader",
            "positions": positions
        })
        
        # Request updated account info to get current balance/equity
        print(f"[Positions] Requesting updated account info...")
        request_trader_info()
    else:
        print(f"[Positions] No open positions")
        broadcast({
            "type": "POSITIONS",
            "source": "pepperstone_ctrader",
            "positions": []
        })


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

    host = EndPoints.PROTOBUF_LIVE_HOST if current_endpoint_type == "live" else EndPoints.PROTOBUF_DEMO_HOST
    print(f"[DEBUG] Connecting to: {host}:{EndPoints.PROTOBUF_PORT}")
    client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
    client.setConnectedCallback(connected)
    client.setDisconnectedCallback(disconnected)
    client.setMessageReceivedCallback(on_message_received)
    client.startService()
    reactor.run(installSignalHandlers=False)


def reconnect_with_endpoint(endpoint_type: str) -> None:
    """Reconnect to cTrader with a different endpoint (demo or live)"""
    global client, current_endpoint_type
    print(f"[RECONNECT] Switching to {endpoint_type} endpoint and reconnecting...")
    
    # Stop current client
    if client:
        try:
            client.stopService()
        except Exception as e:
            print(f"[RECONNECT] Error stopping client: {e}")
    
    # Update endpoint type
    current_endpoint_type = endpoint_type
    
    # Restart with new endpoint
    host = EndPoints.PROTOBUF_LIVE_HOST if endpoint_type == "live" else EndPoints.PROTOBUF_DEMO_HOST
    print(f"[RECONNECT] Connecting to: {host}:{EndPoints.PROTOBUF_PORT}")
    client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
    client.setConnectedCallback(connected)
    client.setDisconnectedCallback(disconnected)
    client.setMessageReceivedCallback(on_message_received)
    client.startService()


async def handle_android_client(websocket, *_args) -> None:
    android_clients.add(websocket)
    print(f"[Bridge] Android client connected from {websocket.remote_address}")
    
    try:
        # Send initial status
        await websocket.send(json.dumps(status_payload(), separators=(",", ":")))
        
        # Send last account info if available
        if last_account_message is not None:
            print(f"[Bridge] Sending cached account info to new client: {json.dumps(last_account_message)}")
            await websocket.send(json.dumps(last_account_message, separators=(",", ":")))
        elif account_authed and account_id:
            # Request fresh account info if we don't have cached data
            print(f"[Bridge] Requesting fresh account info for new client")
            reactor.callFromThread(request_trader_info)
        
        # Request positions for new client
        if account_authed and account_id:
            print(f"[Bridge] Requesting positions for new client")
            reactor.callFromThread(request_positions)
            
    except Exception as e:
        print(f"[Bridge] Error sending initial data to client: {e}")
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
                
                # Subscribe to live ticks
                reactor.callFromThread(subscribe_app_symbols, [str(symbol) for symbol in symbols])
                
                # Request historical candles
                timeframe = payload.get("timeframe", "1h")
                count = payload.get("count", 500)
                for symbol in symbols:
                    reactor.callFromThread(request_historical_candles, str(symbol), timeframe, count)
                
                try:
                    await websocket.send(json.dumps({"type": "ack", "action": "subscribe", "symbols": symbols}))
                except Exception:
                    break
            
            elif action == "place_order":
                symbol = str(payload.get("symbol", ""))
                side = str(payload.get("side", "buy")).lower()
                volume = float(payload.get("volume", 0.01))
                stop_loss = payload.get("stopLoss")
                take_profit = payload.get("takeProfit")
                
                if symbol and volume > 0:
                    print(f"[Bridge] Received order request: {side.upper()} {volume} lots of {symbol}")
                    reactor.callFromThread(place_market_order, symbol, side, volume, stop_loss, take_profit)
                    try:
                        await websocket.send(json.dumps({"type": "ack", "action": "place_order"}))
                    except Exception:
                        break
                else:
                    try:
                        await websocket.send(json.dumps({"type": "error", "message": "Invalid order parameters"}))
                    except Exception:
                        break

            elif action == "close_position":
                position_id = int(payload.get("positionId", 0) or 0)
                volume = float(payload.get("volume", 0.0) or 0.0)

                if position_id > 0 and volume > 0:
                    print(f"[Bridge] Received close position request: positionId={position_id}, volume={volume}")
                    reactor.callFromThread(close_position, position_id, volume)
                    try:
                        await websocket.send(json.dumps({"type": "ack", "action": "close_position", "positionId": position_id}))
                    except Exception:
                        break
                else:
                    try:
                        await websocket.send(json.dumps({"type": "error", "message": "Invalid close position parameters"}))
                    except Exception:
                        break
            
            elif action == "ping":
                try:
                    await websocket.send(json.dumps({"type": "pong", "time": int(time.time() * 1000)}))
                except Exception:
                    break
            elif action == "get_account":
                if last_account_message is not None:
                    try:
                        await websocket.send(json.dumps(last_account_message, separators=(",", ":")))
                    except Exception:
                        break
                elif account_authed and account_id:
                    reactor.callFromThread(request_trader_info)
                    try:
                        await websocket.send(json.dumps({"type": "ack", "action": "get_account"}))
                    except Exception:
                        break
                else:
                    try:
                        await websocket.send(json.dumps({"type": "error", "message": "cTrader account is not authenticated yet"}))
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
