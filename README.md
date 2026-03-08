# ASC - Research Center Kotlin Translation

This folder contains a high-fidelity translation of the Research Center news reader application from React/TypeScript to Kotlin (Jetpack Compose), integrated into the ASC project.

## Structure

- `data/models/`: Data classes matching the TypeScript interfaces.
- `services/`: Logic for news fetching and AI integration.
- `ui/theme/`: Colors and typography matching the dark-themed web version.
- `ui/components/`: Reusable Compose components (Sidebar, NewsList, etc.).
- `ui/screens/`: Main application screen.

## Dependencies

To use this code in your Android app, you will need the following dependencies in your `build.gradle.kts`:

```kotlin
dependencies {
    // Compose
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.foundation:foundation:1.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    
    // Networking (Recommended)
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
}
```

## Integration

1. Copy the `com.researchcenter` package into your `app/src/main/java` (or `kotlin`) directory.
2. Update the `MainActivity` or call `MainScreen()` from your existing navigation.
3. Replace the placeholder networking logic in `NewsService` and `AiService` with actual Ktor calls.
4. Add your API keys to the `AiService` and `NewsService`.

## Aesthetics

The theme uses a pure black background (`#000000`) with white text and subtle borders (`white/10`) to match the terminal-like aesthetic of the original web application.
