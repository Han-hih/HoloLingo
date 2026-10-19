plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hololingo.core.data"
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
    testImplementation(libs.junit)
}
