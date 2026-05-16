# How to Fix cTrader Account Authorization Issue

## The Problem
You authorized the app, but cTrader linked it to an empty auto-generated account (47223753) instead of your real account (5283699).

## Root Cause
This happens when:
1. You have multiple cTIDs (cTrader IDs)
2. You authorized with the wrong cTID
3. The account 5283699 belongs to a different cTID than the one you used to authorize

## Solution

### Step 1: Check Your cTID
1. Go to: https://id.ctrader.com/
2. Log in
3. Check which email/cTID you're using
4. Go to "Trading Accounts" and verify you see account 5283699 there

### Step 2: Revoke Current Authorization
1. Go to: https://id.ctrader.com/my/settings/openapi
2. Find "Asc market" app
3. Click "Revoke" or "Remove access"

### Step 3: Re-authorize with Correct cTID
1. Make sure you're logged into the cTID that owns account 5283699
2. Run the authorization script again
3. When you see the authorization page, verify:
   - The account shown is 5283699
   - It shows your actual balance
   - It says "Pepperstone • Demo • 5283699"

### Step 4: Verify
After authorization, the script should show account 5283699 in the list, not 47223753.

## Alternative: Use Playground Tokens

If you can't get the OAuth flow to work with the correct account:

1. Go to: https://openapi.ctrader.com/apps
2. Click "Sandbox" on your "Asc market" app
3. Make sure you're logged in with the cTID that owns 5283699
4. Select "Account info and trading" scope
5. Click "Get token"
6. Copy the tokens shown

These playground tokens will work with your actual account.

## Important Notes

- Each cTID can have multiple trading accounts
- You need to authorize with the cTID that owns account 5283699
- If 5283699 is under a different cTID, you need to log out and log in with that cTID before authorizing
