# NEXORA Browser

A starter native Android browser project using Kotlin and Android WebView, styled with a light-red/white NEXORA UI.

## Build APK from your phone (cloud build)

1. Extract this ZIP.
2. Create a GitHub repository and upload the extracted files, keeping the folder structure.
3. Open the repository's **Actions** tab.
4. Select **Build NEXORA Browser APK** and tap **Run workflow** (or push to `main`).
5. When the workflow finishes successfully, open that run, scroll to **Artifacts**, and download `NEXORA-Browser-debug-apk`.
6. Extract the artifact ZIP and install `app-debug.apk`. Android may ask you to allow installation from the browser/file manager you used.

## Features in this starter

- Android WebView browsing and URL/search bar
- Desktop user-agent toggle
- Multiple normal tabs
- Bookmarks and recent history stored locally
- Google, Bing, DuckDuckGo, and Brave Search selection UI
- Android DownloadManager integration
- Separate incognito Activity process and WebView data directory suffix on Android 9+
- Basic touchpad/cursor prototype

## Important limitations

- This is a starter project, not a fully production-tested browser.
- The search engine selector currently stores the selected engine; the address bar search routing should be wired to that saved preference before release.
- The virtual mouse is a prototype: web page hit-testing and clicks can fail on complex sites. It is not a system-wide pointer.
- Downloads are delegated to Android DownloadManager; some sites require authentication, cookies, or special headers and may not download correctly.
- Incognito runs in a separate Android process with a separate WebView data directory on Android 9+. The session does not add pages to normal history. Private mode is not a guarantee of anonymity: websites, network providers, and downloaded files can still reveal activity. Close the private session to clear its WebView cookies/cache.
- The build workflow creates a debug APK for testing, not a Play Store signed release.
- Check Android WebView updates and test navigation, downloads, pop-ups, media, permissions, and incognito cleanup on your device before sharing the APK.

## Project details

- Package: `com.nexora.browser`
- Minimum Android: 9 (API 28)
- Compile SDK: 35
- Java: 17
