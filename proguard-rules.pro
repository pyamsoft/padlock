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

# We are open source, we don't need obfuscation.
# We will still use optimizations though
-dontobfuscate

# Don't obfuscate causes the gradle build to fail after the optimization step
# The addition of !code/allocation/variable is needed to prevent this
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable

# https://code.google.com/p/android/issues/detail?id=194513
# ProGuard include the legacy HTTP jar even if it is not used.
# This creates an issue where there are 7 duplicate classes and
# causes the build to fail.
# Because the duplicate classes are Note and not Warning level,
# we must ignore the Note classes here.
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**

# RetroLambda
-dontwarn java.lang.invoke.*

# Auto value
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.processor.**

# Silence some okio warnings
-dontwarn java.nio.file.**
-dontwarn org.codehaus.mojo.**

