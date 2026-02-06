Remote Config Setup
===================

This project supports a small set of BuildConfig-backed options to control remote feature-flagging for the Macro Intelligence Stream.

Available BuildConfig fields (added in `app/build.gradle.kts`):

- `REMOTE_CONFIG_URL` (String) — URL of a small JSON document used for remote flags. Example JSON: `{ "promote_macro_stream": true }`.
- `DEFAULT_FORCE_REMOTE` (boolean) — compile-time default for whether remote flags should force-override local prefs.
- `DEFAULT_REMOTE_POLL_MS` (long) — compile-time default poll interval in milliseconds.

How to set them locally (do NOT commit secrets into VCS):

1) local.properties (recommended for local dev)

Add the following lines to your `local.properties` in the root of the repo (this file is usually ignored):

```
REMOTE_CONFIG_URL=https://example.com/remote-config.json
DEFAULT_FORCE_REMOTE=false
DEFAULT_REMOTE_POLL_MS=10000
```

2) gradle.properties (project-level)

You can also add the same keys to `gradle.properties` (committed or CI-managed) as project properties:

```
REMOTE_CONFIG_URL=https://example.com/remote-config.json
DEFAULT_FORCE_REMOTE=false
DEFAULT_REMOTE_POLL_MS=10000
```

3) Command-line (CI or ad-hoc builds)

Pass properties to Gradle on the command-line for ephemeral builds or CI:

```bash
./gradlew assembleDebug -PREMOTE_CONFIG_URL="https://example.com/remote-config.json" -PDEFAULT_FORCE_REMOTE=false -PDEFAULT_REMOTE_POLL_MS=10000
```

Notes and verification
- The app reads these values into `BuildConfig` (see `app/build.gradle.kts`). If `REMOTE_CONFIG_URL` is empty, remote polling is disabled.
- The app also supports a reflection-safe lookup, so missing fields fall back gracefully.
- The dashboard shows the remote status in the top-right chip; tapping the poll chip copies the configured URL (or poll interval) to clipboard.
- For CI/production, host a tiny JSON file containing the flags you want to apply. Example body:

```json
{
  "promote_macro_stream": true
}
```

Security
- Do not store secrets (API keys) in `local.properties` committed to source control. Use secure CI secrets for sensitive values.

If you want, I can add a small `curl` script to validate the JSON endpoint and show the parsed keys locally.
