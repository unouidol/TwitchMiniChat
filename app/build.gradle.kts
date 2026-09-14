import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.firebase.crashlytics)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

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
        versionCode = 7
        versionName = "5.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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

        /*
         * These three ask a remote service which versions exist, not whether
         * this repository has a defect. Their findings appear and disappear with
         * the dependency cache, the network and whatever the library authors
         * released that morning, so the warning count stops being a property of
         * the code and becomes a property of the world outside it. Two runs of
         * the same commit can disagree, which makes comparing a branch against a
         * baseline meaningless in exactly the situation where the comparison is
         * wanted.
         *
         * Nothing here is repairable in the tree, so nothing is being hidden.
         * Knowing that a dependency has aged is still worth knowing; it is just
         * a periodic review with a human deciding whether to upgrade, not a
         * finding that should change the result of a build.
         */
        disable += "GradleDependency"
        disable += "NewerVersionAvailable"
        disable += "AndroidGradlePluginVersion"

        /*
         * These two only work together. The baseline freezes the warnings that
         * already exist so a new one stands out instead of being lost in a count
         * nobody reads, but on its own it enforces nothing: abortOnError fails
         * the build on errors, and a new warning is still only a warning.
         * warningsAsErrors is what makes the build stop; the baseline is what
         * keeps it from stopping on the 38 findings that were already here.
         *
         * Regenerate with :app:updateLintBaselineDevDebug after deliberately
         * fixing something, never to make a fresh warning go away.
         */
        baseline = file("lint-baseline.xml")
        warningsAsErrors = true
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
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
