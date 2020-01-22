plugins {
    kotlin("jvm")
    kotlin("kapt")
}

apply(plugin = "de.marcphilipp.nexus-publish")

dependencies {
    implementation(gradleApi())

    kapt("com.google.auto.service:auto-service:1.0-rc6")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(project(":service-registry"))
    implementation("com.didiglobal.booster:booster-android-gradle-api:1.3.0")
    implementation("com.didiglobal.booster:booster-task-spi:1.3.0")
    implementation("com.didiglobal.booster:booster-transform-asm:1.3.0")
    implementation("com.didiglobal.booster:booster-transform-util:1.3.0")
    compileOnly("com.android.tools.build:gradle:3.0.0")
    testCompileOnly("com.android.tools.build:gradle:3.0.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
