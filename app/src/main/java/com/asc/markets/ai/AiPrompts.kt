package com.asc.markets.ai

import com.asc.markets.data.ChatMessage
import com.asc.markets.data.AppView

/**
 * Centralized AI prompt management for the ASC Markets platform.
 */
object AiPrompts {
    val SYSTEM_ROLE = """
        You are ASC AI, an intelligent trading assistant for the ASC Markets platform.
        
        YOUR ROLE:
        - Provide clear, actionable market analysis
        - Explain trading concepts in simple terms
        - Help users understand market movements
        - Suggest strategies based on current conditions
        - Answer questions about the platform and features
        
        YOUR CAPABILITIES:
        - Access to real-time market data
        - AI-powered market analysis and predictions
        - Technical and fundamental analysis
        - Risk management guidance
        - Platform navigation help
        
        YOUR STYLE:
        - Professional but friendly
        - Clear and concise
        - Use bullet points for clarity
        - Provide specific examples
        - Explain technical terms when used
        
        IMPORTANT RULES:
        - Always include risk warnings for trading advice
        - Never guarantee profits or outcomes
        - Encourage proper risk management
        - Suggest users do their own research (DYOR)
        - Be honest about limitations and uncertainties
    """.trimIndent()

    /**
     * Builds a prompt for general market analysis or chat response.
     */
    fun buildAnalysisPrompt(
        question: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        currentMarketData: Map<String, Any>? = null,
        userContext: String? = null
    ): String {
        val historyContext = if (conversationHistory.isNotEmpty()) {
            """
            [CONVERSATION HISTORY]
            ${conversationHistory.takeLast(5).joinToString("\n") { 
                "${it.role.uppercase()}: ${it.content}" 
            }}
            """.trimIndent()
        } else ""
        
        val marketContext = if (currentMarketData != null) {
            """
            [CURRENT MARKET DATA]
            ${currentMarketData.entries.joinToString("\n") { 
                "- ${it.key}: ${it.value}" 
            }}
            """.trimIndent()
        } else ""
        
        // AI DEPLOYMENTS REMOVED
        
        val platformContext = """
            [PLATFORM CONTEXT]
            - Available Markets: Forex, Crypto, Commodities, Indices, Stocks, Bonds
            - Data Sources: MT5 EA (Primary), Binance (Crypto)
            - Features: Live charts, EA AI confidence, News feed, Alerts, Simulations
        """.trimIndent()
        
        return """
            $SYSTEM_ROLE
            
            $platformContext
            
            $historyContext
            
            $marketContext
            
            ${if (userContext != null) "[USER CONTEXT]\n$userContext\n" else ""}
            
            [USER QUESTION]
            $question
            
            Provide a helpful, clear response. Use markdown formatting for better readability.
        """.trimIndent()
    }

    /**
     * Builds a prompt specifically for news analysis.
     */
    fun buildNewsPrompt(): String {
        return """
            $SYSTEM_ROLE
            
            You are currently analyzing the news feed. 
            Summarize the latest developments and their likely impact on market volatility and institutional flow.
        """.trimIndent()
    }
}
