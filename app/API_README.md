This app is configured to use the Render-hosted API by default.

Base URL:
https://webserverapi-vc37.onrender.com/

How it works:
- The Retrofit client reads the API base URL from BuildConfig.API_BASE_URL at build time.
- At runtime the app prefers a value stored in SharedPreferences (key: `api_base_url`). To override the URL at runtime call:

    SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
    prefs.setApiBaseUrl("https://your-override-url.com/");

- After changing the base URL at runtime, recreate any Retrofit clients (the app currently caches a singleton Retrofit instance). The easiest way is to update code to call `ApiClient.reset()` then `ApiClient.getClient(context)`; or simply restart the app.

Notes for developers:
- BuildConfig.API_BASE_URL is set in `app/build.gradle.kts`.
- If you want different URLs per build variant, update `build.gradle.kts` accordingly.
