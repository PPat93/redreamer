# Shrinking is currently disabled (isMinifyEnabled = false). These rules exist so it can be
# switched on later without silently corrupting data — everything below protects a name that is
# matched as a *string* at runtime, which R8 would otherwise be free to rename.

# Mood is persisted in the database by enum constant name (Converters uses valueOf/name).
# If R8 renames these constants, every stored mood fails to parse on the next read.
-keepclassmembers enum com.parrotworks.redreamer.data.Mood {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Backup JSON is a long-lived, user-facing file format; its field names must stay stable so
# exports written by one build still import into another.
-keep class com.parrotworks.redreamer.data.backup.** { *; }

# The editor stashes its draft into SavedStateHandle via java.io.Serializable, which resolves
# classes and fields reflectively.
-keep class com.parrotworks.redreamer.ui.editor.DreamEditorUiState { *; }
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
}

# kotlinx.serialization keeps its generated serializers reachable.
-keepattributes *Annotation*, InnerClasses
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-keepclassmembers class **$serializer {
    *** INSTANCE;
}
