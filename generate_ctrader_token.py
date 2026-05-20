#!/usr/bin/env python3
"""
Pepperstone cTrader Access Token Generator
Automates the OAuth 2.0 flow to generate production access tokens
"""

import os
import sys
import webbrowser
import urllib.parse
from http.server import HTTPServer, BaseHTTPRequestHandler
import requests
import json
import threading

# Configuration
CLIENT_ID = os.environ.get('CTRADER_CLIENT_ID', '')
CLIENT_SECRET = os.environ.get('CTRADER_CLIENT_SECRET', '')
REDIRECT_URI = 'http://localhost:8888/callback'
SCOPE = 'trading'  # 'accounts' for read-only, 'trading' for full access
AUTH_URL = 'https://id.ctrader.com/my/settings/openapi/grantingaccess/'
TOKEN_URL = 'https://openapi.ctrader.com/apps/token'

# Global variable to store the authorization code
auth_code = None
server_running = True


class CallbackHandler(BaseHTTPRequestHandler):
    """HTTP handler to receive the OAuth callback"""
    
    def do_GET(self):
        global auth_code, server_running
        
        # Parse the query parameters
        query = urllib.parse.urlparse(self.path).query
        params = urllib.parse.parse_qs(query)
        
        if 'code' in params:
            auth_code = params['code'][0]
            
            # Send success response
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            
            html = """
            <html>
            <head><title>Authorization Success</title></head>
            <body style="font-family: Arial; text-align: center; padding: 50px;">
                <h1 style="color: green;">✓ Authorization Successful!</h1>
                <p>Authorization code received. Exchanging for access token...</p>
                <p>You can close this window and return to the terminal.</p>
            </body>
            </html>
            """
            self.wfile.write(html.encode())
            
            # Stop the server
            server_running = False
        else:
            # Send error response
            self.send_response(400)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            
            html = """
            <html>
            <head><title>Authorization Failed</title></head>
            <body style="font-family: Arial; text-align: center; padding: 50px;">
                <h1 style="color: red;">✗ Authorization Failed</h1>
                <p>No authorization code received.</p>
                <p>Please try again.</p>
            </body>
            </html>
            """
            self.wfile.write(html.encode())
    
    def log_message(self, format, *args):
        # Suppress default logging
        pass


def start_callback_server():
    """Start HTTP server to receive OAuth callback"""
    server = HTTPServer(('localhost', 8888), CallbackHandler)
    print(f"[INFO] Callback server started on {REDIRECT_URI}")
    
    while server_running:
        server.handle_request()
    
    server.server_close()
    print("[INFO] Callback server stopped")


def get_authorization_code():
    """Open browser for user authorization and wait for callback"""
    global auth_code
    
    # Build authorization URL
    params = {
        'client_id': CLIENT_ID,
        'redirect_uri': REDIRECT_URI,
        'scope': SCOPE,
        'product': 'web'
    }
    auth_url = f"{AUTH_URL}?{urllib.parse.urlencode(params)}"
    
    print("\n" + "="*60)
    print("STEP 1: User Authorization")
    print("="*60)
    print(f"\nOpening browser for authorization...")
    print(f"URL: {auth_url}\n")
    print("Please:")
    print("1. Login with your Pepperstone credentials")
    print("2. Select your demo account (5287516)")
    print("3. Click 'Allow access'")
    print("\nWaiting for authorization...\n")
    
    # Start callback server in background
    server_thread = threading.Thread(target=start_callback_server, daemon=True)
    server_thread.start()
    
    # Open browser
    webbrowser.open(auth_url)
    
    # Wait for authorization code
    server_thread.join(timeout=300)  # 5 minute timeout
    
    if auth_code:
        print(f"[OK] Authorization code received: {auth_code[:20]}...")
        return auth_code
    else:
        print("[ERROR] Failed to receive authorization code")
        return None


