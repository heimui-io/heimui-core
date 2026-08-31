# =====================================================================
# Reglas Oficiales R8 / ProGuard / DexGuard para HeimUI Core
# =====================================================================

-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# 1. Proteger modelos @Serializable en la capa domain
-keep @kotlinx.serialization.Serializable class io.heimui.core.domain.model.** { *; }

# 2. Preservar serializadores sintéticos generados en tiempo de compilación
-keepclassmembers class io.heimui.core.domain.model.** {
    *** Companion;
    public static ** serializer();
    public static ** serializer(...);
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# 3. Proteger Enums y discriminadores polimórficos
-keepclassmembers enum io.heimui.core.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    @kotlinx.serialization.SerialName <fields>;
}

# 4. Preservar constructores de componentes de UI
-keepclassmembers class * extends io.heimui.core.domain.model.component.HeimComponent {
    <init>(...);
}
