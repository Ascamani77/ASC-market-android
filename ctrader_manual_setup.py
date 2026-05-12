"""
Manual cTrader setup - paste your credentials directly
"""
import json
import urllib.parse
import urllib.request
import sys

print("="*60)
print("cTrader Manual Token Setup")
print("="*60)
print()
print("Go to: https://openapi.ctrader.com/")
print("Click on your 'Asc market' application")
print("Copy the Client ID and Secret EXACTLY as shown")
print()

# Get credentials from user
client_id = input("Paste your Client ID: ").strip()
client_secret = input("Paste your Client Secret: ").strip()

if not client_id or not client_secret:
    print("\nERROR: Both Client ID and Secret are required")
    sys.exit(1)

print(f"\nClient ID: {client_id}")
print(f"Client Secret: {client_secret[:10]}...")
print()

# Build authorization URL
redirect_uri = "http://localhost:8088/callback"
auth_params = {
    "client_id": client_id,
    "redirect_uri": redirect_uri,
    "scope": "trading"
}
auth_url = f"https://openapi.ctrader.com/apps/auth?{urllib.parse.urlencode(auth_params)}"

print("Step 1: Open this URL in your browser:")
print(auth_url)
print()
print("Step 2: After authorizing, you'll be redirected to a URL like:")
print("http://localhost:8088/callback?code=XXXXX")
print()

# Get authorization code from user
redirect_url = input("Paste the FULL redirect URL here: ").strip()

# Extract code from URL
if "code=" in redirect_url:
    parsed = urllib.parse.urlparse(redirect_url)
    params = urllib.parse.parse_qs(parsed.query)
    code = params.get("code", [""])[0]
else:
    code = redirect_url

if not code:
    print("\nERROR: No authorization code found")
    sys.exit(1)

print(f"\nAuthorization code: {code[:20]}...")

# Exchange code for token
print("\nExchanging code for access token...")

token_data = {
    "grant_type": "authorization_code",
    "code": code,
    "redirect_uri": redirect_uri,
    "client_id": client_id,
    "client_secret": client_secret
}

try:
    token_request = urllib.request.Request(
        "https://openapi.ctrader.com/apps/token",
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
    print("SUCCESS! Copy these to local.properties:")
    print("="*60)
    print(f"\nCTRADER_CLIENT_ID={client_id}")
    print(f"CTRADER_CLIENT_SECRET={client_secret}")
    print(f"CTRADER_ACCESS_TOKEN={access_token}")
    if refresh_token:
        print(f"CTRADER_REFRESH_TOKEN={refresh_token}")
    print(f"\nExpires in: {expires_in} seconds ({expires_in//3600} hours)")
    print("\n" + "="*60)
    
    # Save to file
    with open("ctrader_credentials.txt", "w") as f:
        f.write(f"CTRADER_CLIENT_ID={client_id}\n")
        f.write(f"CTRADER_CLIENT_SECRET={client_secret}\n")
        f.write(f"CTRADER_ACCESS_TOKEN={access_token}\n")
        if refresh_token:
            f.write(f"CTRADER_REFRESH_TOKEN={refresh_token}\n")
        f.write(f"EXPIRES_IN={expires_in}\n")
    
    print("\nCredentials saved to: ctrader_credentials.txt")
    print("\nNOTE: You still need CTRADER_ACCOUNT_ID from your cTrader account")
    
except urllib.error.HTTPError as e:
    error_body = e.read().decode()
    print(f"\nERROR: Token exchange failed (HTTP {e.code}):")
    try:
        error_json = json.loads(error_body)
        print(json.dumps(error_json, indent=2))
    except:
        print(error_body)
    
    print("\nPossible issues:")
    print("1. Client ID or Secret is incorrect")
    print("2. Authorization code expired (try again quickly)")
    print("3. Redirect URI mismatch in cTrader app settings")
    sys.exit(1)
except Exception as e:
    print(f"\nERROR: {type(e).__name__}: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
