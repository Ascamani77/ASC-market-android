"""
Quick test script to verify cTrader production connection
"""
import os
import sys
import time
from ctrader_open_api import Client, EndPoints, Protobuf, TcpProtocol
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor

# Load credentials from environment or use defaults
CLIENT_ID = os.getenv("CTRADER_CLIENT_ID", "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s")
CLIENT_SECRET = os.getenv("CTRADER_CLIENT_SECRET", "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty")
ACCESS_TOKEN = os.getenv("CTRADER_ACCESS_TOKEN", "bN-GI8U-FyVC-b_nS9iAPHE3F-d_1GGa4XgidKRH3aw")
ACCOUNT_ID = int(os.getenv("CTRADER_ACCOUNT_ID", "47223753"))
HOST_TYPE = os.getenv("CTRADER_HOST_TYPE", "live").lower()

test_results = {
    "connected": False,
    "app_authenticated": False,
    "account_authenticated": False,
    "error": None
}

def on_connected(client):
    print("✅ Connected to cTrader API")
    test_results["connected"] = True
    
    # Send application auth request
    request = ProtoOAApplicationAuthReq()
    request.clientId = CLIENT_ID
    request.clientSecret = CLIENT_SECRET
    client.send(request)

def on_disconnected(client, reason):
    print(f"❌ Disconnected: {reason}")
    test_results["error"] = str(reason)
    reactor.stop()

def on_message_received(client, message):
    payload = Protobuf.extract(message)
    
    if message.payloadType == ProtoOAApplicationAuthRes().payloadType:
        print("✅ Application authenticated")
        test_results["app_authenticated"] = True
        
        # Send account auth request
        request = ProtoOAAccountAuthReq()
        request.ctidTraderAccountId = ACCOUNT_ID
        request.accessToken = ACCESS_TOKEN
        client.send(request)
    
    elif message.payloadType == ProtoOAAccountAuthRes().payloadType:
        print("✅ Account authenticated")
        test_results["account_authenticated"] = True
        
        # Success! Stop the test
        print("\n" + "="*60)
        print("SUCCESS! Production connection verified")
        print("="*60)
        print(f"Host: {host}")
        print(f"Account ID: {ACCOUNT_ID}")
        print(f"Environment: {HOST_TYPE.upper()}")
        print("="*60)
        
        reactor.callLater(1, reactor.stop)
    
    elif message.payloadType == ProtoOAErrorRes().payloadType:
        print(f"❌ Error: {payload}")
        test_results["error"] = str(payload)
        reactor.stop()

def on_error(failure):
    print(f"❌ Error: {failure}")
    test_results["error"] = str(failure)
    reactor.stop()

# Main test
print("="*60)
print("Testing cTrader Production Connection")
print("="*60)
print(f"Environment: {HOST_TYPE.upper()}")
print(f"Account ID: {ACCOUNT_ID}")

host = EndPoints.PROTOBUF_LIVE_HOST if HOST_TYPE == "live" else EndPoints.PROTOBUF_DEMO_HOST
print(f"Connecting to: {host}:{EndPoints.PROTOBUF_PORT}")
print("="*60)

client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
client.setConnectedCallback(on_connected)
client.setDisconnectedCallback(on_disconnected)
client.setMessageReceivedCallback(on_message_received)

# Set timeout
def timeout():
    if not test_results["account_authenticated"]:
        print("\n❌ Test timeout - connection took too long")
        reactor.stop()

reactor.callLater(10, timeout)

# Start the client
client.startService()
reactor.run(installSignalHandlers=False)

# Print results
print("\n" + "="*60)
print("Test Results:")
print("="*60)
print(f"Connected: {test_results['connected']}")
print(f"App Authenticated: {test_results['app_authenticated']}")
print(f"Account Authenticated: {test_results['account_authenticated']}")
if test_results['error']:
    print(f"Error: {test_results['error']}")
print("="*60)

# Exit with appropriate code
sys.exit(0 if test_results["account_authenticated"] else 1)
