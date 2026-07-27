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
    namespace = "com.fs.twitchminichat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fs.twitchminichat"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "5.4.0"
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

    lint {
        disable += "OldTargetApi"
    }

    flavorDimensions += "env"

    productFlavors {
        create("stable") {
            dimension = "env"

            resValue("string", "app_name", "TwitchMiniChat")
            manifestPlaceholders["authScheme"] = "ircminichat"
            buildConfigField("String", "AUTH_SCHEME", "\"ircminichat\"")
            buildConfigField("boolean", "REQUEST_EMOTE_SCOPE", "true")

            buildConfigField("String", "FCM_REGISTER_URL", "\"https://api.ircminichat.party/register_fcm\"")
        }

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            resValue("string", "app_name", "TwitchMiniChat Dev")
            manifestPlaceholders["authScheme"] = "ircminichatdev"
            buildConfigField("String", "AUTH_SCHEME", "\"ircminichatdev\"")
            buildConfigField("boolean", "REQUEST_EMOTE_SCOPE", "true")
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
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(localProps.getProperty("RELEASE_STORE_FILE"))
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false

            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }

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
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
