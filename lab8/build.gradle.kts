plugins {
    kotlin("jvm") version "1.5.10"
}

group = "pavlova.alexandra"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("junit:junit:4.13.1")
}