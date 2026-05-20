# Context Page Feature Analysis

## ✅ **GOOD NEWS: Context Page IS Working!**

Your context page feature **IS implemented and working correctly**. Here's how it works:

### How It Works:

1. **User Selects Context Page** (e.g., "Market Status")
   ```kotlin
   viewModel.setAscChatContextPage(AppView.MARKET_STATUS)
   ```

2. **Context is Stored**
   ```kotlin
   _ascChatContextPageId.value = "MARKET_STATUS"
   ```

3. **When Sending Message**
   ```kotlin
   fun sendAscChatMessage(...) {
       val appContext = buildChatAppContext(deployments)
       // appContext includes the focused page context
   }
   ```

4. **Context is Built**
   ```kotlin
   private fun buildChatAppContext(...): String {
       val chatContextPage = AppView.values().firstOrNull { 
           it.name == _ascChatContextPageId.value 
       }
       
       appendLine("chat_context_focus=${chatContextPage.name}")
       appendLine("chat_context_focus_label=${chatContextPage.toAiContextLabel()}")
       appendLine("chat_context_payload_start")
       appendLine(buildFocusedPageContext(chatContextPage))  // ✅ This provides page-specific data
       appendLine("chat_context_payload_end")
   }
   ```

5. **Page-Specific Context is Provided**
   ```kotlin
   private fun buildFocusedPageContext(page: AppView?): String {
       return when (page) {
           AppView.MARKET_STATUS -> buildMarketStatusFocusedContext()
           AppView.CALENDAR -> buildCalendarFocusedContext()
           AppView.NEWS -> buildNewsFocusedContext()
           AppView.WATCHLIST -> buildWatchlistFocusedContext()
           // ... and 30+ other pages
       }
   }
   ```

### What Context Each Page Provides:

#### **MARKET_STATUS** (Your New Page!)
```kotlin
private fun buildMarketStatusFocusedContext(): String {
    return buildString {
        appendLine("market_status_context_available=true")
        appendLine("market_status_symbol=${marketState.symbol}")
        appendLine("market_status_bias=${marketState.technicalBias}")
        appendLine("market_status_confidence=${marketState.confidence}")
        appendLine("market_status_safety_blocked=${marketState.safetyBlocked}")
        // ... market data
    }
}
```

#### **CALENDAR**
- Calendar events (display + AI payload)
- Date ranges, selected date
- Event details (time, currency, impact)

#### **NEWS**
- News items with timestamps
- Categories and country codes
- Latest updates

#### **WATCHLIST**
- Watchlist items sorted by confidence
- Asset details, move probability
- Risk levels

#### **MARKETS**
- All market pairs
- Prices, changes, categories
- Live market data

### The Problem: AI Prompt is Too Restrictive

The context page **IS working**, but the AI responses are poor because of the **system prompt**, not the context. Look at what the AI receives:

```kotlin
val INSTITUTIONAL_OPERATIONAL_DIRECTIVE = """
    INSTITUTIONAL_OPERATIONAL_DIRECTIVE: Dedicate 90% of your logic to PRE-MOVE timing. 
    Strictly suppress all retail microstructure 'trading advice'. 
    Prioritize macro event accumulation, timing, and institutional liquidity signals; 
    avoid providing trade-level action or dispatch suggestions.
""".trimIndent()
```

This tells the AI to:
- ❌ "Suppress trading advice"
- ❌ Use "surveillance-style" language
- ❌ Focus on "institutional liquidity signals" (confusing jargon)
- ❌ Avoid "trade-level action"

**Result:** AI is too cautious and unhelpful, even though it has all the right context!

### Example of What AI Receives:

When you select **Market Status** page and ask "What's the market status?", the AI gets:

