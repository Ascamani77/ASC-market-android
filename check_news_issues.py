"""Check why news is showing missing source and low confidence"""
import json

with open('ai_news_intelligence.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

articles = data['articles'][:10]

print("=== CHECKING FIRST 10 ARTICLES ===\n")

for i, article in enumerate(articles, 1):
    print(f"{i}. {article['title'][:60]}...")
    print(f"   Source: '{article.get('source', 'MISSING')}'")
    print(f"   Impact Score: {article['intelligence']['impact_score']}")
    print(f"   Confidence: {article['intelligence']['confidence']}")
    print(f"   Sentiment: {article['intelligence']['sentiment']}")
    print()

# Check statistics
sources_empty = sum(1 for a in data['articles'] if not a.get('source') or a.get('source') == '')
confidence_low = sum(1 for a in data['articles'] if a['intelligence']['confidence'] == 'low')
total = len(data['articles'])

print(f"\n=== STATISTICS ===")
print(f"Total articles: {total}")
print(f"Empty source: {sources_empty} ({sources_empty/total*100:.1f}%)")
print(f"Low confidence: {confidence_low} ({confidence_low/total*100:.1f}%)")
