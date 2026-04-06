import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

val historyKey: String = localProps.getProperty("historySecretKey", "")

val hasReleaseSigning =
    !localProps.getProperty("RELEASE_STORE_FILE").isNullOrBlank() &&
            !localProps.getProperty("RELEASE_STORE_PASSWORD").isNullOrBlank() &&
            !localProps.getProperty("RELEASE_KEY_ALIAS").isNullOrBlank() &&
            !localProps.getProperty("RELEASE_KEY_PASSWORD").isNullOrBlank()

android {
    namespace = "com.fs.twitchminichat.v2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fs.twitchminichat.v2gecko"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "HISTORY_SECRET_KEY", "\"$historyKey\"")
        resValue("string", "fcm_register_url", "https://api.ircminichat.party/register_fcm")
        resValue("string", "dex_upload_url", "https://api.ircminichat.party/upload_dex_list")
    }

    buildFeatures {
        compose = false
        buildConfig = true
        resValues = true
    }

    flavorDimensions += "env"

    productFlavors {
        create("stable") {
            dimension = "env"

            resValue("string", "app_name", "TwitchMiniChat")
            manifestPlaceholders["authScheme"] = "ircminichat"
            buildConfigField("String", "AUTH_SCHEME", "\"ircminichat\"")

            buildConfigField("String", "FCM_REGISTER_URL", "\"https://api.ircminichat.party/register_fcm\"")
        }

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            resValue("string", "app_name", "TwitchMiniChat Dev")
            manifestPlaceholders["authScheme"] = "ircminichatdev"
            buildConfigField("String", "AUTH_SCHEME", "\"ircminichatdev\"")
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

            signingConfig = signingConfigs.findByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.glide)
    implementation(libs.geckoview)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}