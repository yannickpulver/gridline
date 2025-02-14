import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildKonfig)
    id("kotlin-parcelize")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.appcompat)

            implementation(libs.koin.androidx.compose)
            implementation(libs.koin.android)

            implementation(libs.peekaboo.image.picker)

        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)

            implementation(libs.compose.reorderable)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.decompose)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.serialization)
            implementation(libs.kermit)

            implementation(libs.kotlinx.datetime)
            implementation(libs.ksoup)
            implementation(libs.ksoup.network)

            implementation(libs.decompose.extensions.compose)

            implementation(libs.mpfilepicker)

            implementation(libs.coil)
            implementation(libs.coil.compose)
            implementation(libs.coil.network)


        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.java)
            //implementation(libs.ktor.util)
            implementation(libs.kotlinx.coroutines.swing)
            implementation("com.bybutter.compose:compose-jetbrains-expui-theme:2.2.0")
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


android {
    namespace = "com.yannickpulver.gridline"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.yannickpulver.gridline"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 14
        versionName = rootProject.file("VERSION").readText().trim()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = gradleLocalProperties(rootDir, providers).getOrDefault(
                "signingKey",
                null
            ) as String? ?: System.getenv("ANDROID_SIGNING_KEY") ?: ""
            keyAlias = gradleLocalProperties(rootDir, providers).getOrDefault(
                "signingAlias",
                null
            ) as String? ?: System.getenv("ANDROID_SIGNING_ALIAS") ?: ""
            keyPassword = gradleLocalProperties(rootDir, providers).getOrDefault(
                "signingKey",
                null
            ) as String? ?: System.getenv("ANDROID_SIGNING_KEY") ?: ""

        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.yannickpulver.gridline.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg)
            packageName = "Gridline"
            packageVersion = rootProject.file("VERSION").readText().trim()
            includeAllModules = true
            macOS {
                appStore = project.property("appStore").toString().toBoolean()
                iconFile.set(project.file("icon.icns"))
                bundleID = "com.yannickpulver.gridline"
                signing {
                    sign.set(true)
                    // This will have to match the name of the certificate issuer -> If you have multiple, remove all but one.
                    identity.set("Yannick Pulver")
                }

                if (appStore) {
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

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs(
            "--add-opens",
            "java.desktop/java.awt.peer=ALL-UNNAMED"
        ) // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
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

