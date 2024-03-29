plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {

    namespace = "com.gardilily.onedottongji"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gardilily.onedottongji"
        minSdk = 29
        targetSdk = 34
        versionCode = 62
        versionName = "3.0.15"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }

}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Material Design 3
    implementation("androidx.compose.material3:material3:1.1.1")

    // When using a MDC theme
    implementation("com.google.android.material:compose-theme-adapter:1.2.1")

    // When using a AppCompat theme
    implementation("com.google.accompanist:accompanist-appcompat-theme:0.28.0")

    implementation("androidx.activity:activity-compose:1.7.2")

    // https://mvnrepository.com/artifact/androidx.compose.ui/ui
    implementation("androidx.compose.ui:ui:1.4.3")

    implementation("com.caverock:androidsvg-aar:1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
