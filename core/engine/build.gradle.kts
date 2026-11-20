plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.chesscompanion.core.engine"
    compileSdk = 37
    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // S2-2 benchmark (several minutes): only with `-PengineBenchmark`, and then alone.
        val benchmark = "com.chesscompanion.core.engine.StockfishBenchmark"
        if (project.hasProperty("engineBenchmark")) {
            testInstrumentationRunnerArguments["class"] = benchmark
        } else {
            testInstrumentationRunnerArguments["notClass"] = benchmark
        }
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// S2-2 benchmark positions, shared with the PC bench (`station-pc/bench/common.py`).
androidComponents {
    onVariants { variant ->
        variant.deviceTests.values.forEach { it.sources.assets?.addStaticSourceDirectory("../../testdata/positions") }
    }
}

dependencies {
    implementation(project(":core:chess"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.onnxruntime.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    // JVM unit tests run the Maia ONNX models with desktop ONNX Runtime (same Java API as Android).
    testImplementation(libs.onnxruntime.jvm)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Real Stockfish on the tablet: `gradlew :core:engine:connectedDebugAndroidTest`.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

// The Android AAR's native libraries cannot load on the JVM: unit tests use the desktop artifact.
configurations.matching { it.name.contains("UnitTest") }.configureEach {
    exclude(group = "com.microsoft.onnxruntime", module = "onnxruntime-android")
}
