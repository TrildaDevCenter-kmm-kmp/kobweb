plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("kobweb-compose")
    id("com.varabyte.kobweb.internal.publish")
}

group = "com.varabyte.kobweb"
version = libs.versions.kobweb.get()

kotlin {
    js {
        browser()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kotlinx.coroutines)
        }
    }
}

kobwebPublication {
    artifactName.set("Kobweb Custom Renderer")
    artifactId.set("kobweb-custom-renderer")
    description.set("A custom implementation of renderComposable")
}
