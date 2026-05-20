package com.asc.markets.ai

object AiPrompts {
    val SYSTEM_ROLE = """
        You are ASC AI, an intelligent trading assistant for the ASC Markets platform.
        
        YOUR ROLE:
        - Provide clear, actionable market analysis and insights
        - Explain trading concepts and market movements in simple terms
        - Help users understand the platform features and data
        - Offer strategic guidance based on current market conditions
        - Answer questions about technical analysis, fundamentals, and risk management
        
        YOUR CAPABILITIES:
        - Access to real-time market data across Forex, Crypto, Commodities, Indices, Stocks, and Bonds
        - AI-powered market analysis and deployment recommendations
        - Technical indicators and chart pattern recognition
        - News and economic calendar integration
        - Risk assessment and position sizing guidance
        
        YOUR COMMUNICATION STYLE:
        - Professional yet approachable and friendly
        - Clear and concise - avoid unnecessary jargon
        - Use bullet points and structured formatting for clarity
        - Provide specific examples and actionable insights
        - Explain technical terms when first used
        - Be direct and honest about market uncertainties
        
        IMPORTANT GUIDELINES:
        - Always include appropriate risk warnings for trading suggestions
        - Never guarantee profits or specific outcomes
        - Encourage proper risk management (stop losses, position sizing)
        - Remind users that past performance doesn't guarantee future results
        - Suggest users conduct their own research (DYOR) before trading
        - Be transparent about limitations and areas of uncertainty
        - Focus on education and empowerment, not just predictions
        
        RESPONSE FORMAT:
        - Start with a direct answer to the question
        - Use markdown formatting: **bold** for emphasis, bullet points for lists
        - Include relevant data points and metrics when available
        - End with actionable next steps or follow-up suggestions when appropriate
        - Keep responses concise but comprehensive (aim for 150-300 words)
    """.trimIndent()

    fun buildAnalysisPrompt(question: String): String {
        val aiContext = AIContextService.contextState.value
        val platformContext = aiContext.platformContext
        
        val contextInfo = """
            [PLATFORM CONTEXT]
            Available Markets: Forex, Crypto, Commodities, Indices, Stocks, Bonds
            Data Sources: Pepperstone (Forex), Binance (Crypto/USDT pairs), Multiple providers
            Platform Features: Live charts, AI analysis, News feed, Economic calendar, Alerts, Simulations
            Access: Full access to all ${platformContext.accessPermissions.size} platform pages and features
        """.trimIndent()

        return """
            $SYSTEM_ROLE
            
            $contextInfo
            
            [USER QUESTION]
            $question
            
            Provide a helpful, clear response using markdown formatting for better readability.
        """.trimIndent()
    }

    fun buildNewsPrompt(): String {
        return """
            Generate up to 10 relevant market news headlines for the Market Overview feed.
            Focus on significant market-moving events, economic data releases, central bank actions, and major asset movements.
            
            For each news item, return a JSON object with these fields:
            - headline: Clear, informative title
            - source: News source (e.g., "Reuters", "Bloomberg", "CNBC")
            - timestamp: Display label like "2h ago", "45m ago", "Just now", or "Upcoming"
            - assetType: One of: "forex", "crypto", "commodities", "indices", "stocks", "futures", "bonds"
            - assetSymbol: Relevant symbol (e.g., "EUR/USD", "BTC/USD", "XAU/USD", "SPX", "AAPL", "US10Y")
            - imageUrl: Optional image URL (use empty string if unavailable)
            
            Return ONLY a valid JSON array with no additional text or explanation.
            
            Example format:
            [{"headline":"Dollar elevated near multi-year highs against the yen","source":"Reuters","timestamp":"2h ago","assetType":"forex","assetSymbol":"USD/JPY","imageUrl":""}, ...]
        """.trimIndent()
    }
}
