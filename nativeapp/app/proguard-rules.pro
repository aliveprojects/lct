# kotlinx.serialization: модели читаются из JSON по именам
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class app.finni.kids.** { *** Companion; }
-keepclasseswithmembers class app.finni.kids.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class com.caverock.androidsvg.** { *; }
