plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    // Public plugin API — plugins compile against this, not the full IDE.
    // MUST NOT depend on anything from the IDE internals.
    namespace = "com.nullij.androidcodestudio.plugins.api"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true 
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            // Enforce opt-in for the @InternalPluginApi annotation at compile time.
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

// Only coroutines — no IDE, no reflection, no accessor classes.
dependencies {
    implementation("androidx.compose.runtime:runtime:1.10.6")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId   = "com.github.nullij"
                artifactId = "ACSPluginApi"
                version   = "1.0.0"
            }
        }
    }
}
1