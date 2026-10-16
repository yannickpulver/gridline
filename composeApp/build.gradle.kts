import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

compose.desktop {
    application {
        mainClass = "com.yannickpulver.gridline.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg)
            packageName = "Gridline"
            packageVersion = rootProject.file("VERSION").readText().trim()
            includeAllModules = true
            val isAppStore = project.property("appStore").toString().toBoolean()
            macOS {
                bundleID = "com.yannickpulver.gridline"
                appStore = isAppStore
                iconFile.set(project.file("icon.icns"))
                minimumSystemVersion = "12.0"
                signing {
                    sign.set(true)
                    // This will have to match the name of the certificate issuer -> If you have multiple, remove all but one.
                    identity.set("Yannick Pulver")
                }

                if (isAppStore) {
                    entitlementsFile.set(project.file("entitlements.plist"))
                    runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))
                    provisioningProfile.set(project.file("embedded.provisionprofile"))
                    runtimeProvisioningProfile.set(project.file("runtime.provisionprofile"))
                } else {
                    entitlementsFile.set(project.file("default-entitlements.plist"))
                }
            }
        }

        buildTypes.release.proguard {
            isEnabled = false
        }
    }
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
