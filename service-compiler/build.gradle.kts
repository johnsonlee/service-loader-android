plugins {
    kotlin("jvm")
    kotlin("kapt")
}

apply(plugin = "de.marcphilipp.nexus-publish")

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.auto.service:auto-service:1.0-rc6")
    implementation("com.squareup:javapoet:1.12.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
