plugins {
    id("com.android.application")
}

android {
    namespace = "com.dotwelwidgets.dotwellabs"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dotwelwidgets.dotwellabs"
        minSdk = 26
        targetSdk = 36
        if(project.hasProperty("repairFixtures")) testInstrumentationRunner = "com.dotwelwidgets.dotwellabs.RepairInstrumentation"
        versionCode = 9
        versionName = "0.1.8"
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    // Test fixtures never enter the normal debug or release artifact.
    if(project.hasProperty("repairFixtures")) {
        sourceSets.getByName("androidTest") {
            kotlin.srcDir("src/repairAndroidTest/java")
        }
        sourceSets.getByName("debug") {
            java.srcDir("src/repairFixtures/java")
            kotlin.srcDir("src/repairFixtures/java")
            manifest.srcFile("src/repairFixtures/AndroidManifest.xml")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18.1")
    testImplementation("org.json:json:20240303")
}
