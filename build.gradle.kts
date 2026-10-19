plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Robolectric ne charge pas sa bibliothèque native depuis un jar dont le chemin contient une espace
// (dossier personnel Windows « C:\Users\Prénom Nom »). `robolectric.userHome` dans local.properties
// (propre à la machine, non versionné) déplace le user.home de la JVM de test, donc son cache Maven.
val robolectricUserHome: String? =
    rootProject.file("local.properties").takeIf { it.exists() }?.let { file ->
        java.util.Properties().apply { file.inputStream().use { load(it) } }.getProperty("robolectric.userHome")
    }

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "dev.detekt")

    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        parallel.set(true)
    }

    // JUnit 5 sur tous les modules (Kotlin pur et bibliothèques Android).
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        robolectricUserHome?.let { systemProperty("user.home", it) }
    }
}
