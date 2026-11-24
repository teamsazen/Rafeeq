import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// The Firebase config lives in src/release (kept out of git, as the Flutter
// app's android/app/google-services.json). It names only the release app, so
// debug builds go without Firebase, as Flutter's do.
googleServices {
    missingGoogleServicesStrategy = com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy.IGNORE
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
        // Above the Flutter app's last release (1.2.23+52); 53 was the first 2.0.0 upload.
        versionCode = 54
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

    // The Flutter app's assets (Quran text, fonts, legal pages…), used where they
    // are rather than copied. The azan is in res/raw for notifications instead.
    sourceSets["main"].assets.directories.add("../../assets")

    // The app has English and Urdu; libraries ship no other languages. The
    // azan, the PNG logo, the SVG wordmark, Google Sans (cut to Latin) and the
    // other apps' icons (from Google Play) have native forms in res.
    androidResources {
        localeFilters += listOf("en", "ur")
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~:<dir>sounds:logo.png:calligraphy.svg:GoogleSans-Regular.ttf:<dir>other_apps"
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
    // Quran recitation, in the background and on the lock screen (docs/native-plan.md).
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    // The phone's position, as Flutter's geolocator: quick indoors, works with approximate permission.
    implementation("com.google.android.gms:play-services-location:21.4.0")
    // Flexible in-app updates from Play, as Flutter's in_app_update.
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
}
