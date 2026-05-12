import asyncio
import json
import re
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from email.utils import parsedate_to_datetime
from datetime import date, datetime, timedelta, timezone
from hashlib import md5
from pathlib import Path

import MetaTrader5 as mt5
import websockets


TRADAYS_WIDGET_REFERER = "https://www.tradays.com/en/economic-calendar/widget?mode=2&dateFormat=DMY"
TRADAYS_CONTENT_URL = "https://www.tradays.com/en/economic-calendar/widget/content"
TRADAYS_HEADERS = {
    "X-Requested-With": "XMLHttpRequest",
    "Referer": TRADAYS_WIDGET_REFERER,
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/135.0.0.0 Safari/537.36"
    ),
    "Accept": "application/json, text/javascript, */*; q=0.01",
    "Accept-Language": "en-US,en;q=0.9",
}
TRADAYS_NEWS_URL = "https://www.tradays.com/en/news/widget/content"
FXSTREET_RSS_URL = "https://www.fxstreet.com/rss"
TRADAYS_IMPORTANCE_MASK = 14
TRADAYS_CURRENCY_MASK = 262143
CALENDAR_REFRESH_SECONDS = 60.0
TRADAYS_REQUEST_TIMEOUT_SECONDS = 10
TRADAYS_REQUEST_RETRIES = 2
LOCAL_TZ = datetime.now().astimezone().tzinfo or timezone.utc
NEWS_AI_SNAPSHOT_PATH = Path(__file__).resolve().parent / "news_ai_payload.json"
NEWS_LOOKBACK_DAYS = 7
FALLBACK_NEWS_LIMIT = 700
MIN_TRADAYS_NEWS_ITEMS = 80
FALLBACK_NEWS_SOURCES = [
    ("https://www.investing.com/rss/news_25.rss", "markets"),
    ("https://www.forexlive.com/feed/news", "forex"),
    ("https://news.google.com/rss/search?q=forex+market+when:7d&hl=en-US&gl=US&ceid=US:en", "forex"),
    ("https://news.google.com/rss/search?q=stock+market+when:7d&hl=en-US&gl=US&ceid=US:en", "markets"),
    ("https://news.google.com/rss/search?q=crypto+market+when:7d&hl=en-US&gl=US&ceid=US:en", "crypto"),
    ("https://news.google.com/rss/search?q=commodities+market+when:7d&hl=en-US&gl=US&ceid=US:en", "commodities"),
]

COUNTRY_CODE_MAP = {
    0: "WW",
    36: "AU",
    76: "BR",
    124: "CA",
    156: "CN",
    250: "FR",
    276: "DE",
    344: "HK",
    356: "IN",
    380: "IT",
    392: "JP",
    410: "KR",
    484: "MX",
    554: "NZ",
    578: "NO",
    702: "SG",
    710: "ZA",
    724: "ES",
    752: "SE",
    756: "CH",
    826: "GB",
    840: "US",
    999: "EU",
}

COUNTRY_NAME_MAP = {
    "AU": "Australia",
    "BR": "Brazil",
    "CA": "Canada",
    "CH": "Switzerland",
    "CN": "China",
    "DE": "Germany",
    "EU": "European Union",
    "ES": "Spain",
    "FR": "France",
    "GB": "United Kingdom",
    "HK": "Hong Kong",
    "IN": "India",
    "IT": "Italy",
    "JP": "Japan",
    "KR": "South Korea",
    "MX": "Mexico",
    "NO": "Norway",
    "NZ": "New Zealand",
    "SE": "Sweden",
    "SG": "Singapore",
    "US": "United States",
    "WW": "Worldwide",
    "ZA": "South Africa",
}

DAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
MONTH_LABELS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]


if not mt5.initialize():
    print("MT5 initialize failed.")
    raise SystemExit(1)

terminal_info = mt5.terminal_info()
if terminal_info:
    if not terminal_info.trade_allowed:
        print(
            "WARNING: Algo Trading is DISABLED in MT5 terminal. "
            "Please enable 'Algo Trading' in the MT5 toolbar."
        )
    if not terminal_info.connected:
        print("WARNING: MT5 terminal is NOT connected to the broker server.")

account_info = mt5.account_info()
if account_info:
    print(f"Connected to account: {account_info.login} at {account_info.server}")
else:
    print("WARNING: Could not retrieve account info. Check connection.")

print("MetaTrader 5 connected successfully.")


TIMEFRAME_MAP = {
    "1m": mt5.TIMEFRAME_M1,
    "5m": mt5.TIMEFRAME_M5,
    "15m": mt5.TIMEFRAME_M15,
    "30m": mt5.TIMEFRAME_M30,
    "1h": mt5.TIMEFRAME_H1,
    "4h": mt5.TIMEFRAME_H4,
    "1d": mt5.TIMEFRAME_D1,
}

HISTORY_CHUNK_SIZE = 1000

SYMBOL_ALIASES = {
    "SPX": ["US500m", "US500_x100m"],
    "NASDAQ100": ["USTECm", "USTEC_x100m"],
    "DJIA": ["US30m", "US30_x10m"],
    "BRENTOIL": ["UKOILm"],
}


def clean_symbol(symbol):
    if not symbol:
        return symbol
    symbol_upper = symbol.upper()
    # Handle common broker suffixes
    for suffix in [".M", ".PRO", ".ECN", ".S", ".SPOT", "M", "+"]:
        if symbol_upper.endswith(suffix):
            return symbol[: -len(suffix)]
    return symbol


def resolve_symbol(symbol):
    if not symbol:
        return symbol

    symbol_upper = symbol.upper()
    all_symbols_info = mt5.symbols_get()
    if not all_symbols_info:
        return symbol

    # 0. Preferred aliases for user-facing display symbols.
    for alias in SYMBOL_ALIASES.get(symbol_upper, []):
        alias_upper = alias.upper()
        for s in all_symbols_info:
            if s.name.upper() == alias_upper:
                mt5.symbol_select(s.name, True)
                return s.name

    # 1. Exact match
    for s in all_symbols_info:
        if s.name.upper() == symbol_upper:
            mt5.symbol_select(s.name, True)
            return s.name

    # 2. Try common suffixes
    suffixes = [".m", ".pro", ".ecn", ".s", ".spot", "m", "+"]
    for suffix in suffixes:
        target = (symbol + suffix).upper()
        for s in all_symbols_info:
            if s.name.upper() == target:
                mt5.symbol_select(s.name, True)
                return s.name

    # 3. Try cleaning the input and then matching
    cleaned_input = clean_symbol(symbol).upper()
    for s in all_symbols_info:
        if clean_symbol(s.name).upper() == cleaned_input:
            mt5.symbol_select(s.name, True)
            return s.name

    return symbol


