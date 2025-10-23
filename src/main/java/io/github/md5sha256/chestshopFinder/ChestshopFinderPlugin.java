package io.github.md5sha256.chestshopFinder;

import io.github.md5sha256.chestshopFinder.util.UnsafeChestShopSign;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChestshopFinderPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        UnsafeChestShopSign.init();
        getLogger().info("Plugin enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Plugin disabled");
    }
}
