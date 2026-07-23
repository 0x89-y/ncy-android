# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ncy.notechaty.data.model.** { *; }
-keep class com.ncy.notechaty.data.sync.** { *; }
-keep class com.ncy.notechaty.data.repository.FileChatRepository$BackupManifest { *; }
-keep class com.google.gson.** { *; }
