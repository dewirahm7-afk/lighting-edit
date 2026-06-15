plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.versions)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    // alias(libs.plugins.dependency.analysis)
    // alias(libs.plugins.sort.dependencies)
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionName = "5.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    val isCi = System.getenv("CI") == "true"

    sourceSets {
        create("lightningPlus").apply {
            setRoot("src/LightningPlus")
        }
        if (!isCi) {
            create("lightningLite").apply {
                setRoot("src/LightningLite")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    buildTypes {
        named("debug") {
            multiDexEnabled = true
            isMinifyEnabled = false
            isShrinkResources = false
            setProguardFiles(listOf("proguard-project.txt"))
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false
        }

        named("release") {
            multiDexEnabled = false
            isMinifyEnabled = !isCi
            isShrinkResources = !isCi
            setProguardFiles(listOf("proguard-project.txt"))
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false

            ndk {
                abiFilters.add("arm64-v8a")
                abiFilters.add("armeabi-v7a")
                abiFilters.add("armeabi")
                abiFilters.add("mips")
            }
        }
    }

    flavorDimensions.add("capabilities")

    productFlavors {
        create("lightningPlus") {
            dimension = "capabilities"
            buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"true\")")
            applicationId = "acr.browser.lightning"
            versionCode = 101
        }

        if (!isCi) {
            create("lightningLite") {
                dimension = "capabilities"
                buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"false\")")
                applicationId = "acr.browser.barebones"
                versionCode = 102
            }
        }
    }
    packaging {
        resources {
            excludes += listOf(".readme")
        }
    }
    lint {
        abortOnError = true
    }
    namespace = "acr.browser.lightning"
}

dependencies {
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
    }
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.annotation:annotation:1.9.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.palette:palette:1.0.0")
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.webkit)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)
    implementation(libs.google.material)
    implementation(libs.dagger)
    implementation("com.guolindev.permissionx:permissionx:1.8.1")
    implementation(libs.okhttp)
    implementation(libs.okio)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation("javax.inject:javax.inject:1")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation("org.jspecify:jspecify:1.0.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.leakcanary)

    compileOnly("javax.annotation:jsr250-api:1.0")

    testImplementation(libs.mockito.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)

    ksp(libs.dagger.compiler)
}

// mezzanine {
//    files = files(
//        "src/main/html/list.html",
//        "src/main/html/bookmarks.html",
//        "src/main/html/homepage.html",
//        "src/main/js/InvertPage.js",
//        "src/main/js/TextReflow.js",
//        "src/main/js/ThemeColor.js"
//    )
// }

kotlin {
    jvmToolchain(21)
}