def exchange_code_for_token(code):
    """Exchange authorization code for access token"""
    print("\n" + "="*60)
    print("STEP 2: Exchange Code for Access Token")
    print("="*60)
    
    params = {
        'grant_type': 'authorization_code',
        'code': code,
        'redirect_uri': REDIRECT_URI,
        'client_id': CLIENT_ID,
        'client_secret': CLIENT_SECRET
    }
    
    print(f"\nSending request to: {TOKEN_URL}")
    
    try:
        response = requests.get(TOKEN_URL, params=params, headers={
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        })
        
        if response.status_code == 200:
            data = response.json()
            
            if data.get('errorCode'):
                print(f"[ERROR] {data.get('errorCode')}: {data.get('description')}")
                return None
            
            print("[OK] Access token received successfully!\n")
            return data
        else:
            print(f"[ERROR] HTTP {response.status_code}: {response.text}")
            return None
            
    except Exception as e:
        print(f"[ERROR] Request failed: {e}")
        return None


def save_tokens(token_data):
    """Save tokens to environment variables and file"""
    print("\n" + "="*60)
    print("STEP 3: Save Tokens")
    print("="*60)
    
    access_token = token_data.get('accessToken')
    refresh_token = token_data.get('refreshToken')
    expires_in = token_data.get('expiresIn')
    
    print(f"\nAccess Token:  {access_token[:30]}...")
    print(f"Refresh Token: {refresh_token[:30]}...")
    print(f"Expires In:    {expires_in} seconds (~{expires_in // 86400} days)")
    
    # Save to file
    token_file = 'ctrader_tokens.json'
    with open(token_file, 'w') as f:
        json.dump(token_data, f, indent=2)
    print(f"\n[OK] Tokens saved to: {token_file}")
    
    # Generate PowerShell commands
    print("\n" + "="*60)
    print("STEP 4: Set Environment Variables")
    print("="*60)
    print("\nRun these commands in PowerShell:\n")
    
    print(f"[System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', '{access_token}', 'User')")
    print(f"[System.Environment]::SetEnvironmentVariable('CTRADER_REFRESH_TOKEN', '{refresh_token}', 'User')")
    
    print("\nOr run this script to set them automatically:")
    print("python set_ctrader_env.py")
    
    # Create helper script
    with open('set_ctrader_env.py', 'w') as f:
        f.write(f"""import os
import subprocess

# Set environment variables
access_token = '{access_token}'
refresh_token = '{refresh_token}'

# Windows
subprocess.run(['setx', 'CTRADER_ACCESS_TOKEN', access_token], shell=True)
subprocess.run(['setx', 'CTRADER_REFRESH_TOKEN', refresh_token], shell=True)

print("[OK] Environment variables set!")
print("Please close and reopen PowerShell for changes to take effect.")
""")
    
    print(f"\n[OK] Helper script created: set_ctrader_env.py")


def main():
    print("="*60)
    print("Pepperstone cTrader Access Token Generator")
    print("="*60)
    
    # Check credentials
    if not CLIENT_ID or not CLIENT_SECRET:
        print("\n[ERROR] Missing credentials!")
        print("\nPlease set these environment variables first:")
        print("  CTRADER_CLIENT_ID")
        print("  CTRADER_CLIENT_SECRET")
        print("\nRun:")
        print("  [System.Environment]::SetEnvironmentVariable('CTRADER_CLIENT_ID', 'your_id', 'User')")
        print("  [System.Environment]::SetEnvironmentVariable('CTRADER_CLIENT_SECRET', 'your_secret', 'User')")
        sys.exit(1)
    
    print(f"\n[OK] Client ID: {CLIENT_ID[:20]}...")
    print(f"[OK] Client Secret: {CLIENT_SECRET[:20]}...")
    print(f"[OK] Redirect URI: {REDIRECT_URI}")
    print(f"[OK] Scope: {SCOPE}")
    
    # Step 1: Get authorization code
    code = get_authorization_code()
    if not code:
        print("\n[ERROR] Failed to get authorization code")
        sys.exit(1)
    
    # Step 2: Exchange for access token
    token_data = exchange_code_for_token(code)
    if not token_data:
        print("\n[ERROR] Failed to get access token")
        sys.exit(1)
    
    # Step 3: Save tokens
    save_tokens(token_data)
    
    print("\n" + "="*60)
    print("SUCCESS! Token generation complete")
    print("="*60)
    print("\nNext steps:")
    print("1. Run: python set_ctrader_env.py")
    print("2. Close and reopen PowerShell")
    print("3. Run: .\\start_ctrader_bridge.ps1")
    print("="*60)


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n[INFO] Cancelled by user")
        sys.exit(0)
    except Exception as e:
        print(f"\n[ERROR] Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
