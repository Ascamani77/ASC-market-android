import json
import os
import queue
import sys
import threading
import time
import urllib.parse
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from typing import Dict, Optional, Tuple

from ctrader_open_api import Auth, Client, EndPoints, Protobuf, TcpProtocol
from ctrader_open_api.messages.OpenApiCommonMessages_pb2 import ProtoHeartbeatEvent
from ctrader_open_api.messages.OpenApiMessages_pb2 import *
from twisted.internet import reactor


ROOT = Path(__file__).resolve().parent
LOCAL_PROPERTIES = ROOT / "local.properties"


def read_local_properties() -> Dict[str, str]:
    values: Dict[str, str] = {}
    if not LOCAL_PROPERTIES.exists():
        return values
    for line in LOCAL_PROPERTIES.read_text(encoding="utf-8", errors="ignore").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def config_value(name: str, props: Dict[str, str], default: str = "") -> str:
    return os.getenv(name, props.get(name, default)).strip()


def looks_like_placeholder(value: str) -> bool:
    cleaned = value.strip().lower()
    return cleaned in {
        "",
        "your_client_id",
        "your_real_client_id",
        "[your_client_id]",
        "your_client_secret",
        "your_real_client_secret",
        "[your_client_secret]",
    }


def build_auth_uri(client_id: str, redirect_uri: str, scope: str, base_uri: str) -> str:
    return f"{base_uri}?{urllib.parse.urlencode({'client_id': client_id, 'redirect_uri': redirect_uri, 'scope': scope})}"


def callback_server(redirect_uri: str, result_queue: "queue.Queue[Tuple[Optional[str], Optional[str]]]") -> Optional[HTTPServer]:
    parsed = urllib.parse.urlparse(redirect_uri)
    host = parsed.hostname or "localhost"
    port = parsed.port or (443 if parsed.scheme == "https" else 80)
    path = parsed.path or "/"
    if host not in {"localhost", "127.0.0.1"} or parsed.scheme != "http":
        return None

    class CallbackHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            request = urllib.parse.urlparse(self.path)
            params = urllib.parse.parse_qs(request.query)
            if request.path != path:
                self.send_response(404)
                self.end_headers()
                return
            code = params.get("code", [None])[0]
            error = params.get("error", [None])[0]
            result_queue.put((code, error))
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(b"<html><body><h2>cTrader authorization received.</h2><p>You can return to PowerShell.</p></body></html>")

        def log_message(self, *_args):
            return

    return HTTPServer((host, port), CallbackHandler)


def receive_auth_code(auth_uri: str, redirect_uri: str) -> str:
    result_queue: "queue.Queue[Tuple[Optional[str], Optional[str]]]" = queue.Queue(maxsize=1)
    server = callback_server(redirect_uri, result_queue)

    if server is not None:
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        print(f"Listening for cTrader redirect on {redirect_uri}")
        print("Opening browser for cTrader authorization...")
        webbrowser.open(auth_uri)
        try:
            code, error = result_queue.get(timeout=180)
        except queue.Empty:
            server.shutdown()
            raise TimeoutError("Timed out waiting for cTrader redirect. Check that CTRADER_REDIRECT_URI exactly matches your cTrader app settings.")
        server.shutdown()
        if error:
            raise RuntimeError(f"cTrader authorization failed: {error}")
        if not code:
            raise RuntimeError("cTrader redirect did not include a code")
        return code

    print("Open this URL in your browser:")
    print(auth_uri)
    print("After approving, paste the redirected URL or only the code value here.")
    raw = input("Redirect URL or code: ").strip()
    if "code=" in raw:
        parsed = urllib.parse.urlparse(raw)
        params = urllib.parse.parse_qs(parsed.query)
        code = params.get("code", [""])[0]
    else:
        code = raw
    if not code:
        raise RuntimeError("No authorization code was provided")
    return code


def print_token_result(token: Dict[str, object]) -> str:
    access_token = str(token.get("accessToken") or token.get("access_token") or "")
    refresh_token = str(token.get("refreshToken") or token.get("refresh_token") or "")
    expires_in = token.get("expiresIn") or token.get("expires_in") or "unknown"
    if not access_token:
        raise RuntimeError(f"Token response did not include accessToken: {json.dumps(token, indent=2)}")

    print("\nACCESS TOKEN")
    print(access_token)
    if refresh_token:
        print("\nREFRESH TOKEN")
        print(refresh_token)
    print(f"\nEXPIRES IN: {expires_in}")
    return access_token


def request_accounts(client: Client, client_id: str, client_secret: str, access_token: str) -> None:
    request = ProtoOAApplicationAuthReq()
    request.clientId = client_id
    request.clientSecret = client_secret
    deferred = client.send(request)
    deferred.addErrback(lambda failure: fail_and_stop(failure))


