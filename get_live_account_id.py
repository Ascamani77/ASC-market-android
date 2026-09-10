"""
Get the API account ID for live account 1360716
The visible account ID (1360716) is different from the API's ctidTraderAccountId
"""
import os
import json
from ctrader_open_api import Client, EndPoints, Protobuf, TcpProtocol
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor

# Credentials
CLIENT_ID = os.environ.get("CTRADER_CLIENT_ID", "")
CLIENT_SECRET = os.environ.get("CTRADER_CLIENT_SECRET", "")

# Try to load token from the new live token file
try:
    with open("ctrader_tokens_live.json", "r") as f:
        token_data = json.load(f)
        ACCESS_TOKEN = token_data.get("access_token") or token_data.get("accessToken")
        print(f"✅ Loaded token from ctrader_tokens_live.json")
except:
    # Fallback to old token file
    try:
        with open("ctrader_tokens.json", "r") as f:
            token_data = json.load(f)
            ACCESS_TOKEN = token_data.get("access_token") or token_data.get("accessToken")
            print(f"⚠️  Using token from ctrader_tokens.json (may be for demo account)")
    except:
        print("❌ No token file found. Run get_live_token.ps1 first!")
        exit(1)

TARGET_VISIBLE_ID = 1360716

def on_connected(client):
    print("✅ Connected to cTrader API (live endpoint)")
    
    # Send application auth request
    request = ProtoOAApplicationAuthReq()
    request.clientId = CLIENT_ID
    request.clientSecret = CLIENT_SECRET
    client.send(request)

def on_disconnected(client, reason):
    print(f"Disconnected: {reason}")

def on_message_received(client, message):
    payload = Protobuf.extract(message)
    
    if message.payloadType == ProtoOAApplicationAuthRes().payloadType:
        print("✅ Application authenticated")
        
        # Request account list
        request = ProtoOAGetAccountListByAccessTokenReq()
        request.accessToken = ACCESS_TOKEN
        client.send(request)
    
    elif message.payloadType == ProtoOAGetAccountListByAccessTokenRes().payloadType:
        print("\n" + "="*70)
        print("Accounts Authorized for This Token:")
        print("="*70)
        
        accounts = list(getattr(payload, "ctidTraderAccount", []))
        
        if not accounts:
            print("\n❌ No accounts found for this access token")
            print("\nThis means:")
            print("  1. The token was not authorized for any account")
            print("  2. The token may have expired")
            print("  3. You need to generate a new token with get_live_token.ps1")
        else:
            found_target = False
            for i, account in enumerate(accounts, 1):
                account_id = getattr(account, "ctidTraderAccountId", None)
                is_live = getattr(account, "isLive", False)
                broker = getattr(account, "brokerName", "Unknown")
                
                print(f"\nAccount {i}:")
                print(f"  API Account ID (ctidTraderAccountId): {account_id}")
                print(f"  Type: {'LIVE' if is_live else 'DEMO'}")
                print(f"  Broker: {broker}")
                
                if is_live:
                    found_target = True
                    print(f"\n  🎯 LIVE ACCOUNT FOUND!")
                    print(f"  ✅ USE THIS IN YOUR CONFIG: {account_id}")
                    print(f"\n  Update start_ctrader_bridge.ps1:")
                    print(f"     $env:CTRADER_ACCOUNT_ID = \"{account_id}\"")
                    print(f"     $env:CTRADER_HOST_TYPE = \"live\"")
            
            if not found_target:
                print(f"\n⚠️  WARNING: No LIVE account found in token!")
                print(f"   Expected live account {TARGET_VISIBLE_ID}")
                print(f"   But token only has access to DEMO accounts")
                print(f"\n   Solution:")
                print(f"   1. Run: .\\get_live_token.ps1")
                print(f"   2. When authorizing, SELECT ACCOUNT {TARGET_VISIBLE_ID}")
                print(f"   3. Make sure it shows as LIVE account")
        
        print("\n" + "="*70)
        reactor.stop()
    
    elif message.payloadType == ProtoOAErrorRes().payloadType:
        error = ProtoOAErrorRes()
        error.ParseFromString(message.payload)
        print(f"\n❌ Error: {error.errorCode} - {error.description}")
        
        if error.errorCode == "CANT_ROUTE_REQUEST":
            print("\nThis error means the account is not accessible.")
            print("Generate a new token with: .\\get_live_token.ps1")
        
        reactor.stop()

def timeout():
    print("\n❌ Timeout - no response from cTrader API")
    reactor.stop()

print("="*70)
print("Finding API Account ID for Live Account 1360716")
print("="*70)
print(f"Access Token: {ACCESS_TOKEN[:30]}...")
print(f"Connecting to: {EndPoints.PROTOBUF_LIVE_HOST}:{EndPoints.PROTOBUF_PORT}")
print("="*70)

client = Client(EndPoints.PROTOBUF_LIVE_HOST, EndPoints.PROTOBUF_PORT, TcpProtocol)
client.setConnectedCallback(on_connected)
client.setDisconnectedCallback(on_disconnected)
client.setMessageReceivedCallback(on_message_received)

reactor.callLater(10, timeout)

client.startService()
reactor.run(installSignalHandlers=False)
