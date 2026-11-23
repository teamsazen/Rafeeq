import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The Play upload key, shared with the Flutter app (android/app/build.gradle).
// Without it, release builds fall back to the debug key.
val keystoreProperties = Properties().apply {
    val file = File(System.getProperty("user.home"), "Muzamil/Keystores/rafeeq.txt")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.satechdev.rafeeqapp"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        // The same Play app as the Flutter build, so it updates in place.
        applicationId = "com.satechdev.rafeeqapp"
        minSdk = 24
        targetSdk = 36
        // Above the Flutter app's last release (1.2.23+52).
        versionCode = 53
        versionName = "2.0.0"
        manifestPlaceholders["appName"] = "Rafeeq"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            // Same ID as the Flutter debug build: installs over it and must
            // keep its settings, which is how migration is tested.
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "R-Debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        // java.time on Android 7 (minSdk 24), as the Flutter app had.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/*.version",
            "META-INF/**/*.kotlin_builtins",
            "kotlin/**",
            "DebugProbesKt.bin",
            "META-INF/com.android.tools/**",
        )
    }

    // The Flutter app's fonts, used where they are rather than copied (see AppFonts).
    sourceSets["main"].assets.directories.add("../../assets/fonts")

    // The app has English and Urdu; libraries ship no other languages.
    androidResources {
        localeFilters += listOf("en", "ur")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Compose without Material: the app draws its own controls, like 99 Names.
    val compose = platform("androidx.compose:compose-bom:2026.11.01")
    implementation(compose)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
}