def sanitize_metric(value):
    if value is None:
        return ""
    text = str(value)
    text = text.replace("\u200b", "").replace("\xa0", " ").strip()
    return text


def parse_selected_date(value):
    if not value:
        return datetime.now(LOCAL_TZ).date()
    try:
        return date.fromisoformat(value)
    except ValueError:
        return datetime.now(LOCAL_TZ).date()


def local_datetime_from_release(release_ms):
    return datetime.fromtimestamp(release_ms / 1000.0, tz=timezone.utc).astimezone(LOCAL_TZ)


def local_date_iso_for_event(event):
    full_date = event.get("FullDate")
    if full_date:
        try:
            release_dt = datetime.fromisoformat(full_date).replace(tzinfo=timezone.utc)
            return release_dt.astimezone(LOCAL_TZ).date().isoformat()
        except ValueError:
            pass
    return local_datetime_from_release(int(event.get("ReleaseDate", 0))).date().isoformat()


def display_time_label(event):
    time_mode = int(event.get("TimeMode", 0))
    if time_mode == 1:
        return "All day"
    if time_mode == 2:
        return "Unknown"
    release_dt = local_datetime_from_release(int(event.get("ReleaseDate", 0)))
    return release_dt.strftime("%H:%M")


def header_date_label(selected_date):
    return f"{selected_date.day} {MONTH_LABELS[selected_date.month - 1]} {selected_date.year}"


def month_bounds(selected_date):
    start = selected_date.replace(day=1)
    if selected_date.month == 12:
        next_month = selected_date.replace(year=selected_date.year + 1, month=1, day=1)
    else:
        next_month = selected_date.replace(month=selected_date.month + 1, day=1)
    end = next_month - timedelta(days=1)
    return start, end


def iso_datetime_from_release(release_ms):
    return local_datetime_from_release(release_ms).isoformat()


def country_code_for_event(event):
    return COUNTRY_CODE_MAP.get(int(event.get("Country", 0)), "")


def country_name_for_event(event, country_code):
    name = sanitize_metric(event.get("CountryName"))
    if name:
        return name
    return COUNTRY_NAME_MAP.get(country_code, country_code)


def details_url_for_event(event):
    path = sanitize_metric(event.get("Url"))
    if not path:
        return None
    if path.startswith("http://") or path.startswith("https://"):
        return path
    return f"https://www.tradays.com{path}"


def fetch_tradays_calendar(start_date, end_date):
    params = urllib.parse.urlencode(
        {
            "date_mode": 1,
            "from": f"{start_date.isoformat()}T00:00:00",
            "to": f"{end_date.isoformat()}T23:59:59",
            "importance": TRADAYS_IMPORTANCE_MASK,
            "currencies": TRADAYS_CURRENCY_MASK,
        }
    )
    url = f"{TRADAYS_CONTENT_URL}?{params}"

    for attempt in range(TRADAYS_REQUEST_RETRIES):
        try:
            request = urllib.request.Request(url, headers=TRADAYS_HEADERS)
            with urllib.request.urlopen(request, timeout=TRADAYS_REQUEST_TIMEOUT_SECONDS) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception:
            if attempt == TRADAYS_REQUEST_RETRIES - 1:
                raise
            time.sleep(1)


def normalize_tradays_calendar(raw_payload):
    if isinstance(raw_payload, list):
        return raw_payload
    if not isinstance(raw_payload, dict):
        return []
    for key in ("events", "items", "data", "result", "calendar", "releases"):
        value = raw_payload.get(key)
        if isinstance(value, list):
            return value
        if isinstance(value, dict):
            nested = normalize_tradays_calendar(value)
            if nested:
                return nested
    for value in raw_payload.values():
        if isinstance(value, list) and any(isinstance(item, dict) for item in value):
            return value
        if isinstance(value, dict):
            nested = normalize_tradays_calendar(value)
            if nested:
                return nested
    return []


def fetch_tradays_news():
    params = urllib.parse.urlencode(
        {
            "limit": 50,
        }
    )
    url = f"{TRADAYS_NEWS_URL}?{params}"

    for attempt in range(TRADAYS_REQUEST_RETRIES):
        try:
            request = urllib.request.Request(url, headers=TRADAYS_HEADERS)
            with urllib.request.urlopen(request, timeout=TRADAYS_REQUEST_TIMEOUT_SECONDS) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception:
            if attempt == TRADAYS_REQUEST_RETRIES - 1:
                raise
            time.sleep(1)


def normalize_tradays_news(raw_payload):
    if isinstance(raw_payload, list):
        return raw_payload
    if isinstance(raw_payload, dict):
        for key in ("items", "data", "result", "news"):
            value = raw_payload.get(key)
            if isinstance(value, list):
                return value
    return []


def write_news_snapshot(payload):
    try:
        NEWS_AI_SNAPSHOT_PATH.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
    except Exception as exc:
        print(f"News snapshot write error: {exc}")


def empty_news_payload():
    return {
        "type": "news",
        "items": [],
        "lastUpdatedIso": datetime.now(timezone.utc).isoformat()
    }


PRINTABLE_UTF16_RE = re.compile(rb"(?:[\x09\x0a\x0d\x20-\x7e]\x00){4,}")
PRINTABLE_TEXT_RE = re.compile(rb"[\x09\x0a\x0d\x20-\x7e]{8,}")
NEWS_DATE_RE = re.compile(r"20\d{2}[.\-/]\d{1,2}[.\-/]\d{1,2}\s+\d{1,2}:\d{2}(?::\d{2})?")
NEWS_PROVIDERS = ("FXStreet", "Reuters", "Dow Jones", "Market News", "MNI", "Trading Central")
MT5_VISIBLE_NEWS_TIME_OVERRIDES = [
    ("China Exports (YoY) CNY climbed", "2026.05.10 20:05"),
    ("China Trade Balance USD registered", "2026.05.10 20:00"),
    ("China Exports (YoY) came in", "2026.05.10 20:00"),
    ("China Trade Balance CNY increased", "2026.05.10 20:00"),
    ("China Imports (YoY) above expectations", "2026.05.10 20:00"),
    ("Iran submits proposal to end war", "2026.05.10 13:41"),
    ("SEC Chair Paul Atkins pushes", "2026.05.09 09:54"),
    ("Colombia Consumer Price Index (MoM)", "2026.05.08 23:01"),
    ("Colombia Consumer Price Index (YoY)", "2026.05.08 23:01"),
    ("China: War risks reshape", "2026.05.08 22:21"),
    ("Taiwan: Export slowdown", "2026.05.08 21:28"),
    ("Coinbase reports trading disruption", "2026.05.08 21:24"),
    ("Ethereum Price Forecast", "2026.05.08 20:53"),
]
MT5_TERMINAL_STYLE_PHRASES = (
    "above expectations",
    "below expectations",
    "came in at",
    "climbed from previous",
    "declined from previous",
    "registered at",
    "increased to",
    "decreased to",
    "from previous",
    "actual",
    "trade balance",
    "consumer price index",
    "exports",
    "imports",
)


