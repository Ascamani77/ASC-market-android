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
HOST_TYPE = "demo"  # FIXED TO DEMO
BRIDGE_HOST = os.getenv("CTRADER_BRIDGE_HOST", "0.0.0.0").strip()
BRIDGE_PORT = int(os.getenv("CTRADER_BRIDGE_PORT", "8083"))  # Default to 8083 for demo
PRICE_SCALE = float(os.getenv("CTRADER_PRICE_SCALE", "100000"))

print(f"[cTrader Demo] Starting Pepperstone cTrader DEMO bridge on ws://{BRIDGE_HOST}:{BRIDGE_PORT}")
print(f"[cTrader Demo] Using DEMO endpoint (demo.ctraderapi.com)")

# Rest of the code is identical to ctrader_bridge.py
# Copy the entire content from ctrader_bridge.py here, but with HOST_TYPE fixed to "demo"
# and all print statements prefixed with "[cTrader Demo]"
