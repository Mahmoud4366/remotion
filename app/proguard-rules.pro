# Keep app core classes
-keep class com.example.vpn.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.** { *; }

# Datastore
-keep class androidx.datastore.** { *; }

# Compose (usually handled by AAR rules, but just in case for strict shrinking)
-keep class androidx.compose.** { *; }

# Material
-keep class com.google.android.material.** { *; }

# OpenVPN Wrapper Library
-keep class com.tim.openvpn.** { *; }
-keep class net.openvpn.** { *; }

# We must keep standard entry points for Android
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }
