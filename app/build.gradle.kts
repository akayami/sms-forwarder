plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.akayami.smsforwarder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.akayami.smsforwarder"
        minSdk = 24
        targetSdk = 34
        // versionCode/versionName come from the release tag in CI
        // (see .github/workflows/release.yml). The env fallbacks below
        // keep local/debug builds working when the vars are unset.
        versionCode = (System.getenv("SMS_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("SMS_VERSION_NAME") ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release signing is driven entirely by env vars so no secrets
        // live in the repo. CI (release.yml) materialises the keystore
        // from the SMS_KEYSTORE_B64 secret and exports these. When the
        // vars are absent (local debug), the block is skipped and the
        // release buildType falls back to unsigned.
        create("release") {
            val ksFile = System.getenv("SMS_KEYSTORE_FILE")
            if (ksFile != null) {
                storeFile = file(ksFile)
                storePassword = System.getenv("SMS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SMS_KEY_ALIAS")
                keyPassword = System.getenv("SMS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only attach the signing config when a keystore was provided,
            // otherwise Gradle errors on the empty config during local builds.
            if (System.getenv("SMS_KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.code.gson:gson:2.10.1")
}
