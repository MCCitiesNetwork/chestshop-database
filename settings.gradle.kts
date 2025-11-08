rootProject.name = "chestshop-database"

// Adapters
include(":adapters:worldedit")
include(":adapters:fawe")
include(":adapters:worldguard")

// Core
include(":core")

// Main project
include(":chestshop-database-bukkit")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")