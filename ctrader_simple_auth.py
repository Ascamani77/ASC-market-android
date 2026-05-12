"""
Simple cTrader OAuth script with better error handling
"""
import json
import os
import sys
import urllib.parse
import urllib.request
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer
import threading
import queue

# Configuration
CLIENT_ID = os.getenv("CTRADER_CLIENT_ID", "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nlO4hK6YaP0aBiY31q86s")
CLIENT_SECRET = os.getenv("CTRADER_CLIENT_SECRET", "loPssicrvxYshWozrxgGFL40yAxrZPPlBzUYBmC67cxHAKbjty")
REDIRECT_URI = "http://localhost:8088/callback"
AUTH_URL = "https://openapi.ctrader.com/apps/auth"
TOKEN_URL = "https://openapi.ctrader.com/apps/token"

print(f"Client ID: {CLIENT_ID}")
print(f"Redirect URI: {REDIRECT_URI}")
print()

# Step 1: Build authorization URL
auth_params = {
    "client_id": CLIENT_ID,
    "redirect_uri": REDIRECT_URI,
    "scope": "trading"
}
authorization_url = f"{AUTH_URL}?{urllib.parse.urlencode(auth_params)}"

print("Authorization URL:")
print(authorization_url)
print()

# Step 2: Start local server to receive callback
result_queue = queue.Queue(maxsize=1)

class CallbackHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        params = urllib.parse.parse_qs(parsed.query)
        
        if parsed.path == "/callback":
            code = params.get("code", [None])[0]
            error = params.get("error", [None])[0]
            
            result_queue.put((code, error))
            
            self.send_response(200)
            self.send_header("Content-Type", "text/html")
            self.end_headers()
            self.wfile.write(b"<html><body><h2>Authorization received!</h2><p>You can close this window.</p></body></html>")
        else:
            self.send_response(404)
            self.end_headers()
    
    def log_message(self, *args):
        pass

server = HTTPServer(("localhost", 8088), CallbackHandler)
server_thread = threading.Thread(target=server.serve_forever, daemon=True)
server_thread.start()

print("Listening on http://localhost:8088/callback")
print("Opening browser...")
webbrowser.open(authorization_url)

# Wait for callback
try:
    code, error = result_queue.get(timeout=180)
except queue.Empty:
    print("\nERROR: Timeout waiting for authorization")
    server.shutdown()
    sys.exit(1)

server.shutdown()

if error:
    print(f"\nERROR: Authorization failed: {error}")
    sys.exit(1)

if not code:
    print("\nERROR: No authorization code received")
    sys.exit(1)

print(f"\nAuthorization code received: {code[:20]}...")

# Step 3: Exchange code for token
print("\nExchanging code for access token...")

token_data = {
    "grant_type": "authorization_code",
    "code": code,
    "redirect_uri": REDIRECT_URI,
    "client_id": CLIENT_ID,
    "client_secret": CLIENT_SECRET
}

try:
    token_request = urllib.request.Request(
        TOKEN_URL,
        data=urllib.parse.urlencode(token_data).encode(),
        headers={"Content-Type": "application/x-www-form-urlencoded"}
    )
    
    with urllib.request.urlopen(token_request, timeout=30) as response:
        token_response = json.loads(response.read().decode())
    
    access_token = token_response.get("access_token") or token_response.get("accessToken")
    refresh_token = token_response.get("refresh_token") or token_response.get("refreshToken")
    expires_in = token_response.get("expires_in") or token_response.get("expiresIn")
    
    if not access_token:
        print(f"\nERROR: No access token in response:")
        print(json.dumps(token_response, indent=2))
        sys.exit(1)
    
    print("\n" + "="*60)
    print("SUCCESS! Copy these values to local.properties:")
    print("="*60)
    print(f"\nCTRADER_ACCESS_TOKEN={access_token}")
    if refresh_token:
        print(f"\nCTRADER_REFRESH_TOKEN={refresh_token}")
    print(f"\nExpires in: {expires_in} seconds")
    print("\n" + "="*60)
    
    # Save to file for easy copying
    with open("ctrader_tokens.txt", "w") as f:
        f.write(f"CTRADER_ACCESS_TOKEN={access_token}\n")
        if refresh_token:
            f.write(f"CTRADER_REFRESH_TOKEN={refresh_token}\n")
        f.write(f"EXPIRES_IN={expires_in}\n")
    
    print("\nTokens saved to: ctrader_tokens.txt")
    print("\nNOTE: You still need to get your CTRADER_ACCOUNT_ID")
    print("You can find it in your cTrader account settings or by logging into cTrader.")
    
except urllib.error.HTTPError as e:
    error_body = e.read().decode()
    print(f"\nERROR: Token exchange failed (HTTP {e.code}):")
    print(error_body)
    sys.exit(1)
except Exception as e:
    print(f"\nERROR: {type(e).__name__}: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
