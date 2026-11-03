// Корневой build-файл проекта YTD-Lite.
// Версии плагинов берутся из gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
