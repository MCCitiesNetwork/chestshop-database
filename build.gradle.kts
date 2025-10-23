plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.2.2"
}

group = "io.github.md5sha256"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "minebench-repo"
        url = uri("https://repo.minebench.de/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.acrobot.chestshop:chestshop:3.12.2")
    implementation("org.mybatis:mybatis:3.5.19")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.6")
}


val targetJavaVersion = 21

java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

tasks {
    compileJava {
        options.release.set(targetJavaVersion)
    }
    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }
    shadowJar {
        val base = "io.github.md5sha256.chestshopdatabase.libraries"
        relocate("org.mariadb", "${base}.org.mariadb")
        relocate("org.mybatis", "${base}.org.mybatis")
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin"s jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.8")
        downloadPlugins {
            // Chestshop #445
            url("https://ci.minebench.de/job/ChestShop-3/445/artifact/target/ChestShop.jar")
            // EssX
            url("https://ci.ender.zone/job/EssentialsX/1735/artifact/jars/EssentialsX-2.22.0-dev+40-150dabb.jar")
            // Vault
            url("https://mediafilez.forgecdn.net/files/3007/470/Vault.jar")
        }
    }
}

