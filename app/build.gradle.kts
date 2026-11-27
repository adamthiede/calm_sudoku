import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from key.properties locally, or from SIGNING_* env
// variables in CI. key.properties is gitignored and holds the same four values.
val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

val signingEnvNames = listOf(
    "SIGNING_STORE_FILE",
    "SIGNING_STORE_PASSWORD",
    "SIGNING_KEY_ALIAS",
    "SIGNING_KEY_PASSWORD",
)
val signingEnv = signingEnvNames.associateWith { System.getenv(it).orEmpty() }
val hasEnvSigning = signingEnv.values.all { it.isNotBlank() }
check(!signingEnv.values.any { it.isNotBlank() } || hasEnvSigning) {
    "Incomplete release signing environment; set ${signingEnvNames.joinToString(", ")}"
}
val hasKeystoreSigning = keystorePropertiesFile.exists() && !hasEnvSigning

android {
    namespace = "com.cbaier33.sudoku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adamthiede.sudoku"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasEnvSigning) {
            create("release") {
                storeFile = file(signingEnv.getValue("SIGNING_STORE_FILE"))
                storePassword = signingEnv.getValue("SIGNING_STORE_PASSWORD")
                keyAlias = signingEnv.getValue("SIGNING_KEY_ALIAS")
                keyPassword = signingEnv.getValue("SIGNING_KEY_PASSWORD")
            }
        } else if (hasKeystoreSigning) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Without signing material, fall back to the debug keys so that a
            // local `assembleRelease` still produces an installable APK. CI
            // supplies all four SIGNING_* variables and signs with the real key.
            signingConfig = if (hasEnvSigning || hasKeystoreSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        // MMD's own API is built on ExperimentalMaterial3Api - TopAppBarMMD and
        // ModalBottomSheetMMD both expose it - so opt in once here rather than
        // annotating every call site.
        freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Mudita Mindful Design. Brings androidx.compose.material3 along as an `api`
    // dependency, so stock M3 components are available and inherit the theme.
    implementation(libs.mmd)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.core.splashscreen)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
