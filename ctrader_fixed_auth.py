"""
Fixed cTrader OAuth with proper authentication
"""
import json
import urllib.parse
import urllib.request
import base64

CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nlO4hK6YaP0aBiY31q86s"
CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPlBzUYBmC67cxHAKbjty"
REDIRECT_URI = "https://openapi.ctrader.com/apps/27391/playground"

print("="*60)
print("cTrader Authentication (Fixed)")
print("="*60)
print()

# Build authorization URL
auth_params = {
    "client_id": CLIENT_ID,
    "redirect_uri": REDIRECT_URI,
    "scope": "trading",
    "response_type": "code"
}
auth_url = f"https://openapi.ctrader.com/apps/auth?{urllib.parse.urlencode(auth_params)}"

print("Step 1: Open this URL:")
print(auth_url)
print()
print("Step 2: After authorizing, copy the full URL from your browser")
print()

redirect_url = input("Paste the redirect URL: ").strip()

# Extract code
if "code=" in redirect_url:
    parsed = urllib.parse.urlparse(redirect_url)
    params = urllib.parse.parse_qs(parsed.query)
    code = params.get("code", [""])[0]
else:
    code = redirect_url

if not code:
    print("\nERROR: No authorization code")
    exit(1)

print(f"\nCode: {code[:20]}...")
print("\nExchanging for token...")

# Method 1: Try with Basic Auth (recommended by OAuth2 spec)
try:
    print("\nTrying Basic Authentication...")
    
    # Create Basic Auth header
    credentials = f"{CLIENT_ID}:{CLIENT_SECRET}"
    encoded_credentials = base64.b64encode(credentials.encode()).decode()
    
    token_data = urllib.parse.urlencode({
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": REDIRECT_URI
    })
    
    token_request = urllib.request.Request(
        "https://openapi.ctrader.com/apps/token",
        data=token_data.encode(),
        headers={
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": f"Basic {encoded_credentials}"
        }
    )
    
    with urllib.request.urlopen(token_request, timeout=30) as response:
        token_response = json.loads(response.read().decode())
    
    access_token = token_response.get("access_token") or token_response.get("accessToken")
    
    if access_token:
        print("\n" + "="*60)
        print("SUCCESS!")
        print("="*60)
        print(f"\nCTRADER_ACCESS_TOKEN={access_token}")
        
        refresh_token = token_response.get("refresh_token") or token_response.get("refreshToken")
        if refresh_token:
            print(f"CTRADER_REFRESH_TOKEN={refresh_token}")
        
        expires_in = token_response.get("expires_in") or token_response.get("expiresIn")
        print(f"\nExpires in: {expires_in} seconds")
        
        with open("ctrader_token.txt", "w") as f:
            f.write(f"CTRADER_ACCESS_TOKEN={access_token}\n")
            if refresh_token:
                f.write(f"CTRADER_REFRESH_TOKEN={refresh_token}\n")
        
        print("\nSaved to: ctrader_token.txt")
        exit(0)
    
except urllib.error.HTTPError as e:
    print(f"Basic Auth failed: {e.code}")
    error_body = e.read().decode()
    print(error_body)

# Method 2: Try with credentials in body (fallback)
try:
    print("\nTrying credentials in request body...")
    
    token_data = urllib.parse.urlencode({
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": REDIRECT_URI,
        "client_id": CLIENT_ID,
        "client_secret": CLIENT_SECRET
    })
    
    token_request = urllib.request.Request(
        "https://openapi.ctrader.com/apps/token",
        data=token_data.encode(),
        headers={"Content-Type": "application/x-www-form-urlencoded"}
    )
    
    with urllib.request.urlopen(token_request, timeout=30) as response:
        token_response = json.loads(response.read().decode())
    
    access_token = token_response.get("access_token") or token_response.get("accessToken")
    
    if access_token:
        print("\n" + "="*60)
        print("SUCCESS!")
        print("="*60)
        print(f"\nCTRADER_ACCESS_TOKEN={access_token}")
        
        refresh_token = token_response.get("refresh_token") or token_response.get("refreshToken")
        if refresh_token:
            print(f"CTRADER_REFRESH_TOKEN={refresh_token}")
        
        expires_in = token_response.get("expires_in") or token_response.get("expiresIn")
        print(f"\nExpires in: {expires_in} seconds")
        
        with open("ctrader_token.txt", "w") as f:
            f.write(f"CTRADER_ACCESS_TOKEN={access_token}\n")
            if refresh_token:
                f.write(f"CTRADER_REFRESH_TOKEN={refresh_token}\n")
        
        print("\nSaved to: ctrader_token.txt")
        exit(0)
    else:
        print("\nERROR: No access token in response")
        print(json.dumps(token_response, indent=2))
    
except urllib.error.HTTPError as e:
    print(f"\nBoth methods failed: {e.code}")
    error_body = e.read().decode()
    print(error_body)
except Exception as e:
    print(f"\nERROR: {e}")
    import traceback
    traceback.print_exc()
