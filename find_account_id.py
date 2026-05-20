#!/usr/bin/env python3
"""
Find the correct account ID for your cTrader token
"""

import json
import sys

# Read token from file
with open('ctrader_tokens.json', 'r') as f:
    token_data = json.load(f)
    ACCESS_TOKEN = token_data['accessToken']

print("=" * 60)
print("Finding Account ID")
print("=" * 60)

print(f"\nAccess Token: {ACCESS_TOKEN[:30]}...")

print("\n" + "=" * 60)
print("IMPORTANT:")
print("=" * 60)
print("\nWhen you generated the token, which account did you select?")
print("The account ID you selected during authorization is the one")
print("that's linked to this token.")
print("\nCommon account IDs:")
print("  - 5287516 (the one you mentioned)")
print("  - 47312778 (the old one)")
print("  - Or a different one?")

print("\n" + "=" * 60)
print("Solution:")
print("=" * 60)
print("\n1. Go back to: https://id.ctrader.com/my/settings/openapi/grantingaccess/...")
print("2. Look at which accounts are listed")
print("3. Note the account ID you want to use")
print("4. Generate a NEW token and select that specific account")
print("\nOR")
print("\n1. Login to cTrader web/desktop")
print("2. Check your account number in the top right")
print("3. Use that account ID")

print("\n" + "=" * 60)
