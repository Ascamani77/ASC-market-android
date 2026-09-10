"""
Find which endpoint (demo or live) has your account
Tests both endpoints to determine where account 47340965 exists
"""
import os
import json
from ctrader_open_api import Client, EndPoints, Protobuf, TcpProtocol
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor

# Credentials
CLIENT_ID = os.environ.get("CTRADER_CLIENT_ID", "")
CLIENT_SECRET = os.environ.get("CTRADER_CLIENT_SECRET", "")
ACCESS_TOKEN = "mW5eKJ0xZygSj4BnNvqlteOc_opHJ0EM9Qtcx8RvOfo"
TARGET_ACCOUNT = 47340965

current_endpoint = None
test_results = {}

def test_endpoint(endpoint_name, host):
    global current_endpoint
    current_endpoint = endpoint_name
    
    print(f"\n{'='*60}")
    print(f"Testing {endpoint_name.upper()} endpoint")
    print(f"Host: {host}:{EndPoints.PROTOBUF_PORT}")
    print(f"{'='*60}")
    
    client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)
    
    def on_connected(c):
        print(f"✅ Connected to {endpoint_name}")
        request = ProtoOAApplicationAuthReq()
        request.clientId = CLIENT_ID
        request.clientSecret = CLIENT_SECRET
        c.send(request)
    
    def on_disconnected(c, reason):
        print(f"Disconnected from {endpoint_name}: {reason}")
    
    def on_message_received(c, message):
        global current_endpoint, test_results
        payload = Protobuf.extract(message)
        
        if message.payloadType == ProtoOAApplicationAuthRes().payloadType:
            print(f"✅ Application authenticated on {endpoint_name}")
            
            # Request account list
            request = ProtoOAGetAccountListByAccessTokenReq()
            request.accessToken = ACCESS_TOKEN
            c.send(request)
        
        elif message.payloadType == ProtoOAGetAccountListByAccessTokenRes().payloadType:
            accounts = list(getattr(payload, "ctidTraderAccount", []))
            
            print(f"\nAccounts found on {endpoint_name}: {len(accounts)}")
            
            found_target = False
            for account in accounts:
                account_id = getattr(account, "ctidTraderAccountId", None)
                is_live = getattr(account, "isLive", False)
                broker = getattr(account, "brokerName", "Unknown")
                
                print(f"  - Account {account_id} ({'LIVE' if is_live else 'DEMO'}, {broker})")
                
                if account_id == TARGET_ACCOUNT:
                    found_target = True
                    print(f"    🎯 TARGET ACCOUNT FOUND!")
            
            test_results[endpoint_name] = {
                'success': True,
                'accounts': [getattr(a, "ctidTraderAccountId", None) for a in accounts],
                'found_target': found_target
            }
            
            reactor.stop()
        
        elif message.payloadType == ProtoOAErrorRes().payloadType:
            error = ProtoOAErrorRes()
            error.ParseFromString(message.payload)
            print(f"❌ Error on {endpoint_name}: {error.errorCode} - {error.description}")
            
            test_results[endpoint_name] = {
                'success': False,
                'error': f"{error.errorCode}: {error.description}"
            }
            
            reactor.stop()
    
    def timeout():
        print(f"❌ Timeout on {endpoint_name}")
        test_results[endpoint_name] = {
            'success': False,
            'error': 'Timeout'
        }
        reactor.stop()
    
    client.setConnectedCallback(on_connected)
    client.setDisconnectedCallback(on_disconnected)
    client.setMessageReceivedCallback(on_message_received)
    
    reactor.callLater(10, timeout)
    
    client.startService()
    reactor.run(installSignalHandlers=False)

# Test both endpoints
print("="*60)
print("Finding Account Endpoint")
print("="*60)
print(f"Target Account: {TARGET_ACCOUNT}")
print(f"Access Token: {ACCESS_TOKEN[:30]}...")

# Test LIVE endpoint first
test_endpoint("live", EndPoints.PROTOBUF_LIVE_HOST)

# Test DEMO endpoint
test_endpoint("demo", EndPoints.PROTOBUF_DEMO_HOST)

# Print summary
print("\n" + "="*60)
print("SUMMARY")
print("="*60)

for endpoint, result in test_results.items():
    print(f"\n{endpoint.upper()} Endpoint:")
    if result['success']:
        print(f"  ✅ Success")
        print(f"  Accounts: {result['accounts']}")
        if result['found_target']:
            print(f"  🎯 TARGET ACCOUNT {TARGET_ACCOUNT} FOUND HERE!")
            print(f"\n  ⚠️  UPDATE YOUR CONFIG:")
            print(f"     CTRADER_HOST_TYPE=\"{endpoint}\"")
    else:
        print(f"  ❌ Failed: {result['error']}")

print("\n" + "="*60)
