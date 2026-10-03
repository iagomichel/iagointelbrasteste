plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.iagointelbras.infrastructure"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":base"))
    implementation(project(":common"))
}
