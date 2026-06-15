# =====================================================================
# AGGRESSIVE SHRINKING & OBFUSCATION RULES
# =====================================================================

# Broadens visibility modifiers for better inlining and optimization
-allowaccessmodification

# Obfuscates code by flattening package structures into the root package
# (Excellent security against reverse-engineering; hides class locations)
-repackageclasses ''

# =====================================================================
# CORE ANDROID KEEP RULES
# =====================================================================
# Keeps entry points which are instantiated via reflection by the Android OS.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# =====================================================================
# REFLECTION & SERIALIZATION SAFEGUARDS (PREVENTS JSON PARSING CRASHES)
# =====================================================================
# Keep Generic Signatures, Annotations, and Metadata required by Kotlin Serialization & Ktor
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Metadata

# Keep any classes and members marked with @Keep (like data classes/models)
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# Keep Kotlin Serialization auto-generated companion serializers and properties
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    *** Companion;
}
-keep class * implements kotlinx.serialization.KSerializer { *; }

# =====================================================================
# NOTE ON THIRD-PARTY LIBRARIES (Retrofit, Firebase, Room, Ktor, OkHttp)
# =====================================================================
# Modern Android libraries (.aar) bundle their own "Consumer Proguard Rules" 
# which R8 merges automatically. Manually keeping the entire library namespaces 
# (like -keep class retrofit2.** { *; }) is unnecessary and prevents R8 from 
# shrinking them, resulting in larger APK sizes. They have been omitted here
# so R8 can optimize and shrink them properly based on their library specs.

# =====================================================================
# GLOBAL WARNING SUPPRESSION
# =====================================================================
# Suppresses missing classes warnings globally to prevent R8 compilation crashes.
-dontwarn **

# =====================================================================
# AIDL & BINDER IPC KEEP RULES (UNIVERSAL)
# =====================================================================
# Keep all classes that extend Binder (like AIDL Stubs)
-keep class * extends android.os.Binder { *; }

# Keep all classes that implement IInterface (like AIDL interfaces)
-keep interface * extends android.os.IInterface { *; }
