# Service Loader for Android

This project is used to optimize [Java Service Provider Interface](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) on Android.

## Getting Started

The transformer depends [Booster Framework](https://github.com/didi/booster), so, the [booster-gradle-plugin](https://github.com/didi/booster/tree/master/booster-gradle-plugin) should be contained in classpath of buildscript:

```groovy
buildscript {
    ext.kotlin_version = "1.3.50"
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"

        classpath("com.didiglobal.booster:booster-gradle-plugin:1.3.1")

        classpath("io.johnsonlee.spi:booster-transform-service-loader:1.0.0")
    }
}
```

Then apply booster gradle plugin:

```groovy
apply plugin: "com.android.application"
apply plugin: "kotlin-android"
apply plugin: "kotlin-android-extensions"
apply plugin: 'kotlin-kapt'
apply plugin: "com.didiglobal.booster"
```

## Example

Here is the [example project](https://github.com/johnsonlee/service-loader-android/tree/master/example)

