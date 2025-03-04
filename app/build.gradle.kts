import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("io.nativeblocks.nativeblocks-gradle-plugin").version("1.1.1")
}
val nativeblocksProps = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "sample.nativeblocks.properties")))
}

android {
    namespace = "io.nativeblocks.sampleapp"
    compileSdk = 34
    defaultConfig {
        applicationId = "io.nativeblocks.sampleapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

nativeblocks {
    endpoint = nativeblocksProps["endpoint"] as String
    authToken = nativeblocksProps["authToken"] as String
    organizationId = nativeblocksProps["organizationId"] as String
    basePackageName = "io.nativeblocks.sampleapp"
    moduleName = "Demo"
}

ksp {
    arg("basePackageName", "io.nativeblocks.sampleapp")
    arg("moduleName", "Demo")
}

nativeblocks {
    endpoint = nativeblocksProps["endpoint"] as String
    authToken = nativeblocksProps["authToken"] as String
    organizationId = nativeblocksProps["organizationId"] as String
    basePackageName = "io.nativeblocks.sampleapp"
    moduleName = "Demo"
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material:material:1.7.4")
    implementation("androidx.compose.animation:animation:1.7.4")
    implementation("androidx.compose.ui:ui-tooling:1.7.4")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.nativeblocks:nativeblocks-android:1.3.0")
    implementation("io.nativeblocks:nativeblocks-foundation-android:1.1.0")
    implementation("io.nativeblocks:nativeblocks-wandkit-android:1.0.1")
    implementation(project(":compiler"))
    ksp(project(":compiler"))
}