plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {

    namespace = "com.gardilily.onedottongji"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gardilily.onedottongji"
        minSdk = 29
        targetSdk = 36
        versionCode = 67
        versionName = "4.0.0-b3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        kotlinCompilerExtensionVersion = null  // using default one
    }

}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")

    // Material Design 3
    implementation("androidx.compose.material3:material3:1.3.2")

    // When using a MDC theme
    implementation("com.google.android.material:compose-theme-adapter:1.2.1")

    // When using a AppCompat theme
    implementation("com.google.accompanist:accompanist-appcompat-theme:0.28.0")

    implementation("androidx.activity:activity-compose:1.10.1")

    // https://mvnrepository.com/artifact/androidx.compose.ui/ui
    implementation("androidx.compose.ui:ui:1.8.2")

    implementation("com.caverock:androidsvg-aar:1.4")


    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Coil extension for SVG support
    implementation("io.coil-kt:coil-svg:2.6.0")

    implementation("androidx.work:work-runtime-ktx:2.11.0")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
