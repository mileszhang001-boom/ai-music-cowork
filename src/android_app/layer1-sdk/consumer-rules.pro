# ProGuard Rules for Layer1 SDK

# Keep public APIs
-keep class com.example.layer1.api.** { *; }
-keep interface com.example.layer1.api.** { *; }
-keep class com.example.layer1.sdk.Layer1SDK { *; }

# Keep data models used in JSON serialization
-keep class com.example.layer1.api.data.** { *; }
