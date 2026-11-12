import java.util.Properties
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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

/**
 * Runs one git command through the configuration-cache-compatible exec provider
 * and returns its trimmed output, or null when there is nothing to trust: git
 * missing from the PATH, no repository (a source archive), a non-zero exit, or
 * empty output. Never throws, so a checkout without history still builds.
 */
fun gitOutput(vararg args: String): String? = runCatching {
    val execution = providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }
    if (execution.result.get().exitValue != 0) {
        null
    } else {
        execution.standardOutput.asText.get().trim()
    }
}.getOrNull()

/**
 * The commit this build was made from, as "abc1234", or "abc1234+" when the
 * working tree had uncommitted or untracked changes, or "unknown".
 *
 * Version numbers cannot tell builds apart - several builds exist per release -
 * while the commit can. Both commands have to succeed: if the tree's state
 * cannot be read, the build does not claim to be clean.
 *
 * Known limitation: the "+" does not cover the JDK. It comes from the
 * git-ignored gradle/gradle-daemon-jvm.properties, or from the environment
 * where that file is absent, so builds of one commit made with different JDKs
 * carry the same label and nothing records which JDK made them.
 */
val gitShaForBuild: String = run {
    val sha = gitOutput("rev-parse", "--short=7", "HEAD")
    val status = gitOutput("status", "--porcelain")

    when {
        sha.isNullOrEmpty() -> "unknown"
        status == null -> "unknown"
        status.isEmpty() -> sha
        else -> "$sha+"
    }
}

android {
    namespace = "com.fs.twitchminichat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fs.twitchminichat"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "5.5.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "fcm_register_url", "https://api.ircminichat.party/register_fcm")
        resValue("string", "dex_upload_url", "https://api.ircminichat.party/upload_dex_list")

        /* Read through BuildIdentity, which normalises it before display. */
        buildConfigField("String", "GIT_SHA", "\"$gitShaForBuild\"")
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

            /*
             * The welcome page's update notice, and the daily version check behind it.
             * The download it offers installs this package, so only this flavour asks.
             */
            buildConfigField("boolean", "UPDATE_NOTICE_ENABLED", "true")
        }

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            resValue("string", "app_name", "TwitchMiniChat Dev")
            manifestPlaceholders["authScheme"] = "ircminichatdev"
            buildConfigField("String", "AUTH_SCHEME", "\"ircminichatdev\"")
            buildConfigField("boolean", "REQUEST_EMOTE_SCOPE", "true")

            /*
             * Never: the published APK is the stable package, which installs beside this
             * one instead of updating it. No notice is shown and no check is made.
             */
            buildConfigField("boolean", "UPDATE_NOTICE_ENABLED", "false")
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
    /*
     * The version the code is written against, declared so that the build names it: viewpager2
     * 1.1.0 brings 1.3.1, material 1.13.0 asks for 1.2.1. A declaration is a floor, not a pin:
     * Gradle takes the highest version anything asks for, so a later viewpager2 or material can
     * still move it. What fails when it moves is CheckRecyclerViewVersion, below.
     */
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime)
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

/**
 * The RecyclerView version whose source the chat list's code cites by line number.
 *
 * Deliberately not read from the catalog's `recyclerview`: a bump made there alone must fail
 * [CheckRecyclerViewVersion], so that a new version cannot arrive without someone reading again
 * what cites this one. A deliberate bump changes both, after that reading.
 */
val citedRecyclerViewVersion = "1.3.1"

/**
 * Fails a variant's build when its runtime classpath, the one its APK packs, resolves
 * androidx.recyclerview:recyclerview to any version but [citedRecyclerViewVersion].
 *
 * A check, not a constraint: it reads resolution and changes nothing in it. The declaration in
 * `dependencies` cannot hold the version, being a floor, and `strictly` would hold it by changing
 * resolution: Gradle documents that a strict version on a declared dependency "can downgrade a
 * version", and on Gradle 9.4.1 it did, with the build passing, for a transitive request above it.
 * Runs before each variant's preBuild, so every test, lint and assemble of a variant runs it.
 */
abstract class CheckRecyclerViewVersion : DefaultTask() {
    /** The variant checked, named in the message. */
    @get:Input
    abstract val variantName: Property<String>

    /** The version the code cites. */
    @get:Input
    abstract val expectedVersion: Property<String>

    /** The variant's runtime classpath as resolved, read when the task runs. */
    @get:Internal
    abstract val runtimeClasspath: Property<ResolvedComponentResult>

    @TaskAction
    fun check() {
        val versions = sortedSetOf<String>()
        val seen = HashSet<ResolvedComponentResult>()
        val pending = mutableListOf(runtimeClasspath.get())
        while (pending.isNotEmpty()) {
            val component = pending.removeAt(pending.lastIndex)
            if (!seen.add(component)) continue
            val module = component.moduleVersion
            if (module != null && module.group == "androidx.recyclerview" && module.name == "recyclerview") {
                versions += module.version
            }
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { pending += it.selected }
        }
        val expected = expectedVersion.get()
        if (versions != setOf(expected)) {
            val found = if (versions.isEmpty()) "is not on" else "resolves to ${versions.joinToString()} on"
            val pattern = expected.replace(".", "\\.")
            throw GradleException(
                """
                RecyclerView $found ${variantName.get()}'s runtime classpath, and the code cites $expected.
                The chat list's code cites RecyclerView $expected's source by line number: ChatRowViewBinder's
                KDoc, and the two-slot view cache reasoning in the dev harness's RecyclerProof and in
                docs/manual-plan-5d-recyclerview.md, among others. A different version voids those line
                numbers. Read every citation again against the new version's source, found with
                  git grep -n -E "RecyclerView $pattern|$pattern's|(RecyclerView|RecyclerViewAccessibilityDelegate|LinearLayoutManager|OrientationHelper)\.java"
                and only then change citedRecyclerViewVersion in app/build.gradle.kts.
                """.trimIndent()
            )
        }
        logger.lifecycle("RecyclerView $expected on ${variantName.get()}'s runtime classpath: the version the code cites.")
    }
}

androidComponents {
    onVariants { variant ->
        val check = tasks.register<CheckRecyclerViewVersion>(
            "check${variant.name.replaceFirstChar { it.uppercase() }}RecyclerViewVersion"
        ) {
            group = "verification"
            description = "Fails when ${variant.name}'s runtime classpath resolves RecyclerView to a version the code does not cite."
            variantName.set(variant.name)
            expectedVersion.set(citedRecyclerViewVersion)
            runtimeClasspath.set(variant.runtimeConfiguration.incoming.resolutionResult.rootComponent)
        }
        variant.lifecycleTasks.registerPreBuild(check)
    }
}
