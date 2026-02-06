OpenAI key setup for the app

This project reads the OpenAI API key from `BuildConfig.OPENAI_API_KEY` by default. To avoid committing secrets to source control, use one of the options below:

1) Local Gradle properties (recommended for local development)
- Add the API key to `app/local.properties` (create if missing) with the line:

OPENAI_API_KEY=sk-<your-key-here>

- Ensure `app/build.gradle.kts` includes a `buildConfigField("String", "OPENAI_API_KEY", "\"${property}\"")` style injection. The project already expects `BuildConfig.OPENAI_API_KEY` to exist.

2) Environment variable (useful for CI or temporary runs)
- Set the environment variable `OPENAI_API_KEY` before launching the app. On Windows PowerShell:

```powershell
$env:OPENAI_API_KEY = 'sk-<your-key-here>'
./gradlew assembleDebug
```

3) CI / Secrets store
- Provide the secret through your CI provider's secret mechanism and inject it as a Gradle property or env var during the build.

Runtime behavior
- The app will prefer `BuildConfig.OPENAI_API_KEY` (build-time), and will fall back to `OPENAI_API_KEY` environment variable at runtime.
- If neither is present, `OpenAIClient.chatCompletion()` will throw a descriptive error. Use `OpenAIClient.isKeyConfigured()` to check availability before invoking remote calls.

Security notes
- Never commit `local.properties` or your API key to source control.
- Rotate keys if they are accidentally exposed.
