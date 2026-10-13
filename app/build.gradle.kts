import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.serialization)
    alias(libs.plugins.google.services) apply false
}

// AI recipe import needs a Firebase project; the google-services plugin only runs
// when the (git-ignored) config file is present, so clones/CI without it still build.
val aiImportEnabled = file("google-services.json").exists()
if (aiImportEnabled) {
    apply(plugin = "com.google.gms.google-services")
}

// Version lives in version.properties (tracked in git) rather than being hardcoded here. Play consumes
// a versionCode permanently on upload and rejects reuse.
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) versionPropsFile.inputStream().use { load(it) }
}
val appVersionCode = versionProps.getProperty("VERSION_CODE")?.trim()?.toIntOrNull() ?: 1
val appVersionName = versionProps.getProperty("VERSION_NAME")?.trim() ?: "1.0"

// Configure release signing only when all RELEASE_* properties are non-blank and the keystore
// file is a regular file. This lets bundleRelease still build an unsigned bundle on fresh clones/CI
// without failing on missing signing, and it keeps signing from failing at configuration time.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")
// AdMob IDs: debug always uses Google's official test IDs; release reads the real
// ones from the (git-ignored) local.properties and falls back to the test IDs.
val TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val TEST_ADMOB_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
val adMobAppId = localProperties.getProperty("ADMOB_APP_ID")?.trim().orEmpty()
val adMobRewardedUnitId = localProperties.getProperty("ADMOB_REWARDED_UNIT_ID")?.trim().orEmpty()

val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() &&
    !localProperties.getProperty("RELEASE_STORE_PASSWORD").isNullOrBlank() &&
    !localProperties.getProperty("RELEASE_KEY_ALIAS").isNullOrBlank() &&
    !localProperties.getProperty("RELEASE_KEY_PASSWORD").isNullOrBlank() &&
    file(releaseStoreFile).isFile

android {
    namespace = "com.ryxan.recipescaler"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ryxan.recipescaler"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("boolean", "AI_IMPORT_ENABLED", aiImportEnabled.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            manifestPlaceholders["admobAppId"] = TEST_ADMOB_APP_ID
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$TEST_ADMOB_REWARDED_UNIT_ID\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
            val releaseAdMobAppId = adMobAppId.ifBlank {
                logger.warn("ADMOB_APP_ID is not set in local.properties; release uses the AdMob test app ID.")
                TEST_ADMOB_APP_ID
            }
            val releaseAdMobUnitId = adMobRewardedUnitId.ifBlank {
                logger.warn("ADMOB_REWARDED_UNIT_ID is not set in local.properties; release uses the AdMob test rewarded unit ID.")
                TEST_ADMOB_REWARDED_UNIT_ID
            }
            manifestPlaceholders["admobAppId"] = releaseAdMobAppId
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$releaseAdMobUnitId\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Unit tests run on the debug variant only; release unit tests duplicate them and
// would need the debug-only ui-test-manifest shipped in the release manifest.
androidComponents {
    beforeVariants(selector().withBuildType("release")) {
        it.enableUnitTest = false
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.reorderable)
    implementation(libs.mlkit.document.scanner)

    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(composeBom)
    testImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(libs.compose.ui.test.junit4)
}

// Rewrites version.properties with VERSION_CODE + 1. Wired to run only after successful bundleRelease,
// so the bundle that was just built keeps the code it was configured with and the *next* build gets a
// fresh one. Bumping before/during the build would not work: the version is read at configuration time.
val bumpVersionCode = tasks.register("bumpVersionCode") {
    group = "release"
    description = "Increments VERSION_CODE in version.properties so the next release bundle is uploadable."
    val propsFile = versionPropsFile
    val current = appVersionCode
    val name = appVersionName
    doLast {
        val next = current + 1
        propsFile.writeText(
            """
            # Single source of truth for the app version. Read by app/build.gradle.kts.
            # VERSION_CODE is bumped automatically after every successful :app:bundleRelease
            # (see the bumpVersionCode task), so each uploadable bundle gets a fresh, higher code.
            # Play permanently consumes a versionCode once uploaded and never allows reuse.
            # VERSION_NAME is the user-visible string and is bumped by hand per release.
            VERSION_CODE=$next
            VERSION_NAME=$name
            """.trimIndent() + "\n"
        )
        logger.lifecycle("versionCode $current -> $next (next bundleRelease will use $next)")
    }
}

// Only bundleRelease gets version-bumped, and only when it actually produces a bundle. Debug builds and
// assembleRelease leave the counter alone, since neither produces something uploaded to Play.
tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy(bumpVersionCode)
}

bumpVersionCode.configure {
    onlyIf {
        val bundleTask = tasks.findByName("bundleRelease")
        bundleTask != null && bundleTask.state.executed && !bundleTask.state.upToDate && bundleTask.state.failure == null
    }
}
