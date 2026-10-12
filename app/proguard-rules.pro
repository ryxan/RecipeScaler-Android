# Project-specific R8 rules.
# Hilt, Navigation (type-safe routes) and kotlinx.serialization ship their own consumer rules.
# The keeps below cover code reached via reflection at runtime.

-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions

# Kotlinx Serialization — keeps generated serializers and @Serializable DTOs
# (AI import response DTOs are decoded through reflection-adjacent generated code).
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializer *;
}
-keep class com.ryxan.recipescaler.core.data.importer.** { *; }

# Play Billing — the library ships consumer rules; keep our reflection-free
# DTO surface anyway so R8 doesn't strip members ProRepository reads.
-dontwarn com.android.billingclient.**

# Firebase AI Logic — keep the classes it reflects on.
-dontwarn com.google.firebase.**
