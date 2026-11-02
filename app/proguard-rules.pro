# Правила R8 для YTD-Lite.
#
# Внутри — движок yt-dlp (youtubedl-android), который дергает Python через JNI
# и разбирает JSON через Jackson рефлексией. Такие классы нельзя ни удалять,
# ни переименовывать, иначе приложение упадёт уже на телефоне.

# ---------------------------------------------------------------- yt-dlp движок
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# Распаковка архивов yt-dlp/python — обращение по именам классов.
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.**

# ---------------------------------------------------------------- JSON (Jackson)
# ObjectMapper собирает бин-классы рефлексией: без keep-правил поля
# и аксессоры будут вырезаны или переименованы.
-keep class com.fasterxml.jackson.** { *; }
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* <fields>;
    @com.fasterxml.jackson.annotation.* <methods>;
}
-keepclassmembers,allowobfuscation class * {
    @com.fasterxml.jackson.annotation.JsonProperty <fields>;
}
-dontwarn com.fasterxml.jackson.**
-dontwarn jakarta.**
-dontwarn sun.misc.**
-dontwarn java.lang.instrument.**

# ---------------------------------------------------------------- прочее
# Аннотации и generic-подписи нужны Jackson для разбора JSON,
# SourceFile/LineNumberTable — чтобы стектрейсы оставались читаемыми.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# POJO-классы, в которые Jackson раскладывает ответ yt-dlp.
-keep class com.yausername.youtubedl_android.mapper.** { *; }

-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.debug.**
