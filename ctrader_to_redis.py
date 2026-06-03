"""
cTrader WebSocket to Redis Stream Adapter
Connects to cTrader bridge WebSocket and publishes ticks to Redis stream
"""
import asyncio
import json
import redis
import websockets

CTRADER_WS_URL = "ws://localhost:8082"  # Your cTrader bridge WebSocket
REDIS_URL = "redis://localhost:6379/0"
REDIS_STREAM = "market.ticks.stream"

async def main():
    # Connect to Redis
    r = redis.from_url(REDIS_URL, decode_responses=False)
    print(f"Connected to Redis: {REDIS_URL}")
    print(f"Publishing to stream: {REDIS_STREAM}")
    
    # Connect to cTrader bridge WebSocket
    print(f"Connecting to cTrader bridge: {CTRADER_WS_URL}")
    
    async for websocket in websockets.connect(CTRADER_WS_URL):
        try:
            print("Connected to cTrader bridge!")
            
            async for message in websocket:
                try:
                    data = json.loads(message)
                    
                    # Only process tick messages
                    if data.get("type") != "tick":
                        continue
                    
                    # Extract tick data
                    symbol = data.get("symbol", "")
                    price = data.get("price", 0)
                    bid = data.get("bid", 0)
                    ask = data.get("ask", 0)
                    timestamp = data.get("time", 0)
                    
                    if not symbol or price <= 0:
                        continue
                    
                    # Create tick for Redis
                    tick = {
                        "symbol": symbol,
                        "price": price,
                        "bid": bid,
                        "ask": ask,
                        "ts": timestamp,
                        "last": price
                    }
                    
                    # Publish to Redis stream
                    r.xadd(REDIS_STREAM, {'data': json.dumps(tick)})
                    print(f"Published: {symbol} @ {price}")
                    
                except json.JSONDecodeError:
                    continue
                except Exception as e:
                    print(f"Error processing message: {e}")
                    
        except websockets.ConnectionClosed:
            print("Connection closed, reconnecting...")
            await asyncio.sleep(1)
        except Exception as e:
            print(f"Error: {e}")
            await asyncio.sleep(1)

if __name__ == "__main__":
    print("cTrader to Redis Stream Adapter")
    print("=" * 50)
    asyncio.run(main())
