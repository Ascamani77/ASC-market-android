"""
List all cTrader accounts authorized for the access token
"""
import os
from ctrader_open_api import Client, EndPoints, Protobuf, TcpProtocol
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor

CLIENT_ID = os.getenv("CTRADER_CLIENT_ID", "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s")
CLIENT_SECRET = os.getenv("CTRADER_CLIENT_SECRET", "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty")
ACCESS_TOKEN = os.getenv("CTRADER_ACCESS_TOKEN", "bN-GI8U-FyVC-b_nS9iAPHE3F-d_1GGa4XgidKRH3aw")
HOST_TYPE = os.getenv("CTRADER_HOST_TYPE", "live").lower()

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
            print("❌ No accounts found for this access token")
        else:
            for i, account in enumerate(accounts, 1):
                account_id = getattr(account, "ctidTraderAccountId", None)
                is_live = getattr(account, "isLive", False)
                broker = getattr(account, "brokerName", "Unknown")
                
                print(f"\nAccount {i}:")
                print(f"  Account ID: {account_id}")
                print(f"  Type: {'LIVE' if is_live else 'DEMO'}")
                print(f"  Broker: {broker}")
        
        print("="*60)
        reactor.stop()
    
    elif message.payloadType == ProtoOAErrorRes().payloadType:
        print(f"❌ Error: {payload}")
        reactor.stop()

print("="*60)
print("Listing cTrader Accounts")
print("="*60)
print(f"Environment: {HOST_TYPE.upper()}")

host = EndPoints.PROTOBUF_LIVE_HOST if HOST_TYPE == "live" else EndPoints.PROTOBUF_DEMO_HOST
print(f"Connecting to: {host}:{EndPoints.PROTOBUF_PORT}")
print("="*60)

client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
client.setConnectedCallback(on_connected)
client.setDisconnectedCallback(on_disconnected)
client.setMessageReceivedCallback(on_message_received)

# Set timeout
def timeout():
    print("\n❌ Timeout")
    reactor.stop()

reactor.callLater(10, timeout)

client.startService()
reactor.run(installSignalHandlers=False)
