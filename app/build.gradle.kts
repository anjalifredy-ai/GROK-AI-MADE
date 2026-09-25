import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing credentials: CI supplies them as env vars, local builds read
// them from local.properties (gitignored). Env wins so CI never picks up a
// stray local file.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingCredential(envName: String, propName: String): String? =
    System.getenv(envName) ?: localProps.getProperty(propName)

android {
    namespace = "com.ivor.ivormusic"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ivor.ivormusic"
        minSdk = 30
        targetSdk = 36
        versionCode = 26
        versionName = "4.7-pulse"
        manifestPlaceholders["appLabel"] = "@string/app_name"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        localeFilters += listOf(
            "en",
            "pt-rBR",
            "es",
            "fr",
            "de",
            "it",
            "nl",
            "sv",
            "cs",
            "ro",
            "el",
            "ru",
            "uk",
            "pl",
            "tr",
            "ar",
            "he",
            "fa",
            "hi",
            "th",
            "vi",
            "id",
            "ja",
            "ko",
            "zh-rCN",
            "zh-rTW",
        )
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }
    signingConfigs {
        create("release") {
            val ks = file("${project.rootDir}/keystore/ivormusic.jks")
            val pass = signingCredential("KEYSTORE_PASSWORD", "keystore.storePassword")
            val alias = signingCredential("KEY_ALIAS", "keystore.keyAlias")
            val keyPass = signingCredential("KEY_PASSWORD", "keystore.keyPassword")
            // Only wire a custom keystore when all pieces are present and the
            // file looks non-empty. Otherwise release falls back to the debug
            // keystore so personal forks still produce an installable APK.
            if (ks.exists() && ks.length() > 100 && !pass.isNullOrBlank() && !alias.isNullOrBlank()) {
                storeFile = ks
                storePassword = pass
                keyAlias = alias
                keyPassword = keyPass ?: pass
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "Pulse"
        }
        release {
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                // Personal fork / missing secrets: still ship a signed APK.
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

android.defaultConfig.apply {
    buildConfigField("String", "GITHUB_REPO", "\"anjalifredy-ai/GROK-AI-MADE\"")
    buildConfigField("String", "GITHUB_USERNAME", "\"anjalifredy-ai\"")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.mediarouter)
    implementation(libs.play.services.cast)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.palette.ktx)

    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.jaudiotagger)

    testImplementation(libs.junit)
    testImplementation(libs.json.unit.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
