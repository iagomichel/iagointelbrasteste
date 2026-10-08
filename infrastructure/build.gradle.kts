plugins {
    id("com.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.iagointelbras.infrastructure"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":network"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    implementation(libs.moshi)
    implementation(libs.retrofit)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.retrofit.moshi)
    testImplementation(libs.moshi)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.retrofit.moshi)
    androidTestImplementation(libs.moshi)
}
