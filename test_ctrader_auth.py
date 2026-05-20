import os
from ctrader_open_api import Client, Protobuf, TcpProtocol, EndPoints
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor

# Read credentials
CLIENT_ID = os.getenv("CTRADER_CLIENT_ID", "").strip()
CLIENT_SECRET = os.getenv("CTRADER_CLIENT_SECRET", "").strip()
HOST_TYPE = os.getenv("CTRADER_HOST_TYPE", "demo").strip().lower()

print(f"CLIENT_ID: {CLIENT_ID}")
print(f"CLIENT_ID length: {len(CLIENT_ID)}")
print(f"CLIENT_SECRET: {CLIENT_SECRET[:10]}...")
print(f"CLIENT_SECRET length: {len(CLIENT_SECRET)}")
print(f"HOST_TYPE: {HOST_TYPE}")

if not CLIENT_ID or not CLIENT_SECRET:
    print("ERROR: Missing credentials")
    exit(1)

host = EndPoints.PROTOBUF_LIVE_HOST if HOST_TYPE == "live" else EndPoints.PROTOBUF_DEMO_HOST
print(f"Connecting to: {host}:{EndPoints.PROTOBUF_PORT}")

client = None

def connected(_client):
    global client
    client = _client
    print("\n✓ Connected to cTrader")
    print("Sending ProtoOAApplicationAuthReq...")
    request = ProtoOAApplicationAuthReq()
    request.clientId = CLIENT_ID
    request.clientSecret = CLIENT_SECRET
    print(f"  clientId: {request.clientId}")
    print(f"  clientSecret: {request.clientSecret[:10]}...")
    deferred = client.send(request)
    deferred.addErrback(on_error)

def disconnected(_client, reason):
    print(f"\n✗ Disconnected: {reason}")
    reactor.stop()

def on_error(failure):
    print(f"\n✗ Error: {failure}")
    reactor.stop()

    
    if isinstance(payload, ProtoOAApplicationAuthRes):
    print(f"\n✓ Message received: {type(payload).__name__}")
        reactor.stop()
    elif isinstance(payload, ProtoOAErrorRes):
        print("✓✓✓ APPLICATION AUTHENTICATED SUCCESSFULLY ✓✓✓")
reactor.run()
