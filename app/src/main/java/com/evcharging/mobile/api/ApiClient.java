package com.evcharging.mobile.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import com.evcharging.mobile.BuildConfig;
import com.evcharging.mobile.utils.SharedPreferencesHelper;

public class ApiClient {
    private static Retrofit retrofit = null;

    /**
     * Returns a Retrofit client configured with the app's API base URL.
     * Priority: runtime override (SharedPreferences) -> BuildConfig.API_BASE_URL
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Create logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create OkHttp client
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Determine base URL: prefer runtime override in SharedPreferences
            String baseUrl = BuildConfig.API_BASE_URL;
            try {
                SharedPreferencesHelper prefs = new SharedPreferencesHelper(context.getApplicationContext());
                String overridden = prefs.getApiBaseUrl();
                if (overridden != null && overridden.trim().length() > 0) {
                    baseUrl = overridden;
                }
            } catch (Exception ignored) {
                // If preferences can't be read, fall back to BuildConfig
            }

        // Configure Gson to use ISO-8601 date format (server commonly expects this)
        Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .create();

        retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
        }
        return retrofit;
    }

    /**
     * Reset the cached Retrofit instance. Call this after changing the API base URL at runtime
     * (for example, after calling SharedPreferencesHelper.setApiBaseUrl(...)).
     */
    public static void reset() {
        retrofit = null;
    }
}