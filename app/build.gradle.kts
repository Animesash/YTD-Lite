import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Настройки подписи.
 *
 * В репозитории лежит постоянный ключ `keystore/ytdlite-keystore.p12`, поэтому
 * локальные сборки и сборки из CI подписаны одинаково — новые APK ставятся
 * поверх старых без ошибки «конфликтует с другим пакетом».
 *
 * Чтобы использовать собственный ключ, создайте в корне файл keystore.properties:
 *   storeFile=keystore/my-release.jks
 *   storeType=JKS
 *   storePassword=...
 *   keyAlias=...
 *   keyPassword=...
 * (файл добавлен в .gitignore и в репозиторий не попадает)
 */
/*
 * Оптимизация размера APK.
 *
 * Основной вес сборки — нативные библиотеки yt-dlp:
 *   libffmpeg.zip.so ~33 МБ, libpython.zip.so ~14 МБ.
 * ffmpeg нужен только для склейки отдельных видео- и аудиодорожек (1080p+)
 * и извлечения аудио. Без него APK ~24 МБ вместо ~57 МБ, а yt-dlp качает
 * готовые прогрессивные файлы (до 720p).
 *
 * Включить ffmpeg обратно:  ./gradlew assembleRelease -PwithFfmpeg=true
 * (или строка `withFfmpeg=true` в gradle.properties)
 */
val withFfmpeg: Boolean =
    (project.findProperty("withFfmpeg") ?: "false").toString().toBoolean()

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.ytdlite.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ytdlite.app"
        minSdk = 26 // Android 8.0
        targetSdk = 34
        versionCode = 1
        versionName = "v0.1.0-alpha"

        // Только наши языки: переводы androidx/material на остальные ~80 языков
        // в APK не попадают (минус сотни килобайт в resources.arsc и res/).
        resourceConfigurations += listOf("en", "ru", "uk", "be", "ro")

        // Движок смотрит на этот флаг: инициализировать ли ffmpeg и какой
        // селектор формата использовать по умолчанию.
        buildConfigField("boolean", "HAS_FFMPEG", withFfmpeg.toString())
    }

    // Только 64-битный ARM (Android 8+): один APK — app-arm64-v8a-release.apk,
    // в него попадает единственный комплект Python/ffmpeg.
    // Важно: ABI задаётся здесь, а не через ndk.abiFilters — они конфликтуют.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    // yt-dlp запускается как процесс, поэтому нативные библиотеки
    // должны лежать распакованными (extractNativeLibs).
    packaging {
        // Служебные файлы библиотек в APK не нужны: лицензии, метаданные
        // Kotlin, отладочные пробы корутин и версии для JDK 9+.
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
                "DebugProbesKt.bin",
                "kotlin/**"
            )
        }

        jniLibs {
            useLegacyPackaging = true
            if (!withFfmpeg) {
                // Классы ffmpeg остаются (код компилируется), но нативные
                // библиотеки в APK не попадают: 33 МБ архива ffmpeg и его
                // JNI-обвязка. Без них yt-dlp качает готовые прогрессивные файлы.
                excludes += "**/libffmpeg.zip.so"
                excludes += "**/libffmpeg.so"
                excludes += "**/libffprobe.so"
            }
        }
    }

    signingConfigs {
        create("ytdlite") {
            storeFile = rootProject.file(
                keystoreProperties.getProperty("storeFile") ?: "keystore/ytdlite-keystore.p12"
            )
            storeType = keystoreProperties.getProperty("storeType") ?: "PKCS12"
            storePassword = keystoreProperties.getProperty("storePassword") ?: "ytdlite2026"
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: "ytdlite"
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: "ytdlite2026"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            // Та же подпись, что и у release, — APK взаимозаменяемы.
            signingConfig = signingConfigs.getByName("ytdlite")
        }
        release {
            // R8: удаляет неиспользуемый код (Kotlin, androidx, material, jackson)
            // и переименовывает всё в короткие имена.
            isMinifyEnabled = true
            // Убирает ресурсы, на которые никто не ссылается.
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("ytdlite")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        // Сборка APK не должна зависеть от lint: он гоняется отдельно.
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.14.0") // Material 3 Expressive
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // yt-dlp внутри приложения: Python + yt-dlp + ffmpeg.
    // https://github.com/yt-dlp/yt-dlp (stable)
    // https://github.com/yt-dlp/yt-dlp-nightly-builds (nightly)
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
}
