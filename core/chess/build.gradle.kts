plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Perft profond (plusieurs minutes) : exclu par défaut, lancé avec `gradlew :core:chess:test -PslowTests`.
tasks.test {
    useJUnitPlatform {
        if (!project.hasProperty("slowTests")) excludeTags("slow")
    }
}
