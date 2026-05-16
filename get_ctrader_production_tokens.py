"""
cTrader OAuth 2.0 Token Generator for Production
This script helps you obtain access and refresh tokens for your production cTrader app.
"""

import webbrowser
import urllib.parse
from http.server import HTTPServer, BaseHTTPRequestHandler
import requests
import json

# ============================================
# CONFIGURATION - Fill these from your app
# ============================================
CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"  # From "Credentials" in cTrader app
CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"  # From "Credentials" in cTrader app
REDIRECT_URI = "http://localhost:5555/callback"  # Must match your app's redirect URI

# OAuth endpoints (as per cTrader documentation)
AUTH_URL = "https://id.ctrader.com/my/settings/openapi/grantingaccess/"
TOKEN_URL = "https://openapi.ctrader.com/apps/token"

# ============================================
# OAuth Flow Implementation
# ============================================

authorization_code = None


class CallbackHandler(BaseHTTPRequestHandler):
    """Handles the OAuth callback"""
    
    def do_GET(self):
        global authorization_code
        
        # Parse the callback URL
        query = urllib.parse.urlparse(self.path).query
        params = urllib.parse.parse_qs(query)
        
        if 'code' in params:
            authorization_code = params['code'][0]
            
            # Send success response
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(b"""
                <html>
                <body style="font-family: Arial; text-align: center; padding: 50px;">
                    <h1 style="color: green;">Authorization Successful!</h1>
                    <p>You can close this window and return to the terminal.</p>
                </body>
                </html>
            """)
        else:
            # Error response
            error = params.get('error', ['Unknown error'])[0]
            self.send_response(400)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write(f"""
                <html>
                <body style="font-family: Arial; text-align: center; padding: 50px;">
                    <h1 style="color: red;">Authorization Failed</h1>
                    <p>Error: {error}</p>
                </body>
                </html>
            """.encode())
    
    def log_message(self, format, *args):
        # Suppress server logs
        pass


def get_authorization_code():
    """Step 1: Get authorization code"""
    print("\n" + "="*60)
    print("STEP 1: Authorization")
    print("="*60)
    
    # Build authorization URL (as per cTrader documentation)
    params = {
        'client_id': CLIENT_ID,
        'redirect_uri': REDIRECT_URI,
        'scope': 'trading',  # 'accounts' for read-only, 'trading' for full access
        'product': 'web'  # Makes it look better on mobile devices
    }
    
    auth_url = f"{AUTH_URL}?{urllib.parse.urlencode(params)}"
    
    print(f"\nOpening browser for authorization...")
    print(f"If browser doesn't open, visit this URL manually:")
    print(f"\n{auth_url}\n")
    
    # Open browser
    webbrowser.open(auth_url)
    
    # Start local server to receive callback
    print("Waiting for authorization callback on http://localhost:5555...")
    server = HTTPServer(('localhost', 5555), CallbackHandler)
    
    # Wait for one request (the callback)
    server.handle_request()
    server.server_close()
    
    return authorization_code


def exchange_code_for_tokens(code):
    """Step 2: Exchange authorization code for tokens"""
    print("\n" + "="*60)
    print("STEP 2: Exchanging code for tokens")
    print("="*60)
    
    # As per cTrader documentation, use query parameters
    params = {
        'grant_type': 'authorization_code',
        'code': code,
        'redirect_uri': REDIRECT_URI,
        'client_id': CLIENT_ID,
        'client_secret': CLIENT_SECRET
    }
    
    print("\nRequesting tokens from cTrader...")
    
    try:
        # Use GET request as shown in cTrader documentation
        response = requests.get(TOKEN_URL, params=params, headers={
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        })
        response.raise_for_status()
        
        tokens = response.json()
        return tokens
    
    except requests.exceptions.RequestException as e:
        print(f"\n❌ Error: {e}")
        if hasattr(e.response, 'text'):
            print(f"Response: {e.response.text}")
        return None


def main():
    print("\n" + "="*60)
    print("cTrader Production Token Generator")
    print("="*60)
    
    # Validate configuration
    if CLIENT_ID == "YOUR_PRODUCTION_CLIENT_ID" or CLIENT_SECRET == "YOUR_PRODUCTION_CLIENT_SECRET":
        print("\n❌ ERROR: Please configure CLIENT_ID and CLIENT_SECRET first!")
        print("\nSteps:")
        print("1. Go to https://openapi.ctrader.com/apps")
        print("2. Click 'Credentials' on your 'Asc market' app")
        print("3. Copy Client ID and Client Secret")
        print("4. Update this script with those values")
        print("5. Make sure Redirect URI includes: http://localhost:8080/callback")
        return
    
    # Step 1: Get authorization code
    code = get_authorization_code()
    
    if not code:
        print("\n❌ Failed to get authorization code")
        return
    
    print(f"\n✅ Authorization code received: {code[:20]}...")
    
    # Step 2: Exchange for tokens
    tokens = exchange_code_for_tokens(code)
    
    if not tokens:
        print("\n❌ Failed to get tokens")
        return
    
    # Display results
    print("\n" + "="*60)
    print("SUCCESS! Your Production Tokens:")
    print("="*60)
    
    access_token = tokens.get('accessToken', '')  # Note: cTrader uses camelCase
    refresh_token = tokens.get('refreshToken', '')
    expires_in = tokens.get('expiresIn', 0)
    
    print(f"\nAccess Token:  {access_token}")
    print(f"Refresh Token: {refresh_token}")
    print(f"Expires In:    {expires_in} seconds ({expires_in // 3600} hours)")
    
    # Save to file
    output = {
        "accessToken": access_token,  # Using cTrader's camelCase format
        "refreshToken": refresh_token,
        "expiresIn": expires_in,
        "tokenType": tokens.get('tokenType', 'Bearer')
    }
    
    with open('ctrader_production_tokens.json', 'w') as f:
        json.dump(output, f, indent=2)
    
    print("\n✅ Tokens saved to: ctrader_production_tokens.json")
    
    # Show how to use in local.properties
    print("\n" + "="*60)
    print("Add these to your local.properties:")
    print("="*60)
    print(f"\nCTRADER_HOST_TYPE=live")
    print(f"CTRADER_CLIENT_ID={CLIENT_ID}")
    print(f"CTRADER_CLIENT_SECRET={CLIENT_SECRET}")
    print(f"CTRADER_ACCESS_TOKEN={access_token}")
    print(f"CTRADER_REFRESH_TOKEN={refresh_token}")
    print(f"# CTRADER_ACCOUNT_ID will be auto-discovered")
    
    print("\n" + "="*60)
    print("IMPORTANT: Keep these tokens secure!")
    print("="*60)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n❌ Cancelled by user")
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
