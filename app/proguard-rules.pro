# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ncy.notechaty.data.model.** { *; }
-keep class com.ncy.notechaty.data.sync.** { *; }
-keep class com.ncy.notechaty.data.repository.FileChatRepository$BackupManifest { *; }
-keep class com.google.gson.** { *; }
# Bare Kit 2.3.0 is delivered as a local AAR without consumer ProGuard rules.
# Its native library resolves these Java classes and callback members through
# JNI, so their release-build names must remain stable.
-keep class to.holepunch.bare.kit.** { *; }
