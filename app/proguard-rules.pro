# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepnames class com.idunnololz.summit.user.**
-keepclassmembers class com.idunnololz.summit.user.** {
    <fields>;
}


-keepnames class com.idunnololz.summit.history.**
-keepclassmembers class com.idunnololz.summit.history.** {
    <fields>;
}

-keepnames class com.idunnololz.summit.offline.**
-keepclassmembers class com.idunnololz.summit.offline.** {
    <fields>;
}

-keepnames class com.idunnololz.summit.settings.navigation.NavBarConfig
-keepclassmembers class com.idunnololz.summit.settings.navigation.NavBarConfig {
    <fields>;
}

-keep class * extends androidx.fragment.app.Fragment{}


-keepnames class com.idunnololz.summit.preview.VideoType
-keepnames class com.idunnololz.summit.video.VideoState

# Required for shared elements transitions to work
-keep class android.app.ActivityTransitionCoordinator
-keepnames class com.idunnololz.summit.util.TextResize** {
    <fields>;
    void set*(***);
    *** get*();
}
-keepclassmembers class com.idunnololz.summit.util.TextResize** {
    <fields>;
    void set*(***);
    *** get*();
}
-keep class com.idunnololz.summit.util.TextResize** {
    <fields>;
    void set*(***);
    *** get*();
}

## Begin proguard for AndroidX
-keep class androidx.core.app.CoreComponentFactory { *; }
## End proguard for AndroidX

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

-keepclassmembers enum * { *; }

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

##---------------End: proguard configuration for Gson  ----------

-keep class * extends android.app.Activity
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int v(...);
    public static int i(...);
}


# ROOM
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
# ROOM END


## Begin proguard for AndroidX
-keep class androidx.core.app.CoreComponentFactory { *; }
## End proguard for AndroidX

## Begin Needed for SafeArgs
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable
## End Needed for SafeArgs

##---------------Begin: proguard configuration for Crashlytics  ----------
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.
##---------------End: proguard configuration for Crashlytics  ----------

-keepclassmembers enum * { *; }

## Retrofit
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
## End Retrofit

-keep class com.idunnololz.summit.api.** { <fields>; }