def fail_and_stop(error) -> None:
    print(f"\nERROR: {error}")
    if reactor.running:
        reactor.stop()


def print_accounts(payload) -> None:
    accounts = list(getattr(payload, "ctidTraderAccount", []))
    print("\nAUTHORIZED ACCOUNTS")
    if not accounts:
        print("No accounts were returned. Re-run authorization and make sure you selected your Pepperstone demo account.")
    for index, account in enumerate(accounts, start=1):
        account_id = getattr(account, "ctidTraderAccountId", "")
        is_live = getattr(account, "isLive", "")
        broker = getattr(account, "brokerName", "")
        login = getattr(account, "traderLogin", "")
        print(f"{index}. CTRADER_ACCOUNT_ID={account_id} broker={broker} live={is_live} login={login}")


def fetch_account_ids(host_type: str, client_id: str, client_secret: str, access_token: str) -> None:
    host = EndPoints.PROTOBUF_LIVE_HOST if host_type.lower() == "live" else EndPoints.PROTOBUF_DEMO_HOST
    client = Client(host, EndPoints.PROTOBUF_PORT, TcpProtocol)

    def connected(_client):
        request_accounts(_client, client_id, client_secret, access_token)

    def disconnected(_client, reason):
        print(f"Disconnected: {reason}")

    def on_message(_client, message):
        if message.payloadType == ProtoHeartbeatEvent().payloadType:
            return
        payload = Protobuf.extract(message)
        if message.payloadType == ProtoOAApplicationAuthRes().payloadType:
            request = ProtoOAGetAccountListByAccessTokenReq()
            request.accessToken = access_token
            deferred = _client.send(request)
            deferred.addErrback(lambda failure: fail_and_stop(failure))
        elif message.payloadType == ProtoOAGetAccountListByAccessTokenRes().payloadType:
            print_accounts(payload)
            reactor.stop()
        elif message.payloadType == ProtoOAErrorRes().payloadType:
            fail_and_stop(payload)

    client.setConnectedCallback(connected)
    client.setDisconnectedCallback(disconnected)
    client.setMessageReceivedCallback(on_message)
    client.startService()
    reactor.run()


def main() -> None:
    props = read_local_properties()
    client_id = config_value("CTRADER_CLIENT_ID", props)
    client_secret = config_value("CTRADER_CLIENT_SECRET", props)
    redirect_uri = config_value("CTRADER_REDIRECT_URI", props, "http://localhost:8088/callback")
    host_type = config_value("CTRADER_HOST_TYPE", props, "demo")
    auth_scope = config_value("CTRADER_AUTH_SCOPE", props, "trading")
    auth_base_uri = config_value("CTRADER_AUTH_BASE_URI", props, EndPoints.AUTH_URI)

    if looks_like_placeholder(client_id) or looks_like_placeholder(client_secret):
        print("Set your real CTRADER_CLIENT_ID and CTRADER_CLIENT_SECRET before running this script.")
        print("PowerShell example:")
        print('$env:CTRADER_CLIENT_ID="paste-your-real-client-id-here"')
        print('$env:CTRADER_CLIENT_SECRET="paste-your-real-client-secret-here"')
        print('$env:CTRADER_REDIRECT_URI="http://localhost:8088/callback"')
        print("Then run: python .\\ctrader_oauth_setup.py")
        sys.exit(1)

    print(f"Host type: {host_type}")
    print(f"Redirect URI: {redirect_uri}")
    print(f"Auth scope: {auth_scope}")
    print(f"Auth endpoint: {auth_base_uri}")
    print("Make sure this redirect URI exactly matches your cTrader Open API app settings.")

    auth = Auth(client_id, client_secret, redirect_uri)
    auth_uri = build_auth_uri(client_id, redirect_uri, auth_scope, auth_base_uri)
    print("\nAuthorization URL:")
    print(auth_uri)
    code = receive_auth_code(auth_uri, redirect_uri)
    print("Authorization code received. Exchanging immediately for token...")
    token = auth.getToken(code)
    access_token = print_token_result(token)

    print("\nFetching authorized cTrader account IDs...")
    time.sleep(1)
    fetch_account_ids(host_type, client_id, client_secret, access_token)

    print("\nUse these PowerShell values for the bridge:")
    print(f'$env:CTRADER_HOST_TYPE="{host_type}"')
    print('$env:CTRADER_CLIENT_ID="YOUR_CLIENT_ID"')
    print('$env:CTRADER_CLIENT_SECRET="YOUR_CLIENT_SECRET"')
    print('$env:CTRADER_ACCESS_TOKEN="THE_ACCESS_TOKEN_PRINTED_ABOVE"')
    print('$env:CTRADER_ACCOUNT_ID="ONE_ACCOUNT_ID_PRINTED_ABOVE"')
    print("python ctrader_bridge.py")


if __name__ == "__main__":
    main()
