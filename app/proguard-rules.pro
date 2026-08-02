# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Javascript Bridge and classes called from JS
-keepclassmembers class com.sfdnsapp.pro.MainActivity$AndroidWebBridge {
   public *;
}
-keepattributes JavascriptInterface
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# General code obfuscation and protection
-repackageclasses 'com.sfdnsapp.pro.protected'
-allowaccessmodification
-dontusemixedcaseclassnames

# Keep custom Application and MainActivity
-keep class com.sfdnsapp.pro.MainActivity { *; }
-keep class com.sfdnsapp.pro.DnsVpnService { *; }
-keep class com.sfdnsapp.pro.BootReceiver { *; }
-keep class com.sfdnsapp.pro.DnsTileService { *; }
