import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val historyKey: String = localProps.getProperty("historySecretKey", "")

android {
    namespace = "com.fs.twitchminichat.v2"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.fs.twitchminichat.v2gecko"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "HISTORY_SECRET_KEY", "\"$historyKey\"")
    }


    buildFeatures {
        compose = false
        buildConfig = true
    }
    flavorDimensions += "env"

    productFlavors {
        create("stable") {
            dimension = "env"
            // Nome app
            resValue("string", "app_name", "TwitchMiniChat")

            // Deep link scheme per callback -> app
            manifestPlaceholders["authScheme"] = "twitchminichat"

            // Redirect usato nel login Twitch
            buildConfigField("String", "TWITCH_CLIENT_ID", "\"7tvgt6i65b58k3e8lhxxv1p0b2vrib\"")
            buildConfigField(
                "String",
                "TWITCH_REDIRECT_URI",
                "\"https://unouidol.github.io/ircminichat/callback.html\""
            )
        }

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            resValue("string", "app_name", "TwitchMiniChat Dev")

            manifestPlaceholders["authScheme"] = "twitchminichatdev"
            buildConfigField("String", "TWITCH_CLIENT_ID", "\"7tvgt6i65b58k3e8lhxxv1p0b2vrib\"")
            buildConfigField(
                "String",
                "TWITCH_REDIRECT_URI",
                "\"https://unouidol.github.io/ircminichat/callback_dev.html\""
            )
        }
    }


    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // (opzionale) debug { ... }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

}
dependencies { implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.github.bumptech.glide:glide:4.16.0") // Source: https://mvnrepository.com/artifact/org.mozilla.geckoview/geckoview
    implementation("org.mozilla.geckoview:geckoview:145.0.20251124145406")
}
