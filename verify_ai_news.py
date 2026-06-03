"""Quick verification that AI news intelligence JSON is valid"""
import json

with open('ai_news_intelligence.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

print(f"✅ Type: {data['type']}")
print(f"✅ Source: {data['source']}")
print(f"✅ Version: {data['version']}")
print(f"✅ Timestamp: {data['timestamp']}")
print(f"✅ Total articles: {len(data['articles'])}")
print(f"\n=== First Article ===")
first = data['articles'][0]
print(f"Title: {first['title'][:60]}...")
print(f"Source: {first['source']}")
print(f"Impact Score: {first['intelligence']['impact_score']}")
print(f"Sentiment: {first['intelligence']['sentiment']}")
print(f"Confidence: {first['intelligence']['confidence']}")
print(f"Asset Tags: {', '.join(first['intelligence']['asset_tags'][:5])}")
print(f"\n✅ JSON structure is valid and ready for Android app!")
