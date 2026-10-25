# kotlinx.serialization: conservar serializers generados de los modelos de :receta y de data/.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.trombito.souschef.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.trombito.souschef.**$$serializer { *; }

# Privacidad: R8 elimina las llamadas a Log.v/d/i en release (p. ej. las ordenes de voz de CronoViewModel y
# EscuchaAndroid). Log.w y Log.e se conservan. Solo funciona con la optimizacion de R8 activa (proguard-android-optimize).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