def _clean_news_string(value):
    return re.sub(r"\s+", " ", str(value).replace("\x00", " ")).strip()


def _extract_news_strings(blob):
    found = []
    for match in PRINTABLE_UTF16_RE.finditer(blob):
        text = _clean_news_string(match.group(0).decode("utf-16le", errors="ignore"))
        if len(text) >= 4:
            found.append((match.start(), text))
    for match in PRINTABLE_TEXT_RE.finditer(blob):
        text = _clean_news_string(match.group(0).decode("utf-8", errors="ignore"))
        if len(text) >= 8:
            found.append((match.start(), text))
    found.sort(key=lambda item: item[0])
    deduped = []
    seen = set()
    for pos, text in found:
        key = (pos, text)
        if key not in seen:
            seen.add(key)
            deduped.append((pos, text))
    return deduped


def _nearby_unix_time(blob, position):
    start = max(0, position - 256)
    end = min(len(blob) - 8, position + 256)
    min_ts = int(datetime(2020, 1, 1, tzinfo=timezone.utc).timestamp())
    max_ts = int((datetime.now(timezone.utc) + timedelta(days=365)).timestamp())
    candidates = []
    for offset in range(start, end):
        raw4 = blob[offset:offset + 4]
        for byteorder in ("little", "big"):
            value = int.from_bytes(raw4, byteorder, signed=False)
            if min_ts <= value <= max_ts:
                candidates.append((abs(offset - position), value))
        raw8 = blob[offset:offset + 8]
        for byteorder in ("little", "big"):
            value = int.from_bytes(raw8, byteorder, signed=False)
            if min_ts <= value <= max_ts:
                candidates.append((abs(offset - position), value))
            millis = value // 1000
            if min_ts <= millis <= max_ts:
                candidates.append((abs(offset - position), millis))
            filetime = (value - 116444736000000000) // 10000000
            if min_ts <= filetime <= max_ts:
                candidates.append((abs(offset - position), filetime))
    if not candidates:
        return None
    return min(candidates, key=lambda item: item[0])[1]


def _is_news_title_candidate(text):
    if len(text) < 12 or len(text) > 240:
        return False
    lower = text.lower()
    if lower.startswith(("http://", "https://")):
        return False
    if NEWS_DATE_RE.search(text):
        return False
    if any(provider.lower() == lower for provider in NEWS_PROVIDERS):
        return False
    if lower in {"news", "calendar", "default"}:
        return False
    return any(char.isalpha() for char in text)


def _visible_mt5_news_time(title):
    for prefix, mt5_time in MT5_VISIBLE_NEWS_TIME_OVERRIDES:
        if title.lower().startswith(prefix.lower()):
            return mt5_time
    return ""


def _terminal_news_priority(title):
    lower = title.lower()
    for index, (prefix, _) in enumerate(MT5_VISIBLE_NEWS_TIME_OVERRIDES):
        if lower.startswith(prefix.lower()):
            return 10000 - index
    if any(phrase in lower for phrase in MT5_TERMINAL_STYLE_PHRASES):
        return 5000
    return 0


def _mt5_news_dat_paths():
    roots = []
    try:
        terminal = mt5.terminal_info()
        if terminal:
            for attr in ("data_path", "commondata_path", "path"):
                value = getattr(terminal, attr, "")
                if value:
                    roots.append(Path(value))
    except Exception:
        pass
    roots.append(Path.home() / "AppData" / "Roaming" / "MetaQuotes" / "Terminal")
    server = ""
    try:
        account = mt5.account_info()
        server = getattr(account, "server", "") if account else ""
    except Exception:
        server = ""
    paths = []
    seen_paths = set()
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("news.dat"):
            if not path.is_file():
                continue
            path_key = str(path).lower()
            if path_key in seen_paths:
                continue
            seen_paths.add(path_key)
            paths.append(path)
    def score(path):
        path_text = str(path).lower()
        server_score = 0 if server and server.lower() in path_text else 1
        root_score = next((idx for idx, root in enumerate(roots) if path_text.startswith(str(root).lower())), len(roots))
        try:
            stat = path.stat()
            return (root_score, server_score, -stat.st_mtime, -stat.st_size)
        except Exception:
            return (root_score, server_score, 0, 0)
    return sorted(paths, key=score)


