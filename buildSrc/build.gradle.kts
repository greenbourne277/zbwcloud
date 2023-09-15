plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

allprojects {
    repositories {
        maven { setUrl("https://jitpack.io") }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:11.0.0")
    implementation("com.parmet:buf-gradle-plugin:0.8.2")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.1")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.8.0")
    implementation("com.github.node-gradle:gradle-node-plugin:3.2.1")
    implementation("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.3.1")
    implementation("org.openapitools:openapi-generator-gradle-plugin:6.2.1")
    implementation(gradleApi())
    implementation(localGroovy())
}
