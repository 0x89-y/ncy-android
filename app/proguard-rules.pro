# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ncy.notechaty.data.model.** { *; }
-keep class com.ncy.notechaty.data.sync.** { *; }
-keep class com.ncy.notechaty.data.repository.FileChatRepository$BackupManifest { *; }
-keep class com.google.gson.** { *; }

# Gson reads `object : TypeToken<List<X>>() {}` through that anonymous class's
# generic signature, which R8 full mode strips from classes it does not keep.
# Gson 2.10 ships no rule for it; without this the read fails at runtime.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
