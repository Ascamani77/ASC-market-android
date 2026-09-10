# Build Issue Fixed: Unable to delete R.jar

The build error `java.io.IOException: Unable to delete file '.../R.jar'` was resolved. This error is typically caused by a file lock held by a process (usually the Gradle daemon or a background Java process) on Windows.

## Changes Made
No source code changes were required. The issue was resolved by managing the build environment:
1.  **Stopped Gradle Daemons**: Ran `./gradlew --stop` to terminate any idle or active Gradle processes that might be holding locks on files in the `build` directory.
2.  **Cleaned Project**: Successfully executed the `clean` task to remove the `build` directory.
3.  **Rebuilt Project**: Ran `assembleDebug` to verify that the project builds correctly from a clean state.

## Verification Results
- `clean` task: **Passed**
- `assembleDebug` task: **Passed**

> [!TIP]
> If you encounter this error again, you can manually stop the Gradle daemons from the terminal using `./gradlew --stop` or by killing `java.exe` processes in the Task Manager that are associated with the project.
