import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildKonfig)
}

kotlin {
    android {
        namespace = "com.yannickpulver.gridline.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain = getByName("desktopMain")

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.peekaboo.image.picker)
            implementation(libs.androidx.exifinterface)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.reorderable)
            implementation(libs.tabler.icons)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime)

            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            implementation(libs.koin.compose)

            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.serialization)
            implementation(libs.kermit)

            implementation(libs.coil)
            implementation(libs.coil.compose)
            implementation(libs.coil.network)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.mpfilepicker)
            implementation(libs.jna)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.peekaboo.image.picker)
        }
    }

    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val isAppStore = project.property("appStore").toString().toBoolean()
val signingIdentity = "Yannick Pulver"

// Sparkle updates the DMG from GitHub releases, the App Store build is updated by the store
val sparkleVersion = "2.10.0"
val sparkleDir = layout.buildDirectory.dir("sparkle").get().asFile

compose.desktop {
    application {
        mainClass = "com.yannickpulver.gridline.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg)
            packageName = "Gridline"
            packageVersion = rootProject.file("VERSION").readText().trim()
            includeAllModules = true
            macOS {
                bundleID = "com.yannickpulver.gridline"
                // App Store Connect rejects a build number it has seen before, so every build gets a new one
                packageBuildVersion = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmm"))
                appStore = isAppStore
                iconFile.set(project.file("icon.icns"))
                minimumSystemVersion = "12.0"
                signing {
                    sign.set(true)
                    // This will have to match the name of the certificate issuer -> If you have multiple, remove all but one.
                    identity.set(signingIdentity)
                }

                if (isAppStore) {
                    entitlementsFile.set(project.file("entitlements.plist"))
                    runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))
                    provisioningProfile.set(project.file("embedded.provisionprofile"))
                    runtimeProvisioningProfile.set(project.file("runtime.provisionprofile"))
                } else {
                    entitlementsFile.set(project.file("default-entitlements.plist"))
                    infoPlist {
                        extraKeysRawXml = """
                            <key>SUFeedURL</key>
                            <string>https://github.com/yannickpulver/gridline/releases/latest/download/appcast.xml</string>
                            <key>SUPublicEDKey</key>
                            <string>ekVBw5FwFftt8aOjUHK9IW7cn9IrJ0ezoqN1TPQuZQw=</string>
                            <key>SUEnableAutomaticChecks</key>
                            <true/>
                        """.trimIndent()
                    }
                }
            }
        }

        buildTypes.release.proguard {
            isEnabled = false
        }
    }
}

// Also provides bin/generate_appcast for the release pipeline
val downloadSparkle by tasks.registering(Exec::class) {
    inputs.property("version", sparkleVersion)
    outputs.dir(sparkleDir)
    commandLine(
        "sh", "-c", """
            set -e
            rm -rf "$sparkleDir" && mkdir -p "$sparkleDir" && cd "$sparkleDir"
            curl -fsSL -o Sparkle.tar.xz https://github.com/sparkle-project/Sparkle/releases/download/$sparkleVersion/Sparkle-$sparkleVersion.tar.xz
            echo "c2bf58aa8387266ac179357b1415d6f2635f044da8be41042af32425dae6da0c  Sparkle.tar.xz" | shasum -a 256 -c -
            tar -xf Sparkle.tar.xz
        """.trimIndent()
    )
}

val bundleSparkle by tasks.registering(Exec::class) {
    dependsOn(downloadSparkle)
    val app = layout.buildDirectory.file("compose/binaries/main-release/app/Gridline.app").get().asFile
    commandLine(
        "sh", file("bundle-sparkle.sh"), app, sparkleDir.resolve("Sparkle.framework"),
        "Developer ID Application: $signingIdentity", file("default-entitlements.plist"),
    )
}

if (!isAppStore) {
    tasks.matching { it.name == "createReleaseDistributable" }.configureEach { finalizedBy(bundleSparkle) }
    tasks.matching { it.name == "packageReleaseDmg" }.configureEach { dependsOn(bundleSparkle) }
}

buildkonfig {
    packageName = "com.yannickpulver.gridline"
    defaultConfigs {
        // prod
        buildConfigField(
            FieldSpec.Type.STRING,
            "SUPABASE_KEY",
            gradleLocalProperties(rootDir, providers).getOrDefault("supabaseKey", null) as String?
                ?: System.getenv("SUPABASE_KEY") ?: ""
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SUPABASE_URL",
            gradleLocalProperties(rootDir, providers).getOrDefault("supabaseUrl", null) as String?
                ?: System.getenv("SUPABASE_URL") ?: ""
        )
    }
}
