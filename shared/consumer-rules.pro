# =====================================================================
# Official R8 / ProGuard / DexGuard Rules for HeimUI Core
# =====================================================================

-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# 1. Protect @Serializable models in data.dto package
-keep @kotlinx.serialization.Serializable class io.heimui.core.data.dto.** { *; }

# 2. Preserve synthetic compile-time generated serializers
-keepclassmembers class io.heimui.core.data.dto.** {
    *** Companion;
    public static ** serializer();
    public static ** serializer(...);
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# 3. Protect Enums and polymorphic discriminators
-keepclassmembers enum io.heimui.core.data.dto.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    @kotlinx.serialization.SerialName <fields>;
}

# 4. Preserve UI component constructors
-keepclassmembers class * extends io.heimui.core.domain.model.component.HeimComponent {
    <init>(...);
}
