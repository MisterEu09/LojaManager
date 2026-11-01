// build.gradle.kts (Módulo: app)
plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
id("com.google.gms.google-services") // Plugin para Firebase
id("org.jetbrains.kotlin.plugin.parcelize") // Se você usa Parcelize
id("org.jetbrains.kotlin.kapt") // Necessário para o Glide compiler e Room annotationProcessor
id("com.google.firebase.crashlytics") // Plugin para Firebase Crashlytics
}

android {
namespace = "com.example.lojamamager"
compileSdk = 36 // Recomendo usar 34 para maior estabilidade

defaultConfig {
applicationId = "com.example.lojamamager"
minSdk = 24
targetSdk = 34 // Alinhe com compileSdk
versionCode = 1
versionName = "1.0"
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}

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
kotlinOptions {
jvmTarget = "11"
languageVersion = "1.9"
apiVersion = "1.9"
}
buildFeatures {
viewBinding = true
}
}

dependencies {
implementation(platform(libs.firebase.bom))

implementation(libs.firebase.analytics)
implementation(libs.firebase.auth.ktx)
implementation(libs.firebase.firestore.ktx)
implementation(libs.firebase.storage.ktx)
implementation(libs.firebase.crashlytics)

implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.coroutines.play.services)

implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.lifecycle.livedata.ktx)
implementation(libs.androidx.lifecycle.viewmodel.ktx)

implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)
implementation(libs.androidx.activity)
implementation(libs.androidx.constraintlayout)
implementation(libs.androidx.cardview)
implementation(libs.androidx.drawerlayout)
implementation(libs.androidx.recyclerview)
implementation(libs.androidx.annotation)
implementation ("com.google.android.material:material:1.12.0")
implementation(libs.github.glide)
kapt(libs.glide.compiler)
implementation(libs.circleimageview)
implementation(libs.flexbox)


implementation(libs.retrofit)
implementation(libs.converter.gson)
implementation(libs.retrofit2.kotlin.coroutines.adapter)
implementation(libs.logging.interceptor)

implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
kapt(libs.androidx.room.compiler)
testImplementation(libs.junit)
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)

implementation (libs.opencsv)
implementation (libs.androidx.core.ktx)

}
