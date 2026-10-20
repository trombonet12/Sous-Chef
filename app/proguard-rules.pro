# kotlinx.serialization: conservar serializers generados de los modelos de :receta y de data/.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.trombito.souschef.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.trombito.souschef.**$$serializer { *; }
