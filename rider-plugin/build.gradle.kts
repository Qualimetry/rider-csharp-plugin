plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
}

dependencies {
    // Bundled into the plugin distribution. Used for the small SonarQube JSON parse.
    implementation("com.google.code.gson:gson:2.10.1")
}

// IntelliJ Platform Gradle plugin (1.x). type = RD targets Rider; the Rider SDK
// is downloaded from the JetBrains download servers on first build.
intellij {
    type.set("RD")
    version.set(providers.gradleProperty("riderVersion").get())
    plugins.set(emptyList<String>())
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild").get())
        untilBuild.set(providers.gradleProperty("pluginUntilBuild").get())
    }

    // Rider is a non-Java IDE; searchable options indexing is unnecessary and slow.
    buildSearchableOptions {
        enabled = false
    }

    runIde {
        // Rider ships its own JBR; no extra args required here.
    }

    signPlugin {
        certificateChain.set(providers.environmentVariable("PLUGIN_SIGNING_CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PLUGIN_SIGNING_KEY"))
        password.set(providers.environmentVariable("PLUGIN_SIGNING_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN"))
    }
}
