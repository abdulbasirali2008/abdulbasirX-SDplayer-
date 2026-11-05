# Rules for the optimized release build.

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Media3 playback and the media session.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Libraries pulled in by Media3 that mention classes not shipped on Android.
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
