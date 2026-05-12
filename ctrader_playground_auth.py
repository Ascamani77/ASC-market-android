"""
cTrader authentication using the playground redirect
"""
import json
import urllib.parse
import urllib.request

CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nlO4hK6YaP0aBiY31q86s"
CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPlBzUYBmC67cxHAKbjty"
REDIRECT_URI = "https://openapi.ctrader.com/apps/27391/playground"

print("="*60)
print("cTrader Playground Authentication")
print("="*60)
print()

# Build authorization URL
auth_params = {
    "client_id": CLIENT_ID,
    "redirect_uri": REDIRECT_URI,
    "scope": "trading"
}
auth_url = f"https://openapi.ctrader.com/apps/auth?{urllib.parse.urlencode(auth_params)}"

print("Step 1: Open this URL in your browser:")
print(auth_url)
print()
print("Step 2: After authorizing, you'll be redirected to the playground.")
print("The URL will contain '?code=XXXXX'")
print()
print("Step 3: Copy the FULL URL from your browser's address bar")
print("(It will be a long URL starting with https://openapi.ctrader.com/apps/27391/playground?code=...)")
print()

# Get the redirect URL
redirect_url = input("Paste the full playground URL here: ").strip()

# Extract code
if "code=" in redirect_url:
    parsed = urllib.parse.urlparse(redirect_url)
    params = urllib.parse.parse_qs(parsed.query)
    code = params.get("code", [""])[0]
else:
    print("\nERROR: No 'code' parameter found in URL")
    print("Make sure you copied the full URL after authorization")
    exit(1)

if not code:
    print("\nERROR: Authorization code is empty")
    exit(1)

print(f"\nAuthorization code: {code[:20]}...")
print("\nExchanging code for access token...")

# Exchange code for token
token_data = {
    "grant_type": "authorization_code",
    "code": code,
    "redirect_uri": REDIRECT_URI,
    "client_id": CLIENT_ID,
    "client_secret": CLIENT_SECRET
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
        exit(1)
    
    print("\n" + "="*60)
    print("SUCCESS! Here are your credentials:")
    print("="*60)
    print(f"\nCTRADER_ACCESS_TOKEN={access_token}")
    if refresh_token:
        print(f"\nCTRADER_REFRESH_TOKEN={refresh_token}")
    print(f"\nExpires in: {expires_in} seconds ({expires_in//3600} hours)")
    print("\n" + "="*60)
    
    # Save to file
    with open("ctrader_token.txt", "w") as f:
        f.write(f"CTRADER_ACCESS_TOKEN={access_token}\n")
        if refresh_token:
            f.write(f"CTRADER_REFRESH_TOKEN={refresh_token}\n")
        f.write(f"EXPIRES_IN={expires_in}\n")
    
    print("\nToken saved to: ctrader_token.txt")
    print("\nAdd this to your local.properties file!")
    
except urllib.error.HTTPError as e:
    error_body = e.read().decode()
    print(f"\nERROR: Token exchange failed (HTTP {e.code}):")
    try:
        error_json = json.loads(error_body)
        print(json.dumps(error_json, indent=2))
    except:
        print(error_body)
    exit(1)
except Exception as e:
    print(f"\nERROR: {type(e).__name__}: {e}")
    import traceback
    traceback.print_exc()
    exit(1)
