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

-keepnames class com.idunnololz.summit.tabs.**
-keepclassmembers class com.idunnololz.summit.tabs.** {
    <fields>;
}


-keepnames class com.idunnololz.summit.reddit_objects.**
-keepclassmembers class com.idunnololz.summit.reddit_objects.** {
    <fields>;
}

-keepnames class com.idunnololz.summit.reddit_actions.**
-keepclassmembers class com.idunnololz.summit.reddit_actions.** {
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