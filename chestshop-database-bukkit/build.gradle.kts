plugins {
    id("chestshop-database.java-conventions")
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.2.2"
}

version = "1.0-SNAPSHOT"

repositories {
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
    compileOnly(libs.paper)
    compileOnly("com.acrobot.chestshop:chestshop:3.12.2")

    // Internal projects
    implementation(projects.core)
    implementation(projects.adapters.worldedit) {
        isTransitive = false
    }
    implementation(projects.adapters.fawe) {
        isTransitive = false
    }
    implementation(projects.adapters.worldguard) {
        isTransitive = false
    }

    // Libraries
    implementation("org.mybatis:mybatis:3.5.19")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.6")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.11.3")
}


val targetJavaVersion = 21

java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

tasks {
    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }
    shadowJar {
        val base = "io.github.md5sha256.chestshopdatabase.libraries"
        relocate("org.mariadb", "${base}.org.mariadb")
        relocate("org.mybatis", "${base}.org.mybatis")
        relocate("org.spongepowered", "${base}.org.spongepowered")
        relocate("org.yaml", "${base}.org.yaml")
        relocate("io.leangen.geantyref", "${base}.io.leangen.geantyref")
        relocate("com.github.stefvanschie", "${base}.com.github.stefvanschie")
        relocate("org.apache.ibatis", "${base}.org.apache.ibatis")
        relocate("org.jetbrains.annotations", "${base}.org.jetbrains.annotations")
        relocate("org.intellij.lang", "${base}.org.intellij.lang")
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

