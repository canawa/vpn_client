# Keep libv2ray / gomobile bindings
-keep class go.** { *; }
-keep class libv2ray.** { *; }

# kotlinx.serialization: сериализуемые модели (ProxyNode, SubscriptionInfo и т.п.)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ru.nubovpn.app.**$$serializer { *; }
-keepclassmembers class ru.nubovpn.app.** {
    *** Companion;
}
-keepclasseswithmembers class ru.nubovpn.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
