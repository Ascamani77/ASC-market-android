# Groq API Integration

## Overview
Replaced OpenAI API with Groq API due to OpenAI quota exhaustion. The key provided was a Groq API key (starts with `gsk_`), not Mistral.

## Changes Made

### 1. Configuration Files

#### `local.properties`
- Added: `GROQ_API_KEY=YOUR_GROQ_API_KEY`
- Kept: `OPENAI_API_KEY` (for backward compatibility)
- Added: `GEMINI_API_KEY` (for fallback)

#### `app/build.gradle.kts`
- Added BuildConfig field for Groq API key:
```kotlin
buildConfigField("String", "GROQ_API_KEY", "\"${localProps.getProperty("GROQ_API_KEY") ?: project.findProperty("GROQ_API_KEY") ?: ""}\"")
```

### 2. New Client

#### `app/src/main/java/com/asc/markets/backend/GroqClient.kt`
Created new Groq API client with:
- Base URL: `https://api.groq.com/openai/v1`
- Default model: `llama-3.3-70b-versatile`
- Same interface as OpenAIClient for easy migration
- Methods:
  - `chatCompletion(prompt: String, model: String): String`
  - `isKeyConfigured(): Boolean`

### 3. Code Updates

#### `TradingAssistantEngine.kt`
- Changed import from `OpenAIClient` to `GroqClient`
- Updated `chatWithForexExpert()` to use `BuildConfig.GROQ_API_KEY` and `GroqClient.chatCompletion()`

#### `AscAiTextExplainer.kt`
- Changed import from `OpenAIClient` to `GroqClient`
- Updated `explain()` method to try Groq first, then fall back to Gemini
- Updated error messages to reference Groq instead of OpenAI
- Updated prompt text to mention Groq instead of OpenAI

#### `MarketOverviewTab.kt`
- Updated `fetchNewsFromGemini()` to use Groq API
- Changed API endpoint to `https://api.groq.com/openai/v1/chat/completions`
- Changed model to `llama-3.3-70b-versatile`
- Updated all log messages and comments to reference Groq

#### `ExplanationTab.kt`
- Updated UI text to show "Gemini/Groq" instead of "Gemini/OpenAI"
- Updated comments to reference Groq

## API Compatibility

Groq API is compatible with OpenAI's chat completions format:
- Same request structure (model, messages, max_tokens)
- Same response structure (choices[0].message.content)
- Same authentication method (Bearer token)
- Uses OpenAI-compatible endpoint path

## Models Used

- **Llama 3.3 70B Versatile** (`llama-3.3-70b-versatile`): Used for all chat completions
  - Fast inference speed (Groq's specialty)
  - High quality responses
  - Cost-effective
  - Suitable for news generation, analysis, and explanations

## Fallback Strategy

The app now uses a tiered approach:
1. **Primary**: Groq API (if key configured) - Fast and reliable
2. **Fallback**: Gemini API (if Groq fails)
3. **Last Resort**: Synthetic/mock responses

## Why Groq?

- **Speed**: Groq provides extremely fast inference times
- **Cost**: More cost-effective than OpenAI
- **Compatibility**: Uses OpenAI-compatible API format
- **Quality**: Llama 3.3 70B provides excellent responses

## Testing

After rebuilding the app, test:
1. Trading Assistant chat functionality
2. ASC AI explanations in the Explanation Tab
3. News generation in Market Overview
4. Error handling when API is unavailable

## Security Notes

- API keys are stored in `local.properties` (not committed to git)
- Keys are injected at build time via BuildConfig
- Runtime key pasting is blocked for security
