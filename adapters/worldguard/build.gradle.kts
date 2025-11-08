plugins {
    id("chestshop-database.library-conventions")
}

repositories {
    maven {
        name = "enginehub-repo"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    compileOnlyApi(libs.worldguardBukkit)
    compileOnlyApi(projects.core)
}