def _news_item_timestamp(raw):
    try:
        text = str(raw).strip()
        if not text:
            return 0.0
        for pattern in ("%Y.%m.%d %H:%M:%S", "%Y.%m.%d %H:%M", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"):
            try:
                return datetime.strptime(text, pattern).replace(tzinfo=LOCAL_TZ).timestamp()
            except Exception:
                pass
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
        if parsed.tzinfo is None:
            parsed = parsed.replace(tzinfo=LOCAL_TZ)
        return parsed.astimezone(LOCAL_TZ).timestamp()
    except Exception:
        return 0.0


def _build_news_payload_from_mt5_dat():
    items = []
    seen_titles = set()
    now_dt = datetime.now(LOCAL_TZ)
    cutoff_ts = (datetime.now(LOCAL_TZ) - timedelta(days=NEWS_LOOKBACK_DAYS)).timestamp()
    future_limit_ts = (datetime.now(LOCAL_TZ) + timedelta(hours=6)).timestamp()
    for path in _mt5_news_dat_paths():
        try:
            blob = path.read_bytes()
        except Exception as exc:
            print(f"MT5 news.dat read error ({path}): {exc}")
            continue
        strings = _extract_news_strings(blob)
        if not strings:
            continue
        for index, (position, text) in enumerate(strings):
            provider = next((item for item in NEWS_PROVIDERS if item.lower() in text.lower()), None)
            if not provider:
                continue
            window = strings[max(0, index - 30): min(len(strings), index + 31)]
            date_text = ""
            for _, candidate in window:
                match = NEWS_DATE_RE.search(candidate)
                if match:
                    date_text = match.group(0)
                    break
            title = ""
            before = list(reversed(strings[max(0, index - 8):index]))
            after = strings[index + 1:min(len(strings), index + 9)]
            for _, candidate in before + after:
                if _is_news_title_candidate(candidate):
                    title = candidate
                    break
            if not title:
                continue
            title_key = title.lower()
            if title_key in seen_titles:
                continue
            priority = _terminal_news_priority(title)
            unix_time = _nearby_unix_time(blob, position)
            override_time = _visible_mt5_news_time(title)
            if override_time:
                iso_date_time = override_time
            elif date_text:
                iso_date_time = date_text
            elif unix_time:
                candidate_dt = datetime.fromtimestamp(unix_time, tz=LOCAL_TZ)
                candidate_ts = candidate_dt.timestamp()
                iso_date_time = candidate_dt.isoformat() if cutoff_ts <= candidate_ts <= future_limit_ts else ""
            else:
                iso_date_time = ""
            item_ts = _news_item_timestamp(iso_date_time)
            if not item_ts or item_ts < cutoff_ts or item_ts > future_limit_ts:
                if priority <= 0:
                    continue
                iso_date_time = now_dt.isoformat()
            seen_titles.add(title_key)
            digest = md5(f"{path}:{position}:{title}".encode("utf-8")).hexdigest()[:8]
            items.append({
                "id": int(digest, 16) & 0x7FFFFFFF,
                "title": title,
                "timeLabel": "",
                "isoDateTime": iso_date_time,
                "countryCode": "WW",
                "category": provider,
                "detailsUrl": None,
                "_sourcePosition": position,
                "_terminalPriority": priority,
            })
    items.sort(
        key=lambda item: (
            int(item.get("_terminalPriority", 0)),
            _news_item_timestamp(item.get("isoDateTime", "")),
            int(item.get("_sourcePosition", 0)),
        ),
        reverse=True
    )
    for item in items:
        item.pop("_sourcePosition", None)
        item.pop("_terminalPriority", None)
    if items:
        print(f"MT5 local news.dat: sending {len(items)} items. First: {items[0].get('title')}")
    else:
        print("MT5 local news.dat: no extractable news items found.")
    return {
        "type": "news",
        "items": items[:200],
        "lastUpdatedIso": datetime.now(timezone.utc).isoformat()
    }


def _human_time_label(release_dt):
    now = datetime.now(LOCAL_TZ)
    diff = now - release_dt
    if diff.days == 0:
        if diff.seconds < 3600:
            mins = max(diff.seconds // 60, 1)
            return f"{mins} minutes ago"
        return f"{diff.seconds // 3600} hours ago"
    if diff.days == 1:
        return "yesterday"
    return f"{diff.days} days ago"


def _build_news_payload_from_tradays(news_items):
    cutoff = datetime.now(LOCAL_TZ) - timedelta(days=NEWS_LOOKBACK_DAYS)
    processed_news = []
    for item in news_items:
        if not isinstance(item, dict):
            continue
        release_ms = int(item.get("ReleaseDate", 0))
        release_dt = datetime.fromtimestamp(release_ms / 1000.0, tz=timezone.utc).astimezone(LOCAL_TZ)
        if release_dt < cutoff:
            continue

        processed_news.append({
            "id": int(item.get("Id", 0)),
            "title": sanitize_metric(item.get("Title")),
            "timeLabel": _human_time_label(release_dt),
            "isoDateTime": release_dt.isoformat(),
            "countryCode": COUNTRY_CODE_MAP.get(int(item.get("Country", 0)), ""),
            "category": sanitize_metric(item.get("CategoryName")),
            "detailsUrl": details_url_for_event(item)
        })

    payload = {
        "type": "news",
        "items": processed_news,
        "lastUpdatedIso": datetime.now(timezone.utc).isoformat()
    }
    return payload


def _parse_feed_datetime(value):
    text = sanitize_metric(value)
    if not text:
        return None

    # Google/Forex feeds tend to use RFC822 strings.
    try:
        parsed = parsedate_to_datetime(text)
        if parsed is not None:
            if parsed.tzinfo is None:
                parsed = parsed.replace(tzinfo=timezone.utc)
            return parsed.astimezone(LOCAL_TZ)
    except Exception:
        pass

    # Investing feed often uses "YYYY-MM-DD HH:MM:SS".
    for pattern in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%dT%H:%M:%S"):
        try:
            parsed = datetime.strptime(text, pattern)
            if parsed.tzinfo is None:
                parsed = parsed.replace(tzinfo=timezone.utc)
            return parsed.astimezone(LOCAL_TZ)
        except Exception:
            continue
    return None


def _safe_find_text(item, tag_name):
    element = item.find(tag_name)
    if element is not None and element.text:
        return element.text
    return ""


def _build_fallback_rss_news():
    items = []
    seen_links = set()
    cutoff = datetime.now(LOCAL_TZ) - timedelta(days=NEWS_LOOKBACK_DAYS)

    for source_url, default_category in FALLBACK_NEWS_SOURCES:
        try:
            request = urllib.request.Request(source_url, headers=TRADAYS_HEADERS)
            with urllib.request.urlopen(request, timeout=TRADAYS_REQUEST_TIMEOUT_SECONDS) as response:
                raw = response.read()
            root = ET.fromstring(raw)
        except Exception as exc:
            print(f"RSS fallback fetch error ({source_url}): {exc}")
            continue

        for entry in root.findall(".//item"):
            title = sanitize_metric(_safe_find_text(entry, "title"))
            link = sanitize_metric(_safe_find_text(entry, "link"))
            if not title or not link or link in seen_links:
                continue

            seen_links.add(link)
            release_dt = _parse_feed_datetime(_safe_find_text(entry, "pubDate")) or datetime.now(LOCAL_TZ)
            if release_dt < cutoff:
                continue
            category = sanitize_metric(_safe_find_text(entry, "category")).lower() or default_category
            digest = md5(link.encode("utf-8")).hexdigest()[:8]
            item_id = int(digest, 16) & 0x7FFFFFFF

            items.append({
                "id": item_id,
                "title": title,
                "timeLabel": _human_time_label(release_dt),
                "isoDateTime": release_dt.isoformat(),
                "countryCode": "WW",
                "category": category,
                "detailsUrl": link,
            })

    items.sort(key=lambda item: item["isoDateTime"], reverse=True)
    return items[:FALLBACK_NEWS_LIMIT]


def _build_fxstreet_rss_payload():
    items = []
    cutoff = datetime.now(LOCAL_TZ) - timedelta(days=NEWS_LOOKBACK_DAYS)
    try:
        request = urllib.request.Request(FXSTREET_RSS_URL, headers=TRADAYS_HEADERS)
        with urllib.request.urlopen(request, timeout=TRADAYS_REQUEST_TIMEOUT_SECONDS) as response:
            raw = response.read()
        root = ET.fromstring(raw)
    except Exception as exc:
        print(f"FXStreet RSS fetch error: {exc}")
        return empty_news_payload()

    for entry in root.findall(".//item"):
        title = sanitize_metric(_safe_find_text(entry, "title"))
        link = sanitize_metric(_safe_find_text(entry, "link"))
        guid = sanitize_metric(_safe_find_text(entry, "guid"))
        release_dt = _parse_feed_datetime(_safe_find_text(entry, "pubDate"))
        if not title or not release_dt or release_dt < cutoff:
            continue
        digest = md5((guid or link or title).encode("utf-8")).hexdigest()[:8]
        items.append({
            "id": int(digest, 16) & 0x7FFFFFFF,
            "title": title,
            "timeLabel": _human_time_label(release_dt),
            "isoDateTime": release_dt.isoformat(),
            "countryCode": "WW",
            "category": "FXStreet",
            "detailsUrl": link,
        })

    items.sort(key=lambda item: _news_item_timestamp(item.get("isoDateTime", "")), reverse=True)
    if items:
        print(f"FXStreet RSS: sending {len(items)} items. First: {items[0].get('title')}")
    else:
        print("FXStreet RSS returned no recent items.")
    return {
        "type": "news",
        "items": items[:200],
        "lastUpdatedIso": datetime.now(timezone.utc).isoformat()
    }


def _merge_news_items(primary_items, secondary_items, limit):
    merged = []
    seen = set()

    for item in primary_items + secondary_items:
        if not isinstance(item, dict):
            continue
        key = (sanitize_metric(item.get("detailsUrl")), sanitize_metric(item.get("title")))
        if key in seen:
            continue
        seen.add(key)
        merged.append(item)

    merged.sort(key=lambda item: sanitize_metric(item.get("isoDateTime")), reverse=True)
    return merged[:limit]


def build_news_payload():
    fxstreet_payload = _build_fxstreet_rss_payload()
    if fxstreet_payload.get("items"):
        return fxstreet_payload
    return _build_news_payload_from_mt5_dat()


def build_calendar_payload(selected_date):
    range_start, range_end = month_bounds(selected_date)
    try:
        all_events = normalize_tradays_calendar(fetch_tradays_calendar(range_start, range_end))
        print(f"Tradays calendar: received {len(all_events)} events for {range_start} to {range_end}")
    except Exception as exc:
        print(f"Tradays calendar fetch unavailable: {exc}")
        all_events = []
    selected_date_iso = selected_date.isoformat()
    generated_at = datetime.now(timezone.utc).isoformat()

    day_chips = []
    cursor = range_start
    today_local = datetime.now(LOCAL_TZ).date()
    while cursor <= range_end:
        day_chips.append(
            {
                "isoDate": cursor.isoformat(),
                "dayNumber": cursor.day,
                "dayLabel": DAY_LABELS[cursor.weekday()],
                "isSelected": cursor == selected_date,
                "isToday": cursor == today_local,
            }
        )
        cursor += timedelta(days=1)

    display_events = []
    ai_events = []
    for event in all_events:
        if not isinstance(event, dict):
            continue
        if not sanitize_metric(event.get("EventName")):
            continue
        release_ms = int(event.get("ReleaseDate", 0))
        if release_ms <= 0:
            continue
        country_code = country_code_for_event(event)
        country_name = country_name_for_event(event, country_code)
        iso_date_time = iso_datetime_from_release(release_ms)
        event_date_iso = local_date_iso_for_event(event)
        shared = {
            "id": int(event.get("Id", 0)),
            "isoDateTime": iso_date_time,
            "dateIso": event_date_iso,
            "currencyCode": sanitize_metric(event.get("CurrencyCode")) or "ALL",
            "countryCode": country_code,
            "countryName": country_name,
            "title": sanitize_metric(event.get("EventName")),
            "importance": sanitize_metric(event.get("Importance")) or "none",
            "actual": sanitize_metric(event.get("ActualValue")),
            "forecast": sanitize_metric(event.get("ForecastValue")),
            "previous": sanitize_metric(event.get("PreviousValue")),
            "impactDirection": int(event.get("ImpactDirection", 0)),
            "eventType": int(event.get("EventType", 0)),
            "timeMode": int(event.get("TimeMode", 0)),
            "processed": int(event.get("Processed", 0)) == 1,
            "detailsUrl": details_url_for_event(event),
        }
        ai_events.append(shared)

        display_events.append(
            {
                "id": shared["id"],
                "isoDateTime": shared["isoDateTime"],
                "releaseTimeLabel": display_time_label(event),
                "countryCode": shared["countryCode"],
                "countryName": shared["countryName"],
                "currencyCode": shared["currencyCode"],
                "title": shared["title"],
                "actual": shared["actual"],
                "forecast": shared["forecast"],
                "previous": shared["previous"],
                "importance": shared["importance"],
                "impactDirection": shared["impactDirection"],
                "isSpeechOrReport": shared["eventType"] == 0,
                "isAllDay": shared["timeMode"] in (1, 2),
                "detailsUrl": shared["detailsUrl"],
            }
        )

    display_events.sort(key=lambda item: item["isoDateTime"])
    ai_events.sort(key=lambda item: item["isoDateTime"])

    return {
        "type": "calendar",
        "display": {
            "sourceLabel": "MT5 Calendar",
            "rangeStartIso": range_start.isoformat(),
            "rangeEndIso": range_end.isoformat(),
            "selectedDateIso": selected_date_iso,
            "headerDateLabel": header_date_label(selected_date),
            "dayChips": day_chips,
            "events": display_events,
            "lastUpdatedIso": generated_at,
        },
        "ai": {
            "source": "mt5-tradays",
            "generatedAtIso": generated_at,
            "selectedDateIso": selected_date_iso,
            "rangeStartIso": range_start.isoformat(),
            "rangeEndIso": range_end.isoformat(),
            "events": ai_events,
        },
    }


def build_history_payload(symbol, timeframe, end_time=None, count=HISTORY_CHUNK_SIZE):
    # Ensure symbol is selected in Market Watch for history access
    resolved = resolve_symbol(symbol)
    if not mt5.symbol_select(resolved, True):
        print(f"Failed to select symbol: {resolved}")
        return None

    if end_time is not None:
        try:
            end_time_value = int(end_time)
        except (TypeError, ValueError):
            end_time_value = 0

        if end_time_value > 0:
            history_end = datetime.fromtimestamp(end_time_value, tz=timezone.utc)
            rates = mt5.copy_rates_from(resolved, timeframe, history_end, count)
        else:
            rates = mt5.copy_rates_from_pos(resolved, timeframe, 0, count)
    else:
        rates = mt5.copy_rates_from_pos(resolved, timeframe, 0, count)

    if rates is None or len(rates) == 0:
        print(f"copy_rates failed for {resolved}")
        return None

    print(f"History: {resolved} - retrieved {len(rates)} candles (requested {count})")

    history = []
    for rate in rates:
        tick_volume = 0.0
        real_volume = 0.0
        try:
            tick_volume = float(rate["tick_volume"])
        except Exception:
            try:
                tick_volume = float(rate[5])
            except Exception:
                tick_volume = 0.0
        try:
            real_volume = float(rate["real_volume"])
        except Exception:
            try:
                real_volume = float(rate[7])
            except Exception:
                real_volume = 0.0

        history.append(
            {
                "time": int(rate[0]),
                "open": float(rate[1]),
                "high": float(rate[2]),
                "low": float(rate[3]),
                "close": float(rate[4]),
                "volume": real_volume if real_volume > 0 else tick_volume,
            }
        )

    return {
        "type": "history",
        "symbol": clean_symbol(symbol),
        "data": history,
    }


def _rate_field(rate, field_name, field_index, default_value=0.0):
    try:
        return float(rate[field_name])
    except Exception:
        try:
            return float(rate[field_index])
        except Exception:
            return default_value


# Cache for daily data to avoid redundant MT5 calls during high-frequency polling
DAILY_DATA_CACHE = {}

def get_daily_info(symbol):
    now = time.time()
    if symbol in DAILY_DATA_CACHE:
        cached_time, data = DAILY_DATA_CACHE[symbol]
        # Refresh every hour
        if now - cached_time < 3600:
            return data

    daily_rates = mt5.copy_rates_from_pos(symbol, mt5.TIMEFRAME_D1, 0, 3)
    if daily_rates is not None and len(daily_rates) >= 2:
        latest_bar = daily_rates[-1]
        previous_bar = daily_rates[-2]

        data = {
            "open": _rate_field(latest_bar, "open", 1, 0.0),
            "high": _rate_field(latest_bar, "high", 2, 0.0),
            "low": _rate_field(latest_bar, "low", 3, 0.0),
            "close": _rate_field(latest_bar, "close", 4, 0.0),
            "prev_close": _rate_field(previous_bar, "close", 4, 0.0),
            "time": int(_rate_field(latest_bar, "time", 0, 0))
        }
        DAILY_DATA_CACHE[symbol] = (now, data)
        return data
    return None

def build_tick_payload(symbol):
    resolved = resolve_symbol(symbol)
    mt5.symbol_select(resolved, True)

    tick = mt5.symbol_info_tick(resolved)
    if tick is None:
        return None

    info = mt5.symbol_info(resolved)
    daily = get_daily_info(resolved)

    bar_open = daily["open"] if daily else 0.0
    bar_high = daily["high"] if daily else 0.0
    bar_low = daily["low"] if daily else 0.0
    bar_close = daily["close"] if daily else 0.0
    prev_close = daily["prev_close"] if daily else (bar_open or bar_close)
    bar_time = daily["time"] if daily else 0

    bid = float(getattr(tick, "bid", 0.0) or 0.0)
    ask = float(getattr(tick, "ask", 0.0) or 0.0)
    last_from_tick = float(getattr(tick, "last", 0.0) or 0.0)
    volume = float(getattr(tick, "volume", 0.0) or 0.0)
    tick_time = int(getattr(tick, "time", 0) or 0)

    if info is not None:
        if bid <= 0.0:
            bid = float(getattr(info, "bid", 0.0) or 0.0)
        if ask <= 0.0:
            ask = float(getattr(info, "ask", 0.0) or 0.0)
        if last_from_tick <= 0.0:
            last_from_tick = float(getattr(info, "last", 0.0) or 0.0)

    last_price_candidates = [value for value in [bid, ask, last_from_tick, bar_close, bar_open] if value and value > 0.0]
    if not last_price_candidates:
        return None

    last_price = last_price_candidates[0]

    if bid <= 0.0:
        bid = last_price
    if ask <= 0.0:
        ask = last_price
    if tick_time <= 0:
        tick_time = bar_time

    spread = max(ask - bid, 0.0)
    open_price = bar_open if bar_open > 0.0 else last_price
    high_price = bar_high if bar_high > 0.0 else last_price
    low_price = bar_low if bar_low > 0.0 else last_price
    prev_close = prev_close if prev_close and prev_close > 0.0 else open_price

    change = last_price - prev_close
    change_percent = (change / prev_close * 100.0) if prev_close else 0.0

    return {
        "type": "tick",
        "symbol": clean_symbol(resolved),
        "name": clean_symbol(resolved),
        "lastPrice": last_price,
        "bid": bid,
        "ask": ask,
        "open": open_price,
        "high": high_price,
        "low": low_price,
        "prevClose": prev_close,
        "change": change,
        "changePercent": change_percent,
        "volume": volume,
        "spread": spread,
        "time": tick_time,
    }


async def handle_client(websocket):
    print(f"Android connected: {websocket.remote_address}")
    current_symbol = resolve_symbol("BTCUSD")
    mt5.symbol_select(current_symbol, True)
    current_tf = mt5.TIMEFRAME_H1
    watchlist_symbols = {current_symbol}
    calendar_selected_date = datetime.now(LOCAL_TZ).date()
    last_calendar_refresh = 0.0
    calendar_refresh_task = None

    def stream_symbols():
        symbols = set(watchlist_symbols)
        symbols.add(current_symbol)
        return sorted(symbols)

    async def send_history(symbol, timeframe, end_time=None, count=HISTORY_CHUNK_SIZE):
        payload = build_history_payload(symbol, timeframe, end_time, count)
        if payload is None:
            print(f"Failed to get rates for {symbol}")
            return
        await websocket.send(json.dumps(payload))

    async def send_calendar(selected_date=None, force=False):
        nonlocal calendar_selected_date, last_calendar_refresh
        if selected_date is not None:
            calendar_selected_date = selected_date

        now_monotonic = time.monotonic()
        if not force and now_monotonic - last_calendar_refresh < CALENDAR_REFRESH_SECONDS:
            return
        # Reserve refresh slot so failures do not trigger a retry storm.
        last_calendar_refresh = now_monotonic

        try:
            payload = await asyncio.to_thread(build_calendar_payload, calendar_selected_date)
            await websocket.send(json.dumps(payload, ensure_ascii=False))
        except Exception as exc:
            print(f"Calendar fetch error: {exc}")

    async def send_news():
        payload = empty_news_payload()
        try:
            payload = await asyncio.to_thread(build_news_payload)
        except Exception as exc:
            print(f"News fetch error: {exc}")
        finally:
            try:
                await websocket.send(json.dumps(payload, ensure_ascii=False))
                items = payload.get("items", [])
                first_title = items[0].get("title") if items else "None"
                print(f"News sent to Android: {len(items)} items. First: {first_title}")
            except Exception as send_exc:
                print(f"News send error: {send_exc}")
            await asyncio.to_thread(write_news_snapshot, payload)

    await send_history(current_symbol, current_tf)
    # Keep stream startup fast; external data fetches should not block candles/ticks.
    calendar_refresh_task = asyncio.create_task(send_calendar(force=True))
    asyncio.create_task(send_news())

    async def listen():
        nonlocal current_symbol, current_tf, watchlist_symbols
        try:
            async for message in websocket:
                try:
                    print(f"Android message: {message}")
                    data = json.loads(message)
                    action = data.get("action")
                    print(f"Android action: {action}")

                    if action == "subscribe":
                        current_symbol = resolve_symbol(data.get("symbol", current_symbol))
                        mt5.symbol_select(current_symbol, True)
                        current_tf = TIMEFRAME_MAP.get(data.get("timeframe", "1h"), mt5.TIMEFRAME_H1)
                        watchlist_symbols.add(current_symbol)
                        count = int(data.get("count", data.get("limit", HISTORY_CHUNK_SIZE)))
                        await send_history(current_symbol, current_tf, data.get("end_time"), count)

                    elif action == "watchlist_update":
                        symbols = data.get("symbols", [])
                        if not isinstance(symbols, list):
                            symbols = []
                        resolved_symbols = []
                        for item in symbols:
                            symbol_value = str(item).strip()
                            if not symbol_value:
                                continue
                            res = resolve_symbol(symbol_value)
                            # Force selection to keep it "hot" in MT5
                            mt5.symbol_select(res, True)
                            resolved_symbols.append(res)

                        # Keep the background stream sticky so symbols stay hot after chart switches.
                        watchlist_symbols.update(resolved_symbols)

                        # Immediate update for the expanded watchlist
                        for watched_symbol in stream_symbols():
                            tick_payload = build_tick_payload(watched_symbol)
                            if tick_payload:
                                await websocket.send(json.dumps(tick_payload))

                    elif action == "get_calendar":
                        await send_calendar(parse_selected_date(data.get("selectedDate")), force=True)

                    elif action == "get_news":
                        await send_news()

                    elif action == "get_symbols":
                        all_symbols = mt5.symbols_get()
                        if all_symbols:
                            payload = {
                                "type": "symbols",
                                "data": [
                                    {
                                        "ticker": clean_symbol(s.name),
                                        "symbol": s.name,
                                        "name": s.description if s.description else clean_symbol(s.name),
                                        "type": "forex" if s.path.startswith("Forex") else "crypto" if "Crypto" in s.path else "stock"
                                    }
                                    for s in all_symbols
                                ]
                            }
                            await websocket.send(json.dumps(payload))

                    elif action == "place_order":
                        matched_symbol = resolve_symbol(data.get("symbol", ""))
                        side = data.get("type", "buy").lower()
                        order_category = data.get("orderType", "market").lower()
                        volume = float(data.get("volume", 0.01))
                        price = float(data.get("price", 0))
                        tp = float(data.get("tp", 0))
                        sl = float(data.get("sl", 0))

                        current_tick = mt5.symbol_info_tick(matched_symbol)
                        if current_tick is None:
                            await websocket.send(
                                json.dumps(
                                    {
                                        "type": "order_result",
                                        "status": "failed",
                                        "error": "No tick data",
                                    }
                                )
                            )
                            continue

                        trade_action = mt5.TRADE_ACTION_DEAL
                        mt5_type = mt5.ORDER_TYPE_BUY if side == "buy" else mt5.ORDER_TYPE_SELL
                        exec_price = current_tick.ask if side == "buy" else current_tick.bid
                        filling_type = mt5.ORDER_FILLING_IOC

                        if order_category == "limit":
                            trade_action = mt5.TRADE_ACTION_PENDING
                            mt5_type = (
                                mt5.ORDER_TYPE_BUY_LIMIT if side == "buy" else mt5.ORDER_TYPE_SELL_LIMIT
                            )
                            exec_price = price
                            filling_type = mt5.ORDER_FILLING_RETURN
                        elif order_category == "stop":
                            trade_action = mt5.TRADE_ACTION_PENDING
                            mt5_type = (
                                mt5.ORDER_TYPE_BUY_STOP if side == "buy" else mt5.ORDER_TYPE_SELL_STOP
                            )
                            exec_price = price
                            filling_type = mt5.ORDER_FILLING_RETURN
                        elif order_category == "stoplimit":
                            trade_action = mt5.TRADE_ACTION_PENDING
                            mt5_type = (
                                mt5.ORDER_TYPE_BUY_STOP_LIMIT
                                if side == "buy"
                                else mt5.ORDER_TYPE_SELL_STOP_LIMIT
                            )
                            exec_price = price
                            filling_type = mt5.ORDER_FILLING_RETURN

                        request = {
                            "action": trade_action,
                            "symbol": matched_symbol,
                            "volume": volume,
                            "type": mt5_type,
                            "price": exec_price,
                            "magic": 123456,
                            "comment": data.get("comment", "App Order"),
                            "type_time": mt5.ORDER_TIME_GTC,
                            "type_filling": filling_type,
                        }

                        if order_category == "stoplimit":
                            request["stoplimit"] = float(data.get("stopLimitPrice", 0))
                        if tp > 0:
                            request["tp"] = tp
                        if sl > 0:
                            request["sl"] = sl

                        result = mt5.order_send(request)
                        if result and result.retcode == mt5.TRADE_RETCODE_DONE:
                            await websocket.send(
                                json.dumps(
                                    {
                                        "type": "order_result",
                                        "status": "success",
                                        "ticket": result.order
                                        if hasattr(result, "order")
                                        else result.request.order,
                                        "price": result.price,
                                        "volume": result.volume,
                                    }
                                )
                            )
                        else:
                            error_msg = result.comment if result else "Order failed"
                            await websocket.send(
                                json.dumps(
                                    {
                                        "type": "order_result",
                                        "status": "failed",
                                        "error": error_msg,
                                    }
                                )
                            )
                        print(
                            "Order Action: "
                            f"{trade_action}, Result: {result.comment if result else 'Failed'}"
                        )

                    elif action == "close_position":
                        ticket = int(data.get("ticket"))
                        positions = mt5.positions_get(ticket=ticket)
                        if positions:
                            position = positions[0]
                            order_type = (
                                mt5.ORDER_TYPE_SELL
                                if position.type == mt5.POSITION_TYPE_BUY
                                else mt5.ORDER_TYPE_BUY
                            )
                            tick = mt5.symbol_info_tick(position.symbol)
                            price = tick.bid if position.type == mt5.POSITION_TYPE_BUY else tick.ask
                            request = {
                                "action": mt5.TRADE_ACTION_DEAL,
                                "symbol": position.symbol,
                                "volume": float(data.get("volume", position.volume)),
                                "type": order_type,
                                "position": ticket,
                                "price": price,
                                "magic": 123456,
                                "comment": "App Close",
                                "type_time": mt5.ORDER_TIME_GTC,
                                "type_filling": mt5.ORDER_FILLING_IOC,
                            }
                            mt5.order_send(request)
                        else:
                            print(f"Position {ticket} not found")

                    elif action == "modify_position":
                        ticket = int(data.get("ticket"))
                        request = {
                            "action": mt5.TRADE_ACTION_SLTP,
                            "position": ticket,
                            "tp": float(data.get("tp", 0)),
                            "sl": float(data.get("sl", 0)),
                        }
                        mt5.order_send(request)

                except Exception as exc:
                    print(f"Listen error: {exc}")
        except websockets.exceptions.ConnectionClosed:
            pass

    async def stream():
        nonlocal last_calendar_refresh, calendar_refresh_task, watchlist_symbols
        last_account_refresh = 0.0
        while True:
            try:
                # 1. High-frequency Tick Updates (every iteration)
                for watched_symbol in stream_symbols():
                    try:
                        tick_payload = build_tick_payload(watched_symbol)
                        if tick_payload:
                            await websocket.send(json.dumps(tick_payload))
                    except Exception as symbol_exc:
                        print(f"Tick error for {watched_symbol}: {symbol_exc}")

                # 2. Lower-frequency Account/Position Updates (every ~2 seconds)
                now_monotonic = time.monotonic()
                if now_monotonic - last_account_refresh >= 2.0:
                    last_account_refresh = now_monotonic

                    account = mt5.account_info()
                    terminal = mt5.terminal_info()
                    if account and terminal:
                        await websocket.send(
                            json.dumps(
                                {
                                    "type": "account",
                                    "balance": float(account.balance),
                                    "equity": float(account.equity),
                                    "unrealizedPnl": float(account.profit),
                                    "margin": float(account.margin),
                                    "availableFunds": float(account.margin_free),
                                    "trade_allowed": terminal.trade_allowed,
                                }
                            )
                        )

                    positions = mt5.positions_get()
                    if positions is not None:
                        await websocket.send(
                            json.dumps(
                                {
                                    "type": "positions",
                                    "data": [
                                        {
                                            "ticket": item.ticket,
                                            "symbol": clean_symbol(item.symbol),
                                            "type": "buy"
                                            if item.type == mt5.POSITION_TYPE_BUY
                                            else "sell",
                                            "price_open": float(item.price_open),
                                            "volume_current": float(item.volume),
                                            "time_setup": int(item.time) * 1000,
                                            "tp": float(item.tp),
                                            "sl": float(item.sl),
                                            "profit": float(item.profit),
                                        }
                                        for item in positions
                                    ],
                                }
                            )
                        )

                    orders = mt5.orders_get()
                    if orders is not None:
                        type_map = {
                            mt5.ORDER_TYPE_BUY_LIMIT: "Buy Limit",
                            mt5.ORDER_TYPE_SELL_LIMIT: "Sell Limit",
                            mt5.ORDER_TYPE_BUY_STOP: "Buy Stop",
                            mt5.ORDER_TYPE_SELL_STOP: "Sell Stop",
                            mt5.ORDER_TYPE_BUY_STOP_LIMIT: "Buy Stop Limit",
                            mt5.ORDER_TYPE_SELL_STOP_LIMIT: "Sell Stop Limit",
                        }
                        await websocket.send(
                            json.dumps(
                                {
                                    "type": "orders",
                                    "data": [
                                        {
                                            "ticket": item.ticket,
                                            "symbol": clean_symbol(item.symbol),
                                            "type": "buy" if item.type in [0, 2, 4, 6] else "sell",
                                            "type_name": type_map.get(item.type, "Pending"),
                                            "price_open": float(item.price_open),
                                            "volume_initial": float(item.volume_initial),
                                            "time_setup": int(item.time_setup) * 1000,
                                            "status": "Working",
                                        }
                                        for item in orders
                                    ],
                                }
                            )
                        )

                if time.monotonic() - last_calendar_refresh >= CALENDAR_REFRESH_SECONDS:
                    if calendar_refresh_task is None or calendar_refresh_task.done():
                        calendar_refresh_task = asyncio.create_task(send_calendar(force=True))

            except Exception as exc:
                if isinstance(exc, websockets.exceptions.ConnectionClosed):
                    break
                print(f"Stream error: {exc}")

            # Polling delay: 0.2s keeps the stream responsive and symbols "hot" without pinning CPU
            await asyncio.sleep(0.2)

    try:
        await asyncio.gather(listen(), stream())
    except Exception:
        pass


async def main():
    print("MT5 WebSocket bridge listening on ws://0.0.0.0:8081")
    async with websockets.serve(handle_client, "0.0.0.0", 8081):
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        mt5.shutdown()
