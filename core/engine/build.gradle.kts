plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.chesscompanion.core.engine"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:chess"))
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
