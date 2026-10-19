plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hololingo.core.recognition"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.mlkit.digital.ink)
    testImplementation(libs.junit)
}
