#!/usr/bin/env python3
"""
Get the list of accounts tied to the access token
"""

import json
import os
import sys
from twisted.internet import reactor, ssl
from twisted.internet.protocol import ReconnectingClientFactory
from twisted.protocols.basic import Int32StringReceiver
from OpenApiMessagesFactory import *
from OpenApiModelMessages_pb2 import *

# Read credentials
with open('ctrader_tokens.json', 'r') as f:
    token_data = json.load(f)

CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
ACCESS_TOKEN = token_data['accessToken']

print("=" * 60)
print("Getting Account List from cTrader API")
print("=" * 60)
print(f"\nAccess Token: {ACCESS_TOKEN[:30]}...")

class CTraderClient(Int32StringReceiver):
    def connectionMade(self):
        print("\n[OK] Connected to cTrader API")
        
        # Send application auth
        app_auth = ProtoOAApplicationAuthReq()
        app_auth.clientId = CLIENT_ID
        app_auth.clientSecret = CLIENT_SECRET
        
        print("[DEBUG] Sending application auth...")
        self.send_message(app_auth)
    
    def send_message(self, message):
        message_type = message.DESCRIPTOR.name
        payload_type = ProtoOAPayloadType.Value('PROTO_OA_' + message_type[7:])
        
        proto_message = ProtoMessage()
        proto_message.payloadType = payload_type
        proto_message.payload = message.SerializeToString()
        
        serialized = proto_message.SerializeToString()
        self.sendString(serialized)
    
    def stringReceived(self, data):
        proto_message = ProtoMessage()
        proto_message.ParseFromString(data)
        
        payload_type_name = ProtoOAPayloadType.Name(proto_message.payloadType)
        
        if payload_type_name == 'PROTO_OA_APPLICATION_AUTH_RES':
            print("[OK] Application authenticated")
            
            # Request account list
            account_list_req = ProtoOAGetAccountListByAccessTokenReq()
            account_list_req.accessToken = ACCESS_TOKEN
            
            print("[DEBUG] Requesting account list...")
            self.send_message(account_list_req)
        
        elif payload_type_name == 'PROTO_OA_GET_ACCOUNT_LIST_BY_ACCESS_TOKEN_RES':
            response = ProtoOAGetAccountListByAccessTokenRes()
            response.ParseFromString(proto_message.payload)
            
            print("\n" + "=" * 60)
            print("ACCOUNTS FOUND:")
            print("=" * 60)
            
            if response.ctidTraderAccount:
                for account in response.ctidTraderAccount:
                    account_id = account.ctidTraderAccountId
                    is_live = account.isLive
                    account_type = "LIVE" if is_live else "DEMO"
                    
                    print(f"\n  Account ID: {account_id}")
                    print(f"  Type: {account_type}")
                    print(f"  Broker: {account.brokerName if hasattr(account, 'brokerName') else 'N/A'}")
                    
                    if not is_live:
                        print(f"\n  ✅ USE THIS ACCOUNT ID: {account_id}")
            else:
                print("\n  No accounts found!")
            
            print("\n" + "=" * 60)
            reactor.stop()
        
        elif payload_type_name == 'PROTO_OA_ERROR_RES':
            error = ProtoOAErrorRes()
            error.ParseFromString(proto_message.payload)
            print(f"\n[ERROR] {error.errorCode}: {error.description}")
            reactor.stop()

class CTraderFactory(ReconnectingClientFactory):
    protocol = CTraderClient
    
    def clientConnectionFailed(self, connector, reason):
        print(f"[ERROR] Connection failed: {reason}")
        reactor.stop()
    
    def clientConnectionLost(self, connector, reason):
        print(f"[DEBUG] Connection lost: {reason}")

# Connect to live endpoint
factory = CTraderFactory()
reactor.connectSSL('live.ctraderapi.com', 5035, factory, ssl.ClientContextFactory())

print("\n[DEBUG] Connecting to cTrader API...")
reactor.run()
