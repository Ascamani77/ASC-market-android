OpenAI key setup for the app

This project reads the OpenAI API key from `BuildConfig.OPENAI_API_KEY` by default. To avoid committing secrets to source control, use one of the options below:

1) Local Gradle properties (recommended for local development)
- Add the API key to `app/local.properties` (create if missing) with the line:

OPENAI_API_KEY=sk-<your-key-here>

- Ensure `app/build.gradle.kts` includes a `buildConfigField("String", "OPENAI_API_KEY", "\"${property}\"")` style injection. The project already expects `BuildConfig.OPENAI_API_KEY` to exist.

2) CI / Secrets store
- Provide the secret through your CI provider's secret mechanism and inject it as a Gradle property during the build.

Runtime behavior
- The app only uses `BuildConfig.OPENAI_API_KEY` (build-time). The app intentionally does not accept runtime or pasted API keys for security and auditability reasons. Add the key to `app/local.properties` and rebuild.
- If the build-time key is not present, `OpenAIClient.chatCompletion()` will throw a descriptive error.

Security notes
- Never commit `local.properties` or your API key to source control.
- Do not paste API keys into the app or any runtime input fields; the application will not accept or persist runtime API keys.
- Rotate keys if they are accidentally exposed.
