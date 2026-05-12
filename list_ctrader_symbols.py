import os
from ctrader_open_api import Client, Protobuf, TcpProtocol, EndPoints
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor

CLIENT_ID = os.getenv("CTRADER_CLIENT_ID", "").strip()
CLIENT_SECRET = os.getenv("CTRADER_CLIENT_SECRET", "").strip()
ACCESS_TOKEN = os.getenv("CTRADER_ACCESS_TOKEN", "").strip()
HOST_TYPE = os.getenv("CTRADER_HOST_TYPE", "demo").strip().lower()

account_id = None
client = None

def connected(_client):
    global client
    client = _client
    print("Connected to cTrader")
    request = ProtoOAApplicationAuthReq()
    request.clientId = CLIENT_ID
    request.clientSecret = CLIENT_SECRET
    deferred = client.send(request)
    deferred.addErrback(on_error)

def disconnected(_client, reason):
    print(f"Disconnected: {reason}")

def on_error(failure):
    print(f"Error: {failure}")
    reactor.stop()

def on_message_received(_client, message):
    global account_id
    payload = Protobuf.extract(message)
    
    if isinstance(payload, ProtoOAApplicationAuthRes):
        print("Application authenticated")
        request = ProtoOAGetAccountListByAccessTokenReq()
        request.accessToken = ACCESS_TOKEN
        deferred = client.send(request)
        deferred.addErrback(on_error)
    
    elif isinstance(payload, ProtoOAGetAccountListByAccessTokenRes):
        accounts = list(getattr(payload, "ctidTraderAccount", []))
        if accounts:
            account_id = int(getattr(accounts[0], "ctidTraderAccountId"))
            print(f"Account ID: {account_id}")
            request = ProtoOAAccountAuthReq()
            request.ctidTraderAccountId = account_id
            request.accessToken = ACCESS_TOKEN
            deferred = client.send(request)
            deferred.addErrback(on_error)
    
    elif isinstance(payload, ProtoOAAccountAuthRes):
        print("Account authenticated")
        request = ProtoOASymbolsListReq()
        request.ctidTraderAccountId = account_id
        request.includeArchivedSymbols = False
        deferred = client.send(request)
        deferred.addErrback(on_error)
    
    elif isinstance(payload, ProtoOASymbolsListRes):
        print("\n" + "="*80)
        print("SEARCHING FOR STOCK SYMBOLS (AAPL, MSFT, AMZN, NVDA, TSLA)")
        print("="*80)
        
        stock_keywords = ["AAPL", "APPLE", "MSFT", "MICROSOFT", "AMZN", "AMAZON", 
                         "NVDA", "NVIDIA", "TSLA", "TESLA", "GOOGL", "GOOGLE", 
                         "META", "FACEBOOK", "NFLX", "NETFLIX"]
        
        found_stocks = []
        
        for item in getattr(payload, "symbol", []):
            symbol_name = str(getattr(item, "symbolName", "") or getattr(item, "displayName", "") or "").upper()
            symbol_id = int(getattr(item, "symbolId", 0) or 0)
            
            for keyword in stock_keywords:
                if keyword in symbol_name:
                    found_stocks.append((symbol_name, symbol_id))
                    break
        
        if found_stocks:
            print(f"\nFound {len(found_stocks)} potential stock symbols:\n")
            for name, sid in sorted(found_stocks):
                print(f"  {name:<40} (ID: {sid})")
        else:
            print("\nNo stock symbols found matching keywords")
            print("\nShowing first 50 symbols as reference:")
            for i, item in enumerate(getattr(payload, "symbol", [])[:50]):
                symbol_name = str(getattr(item, "symbolName", "") or getattr(item, "displayName", "") or "")
                print(f"  {symbol_name}")
        
        print("\n" + "="*80)
        reactor.stop()
    
    elif isinstance(payload, ProtoOAErrorRes):
        print(f"Error: {payload.errorCode} - {payload.description}")
        reactor.stop()

host = EndPoints.PROTOBUF_LIVE_HOST if HOST_TYPE == "live" else EndPoints.PROTOBUF_DEMO_HOST
client_obj = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
client_obj.setConnectedCallback(connected)
client_obj.setDisconnectedCallback(disconnected)
client_obj.setMessageReceivedCallback(on_message_received)

print("Connecting to cTrader to list stock symbols...")
client_obj.startService()
reactor.run()
