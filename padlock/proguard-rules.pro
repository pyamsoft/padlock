# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/pyamsoft/dev/sdk/android/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


# Keep the SearchView class so that it works in Release builds.
# https://code.google.com/p/android/issues/detail?id=58508
-keep class androidx.appcompat.widget.SearchView { *; }

# Don't warn about ourselves
-dontwarn com.pyamsoft.padlock.**

# We are open source, we don't need obfuscation.
# We will still use optimizations though
-dontobfuscate

# Don't obfuscate causes the gradle build to fail after the optimization step
# The addition of !code/allocation/variable is needed to prevent this
-optimizations !code/allocation/variable

## Until AAPT2 proguard rule generation fixes
-dontoptimize

