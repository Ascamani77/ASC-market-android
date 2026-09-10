"""
List all cTrader accounts authorized for the access token
"""
import os
import json
from ctrader_open_api import Client, EndPoints, Protobuf, TcpProtocol
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor

# Credentials
CLIENT_ID = os.environ.get("CTRADER_CLIENT_ID", "")
CLIENT_SECRET = os.environ.get("CTRADER_CLIENT_SECRET", "")
# Read token from bridge script or use directly
ACCESS_TOKEN = "mW5eKJ0xZygSj4BnNvqlteOc_opHJ0EM9Qtcx8RvOfo"

def on_connected(client):
    print("✅ Connected to cTrader API")
    
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
        print("\n" + "="*60)
        print("Authorized Accounts:")
        print("="*60)
        
        accounts = list(getattr(payload, "ctidTraderAccount", []))
        
        if not accounts:
            print("\n❌ No accounts found for this access token")
        else:
            for i, account in enumerate(accounts, 1):
                account_id = getattr(account, "ctidTraderAccountId", None)
                is_live = getattr(account, "isLive", False)
                broker = getattr(account, "brokerName", "Unknown")
                print(f"\nAccount {i}:")
                print(f"  Account ID: {account_id}")
                print(f"  Type: {'LIVE' if is_live else 'DEMO'}")
                print(f"  Broker: {broker}")
                
                if not is_live:
                    print(f"\n  ✅ USE THIS ACCOUNT ID IN YOUR CONFIG: {account_id}")
        
        print("\n" + "="*60)
        reactor.stop()
    
    elif message.payloadType == ProtoOAErrorRes().payloadType:
        error = ProtoOAErrorRes()
        error.ParseFromString(message.payload)
        print(f"\n❌ Error: {error.errorCode} - {error.description}")
        reactor.stop()

def timeout():
    print("\n❌ Timeout - no response from cTrader API")
    reactor.stop()

print("="*60)
print("Listing cTrader Accounts")
print("="*60)
print(f"Access Token: {ACCESS_TOKEN[:30]}...")

host = EndPoints.PROTOBUF_LIVE_HOST
print(f"Connecting to: {host}:{EndPoints.PROTOBUF_PORT}")
print("="*60)

client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
client.setConnectedCallback(on_connected)
client.setDisconnectedCallback(on_disconnected)
client.setMessageReceivedCallback(on_message_received)

reactor.callLater(10, timeout)

client.startService()
reactor.run(installSignalHandlers=False)
