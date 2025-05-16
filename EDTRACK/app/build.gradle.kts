
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

//def apikeyPropertiesFile = rootProject.file("apikey.properties")
//def apikeyProperties = new Properties()
//apikeyProperties.load(new FileInputStream(apikeyPropertiesFile))

android {
    namespace = "org.ed.track"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.ed.track"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AGORA_APP_ID", "\"${project.properties["AGORA_APP_ID"]}\"")
    }
    android.buildFeatures.buildConfig = true

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(libs.firebase.analytics)
    implementation (libs.firebase.auth)

    implementation (libs.ccp)
    implementation (libs.firebase.firestore)
    implementation (libs.firebase.storage)
    implementation (libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation (libs.okhttp)
    implementation (libs.okio)


    implementation (libs.gson)
    implementation (libs.core.splashscreen)

    implementation (libs.circleimageview)
    implementation (libs.glide)

    // agora sdk
    implementation(libs.full.sdk)

    //stripe
    implementation (libs.stripe.android)

    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
}