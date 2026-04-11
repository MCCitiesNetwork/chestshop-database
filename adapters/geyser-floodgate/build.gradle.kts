plugins {
    id("chestshop-database.library-conventions")
}

repositories {
    maven {
        name = "opencollab"
        url = uri("https://repo.opencollab.dev/main/")
    }
}

dependencies {
    compileOnlyApi(libs.geyserApi)
    compileOnlyApi(libs.floodgateApi)
}