```
INSTITUTIONAL_OPERATIONAL_DIRECTIVE: Dedicate 90% of your logic to PRE-MOVE timing...

[PLATFORM_CONTEXT_KNOWLEDGE]
- Access Permissions: Dashboard, Market Watch, Markets, ... Market Status ... (40 pages)
- Data Sources: Pepperstone, Binance
- Operational Directives: ...

chat_context_focus=MARKET_STATUS
chat_context_focus_label=Market Status
chat_context_policy=Use the selected page as the main lens when one is selected
chat_context_payload_start
market_status_context_available=true
market_status_symbol=EUR/USD
market_status_bias=BULLISH
market_status_confidence=72
market_status_safety_blocked=false
market_status_close=1.0850
chat_context_payload_end

selected_asset=EUR/USD
selected_asset_price=1.0850
selected_asset_change_pct=0.45
balance=10000.00
equity=10050.00
floating_pnl=50.00
...

[USER QUESTION]
What's the market status?

Provide a concise surveillance-style analysis for: What's the market status?
```

**The AI has ALL the data it needs!** But the prompt tells it to be "surveillance-style" and "suppress advice", so it gives unhelpful responses.

## The Real Issue: Prompt Quality, Not Context

### Current Prompt Problems:

1. **Too Restrictive**
   - "Suppress trading advice" → AI won't help
   - "Surveillance-style" → Robotic responses
   - "Avoid trade-level action" → Can't suggest anything

2. **Too Much Jargon**
   - "PRE-MOVE timing"
   - "Institutional liquidity signals"
   - "Retail microstructure"
   - Users don't understand this language

3. **No Conversation Memory**
   - Each message is isolated
   - Can't reference previous chat
   - User has to repeat context

4. **Poor Formatting**
   - No markdown support
   - No bullet points
   - Wall of text responses

## Solutions:

### 1. **Fix the System Prompt** (High Priority)

Replace the restrictive prompt with a helpful one:

```kotlin
val SYSTEM_ROLE = """
You are ASC AI, an intelligent trading assistant.

YOUR ROLE:
- Provide clear, actionable market analysis
- Explain concepts in simple terms
- Help users understand market movements
- Answer questions about the platform

YOUR STYLE:
- Professional but friendly
- Use bullet points for clarity
- Provide specific examples
- Explain technical terms

IMPORTANT:
- Always include risk warnings
- Never guarantee profits
- Encourage proper risk management
- Be honest about limitations
""".trimIndent()
```

### 2. **Add Conversation History** (High Priority)

```kotlin
fun buildAnalysisPrompt(
    question: String,
    conversationHistory: List<ChatMessage> = emptyList()
): String {
    val historyContext = if (conversationHistory.isNotEmpty()) {
        """
        [CONVERSATION HISTORY]
        ${conversationHistory.takeLast(5).joinToString("\n") { 
            "${it.role.uppercase()}: ${it.content}" 
        }}
        """.trimIndent()
    } else ""
    
    return """
        $SYSTEM_ROLE
        
        $historyContext
        
        [CURRENT CONTEXT]
        ${buildChatAppContext(deployments)}  // ✅ This already includes focused page context!
        
        [USER QUESTION]
        $question
    """.trimIndent()
}
```

### 3. **Add Markdown Rendering** (Medium Priority)

Install library:
```kotlin
implementation("com.halilibo.compose-richtext:richtext-ui:0.17.0")
implementation("com.halilibo.compose-richtext:richtext-commonmark:0.17.0")
```

Update ChatBubble:
```kotlin
@Composable
fun ChatBubble(message: ChatMessage) {
    if (message.role == "model") {
        RichText { Markdown(message.content) }
    } else {
        Text(message.content)
    }
}
```

## Conclusion:

✅ **Context Page Feature: WORKING PERFECTLY**
- Stores selected page
- Provides page-specific data
- Includes in AI prompt
- Supports 40+ pages including Market Status

❌ **AI Responses: POOR DUE TO PROMPT**
- Too restrictive system prompt
- No conversation history
- No markdown formatting
- Confusing jargon

### Fix Priority:
1. **Change system prompt** (5 minutes, huge impact)
2. **Add conversation history** (10 minutes, big improvement)
3. **Add markdown rendering** (15 minutes, better UX)

The context page is doing its job! The AI just needs better instructions on how to use that context. 🎯
