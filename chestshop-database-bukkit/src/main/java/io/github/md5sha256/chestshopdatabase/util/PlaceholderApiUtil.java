package io.github.md5sha256.chestshopdatabase.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves PlaceholderAPI placeholders in text when PlaceholderAPI is present.
 * Uses MiniMessage serialize/deserialize to preserve formatting from {@code settings.yml} templates.
 */
public final class PlaceholderApiUtil {

    private PlaceholderApiUtil() {
    }

    public static @NotNull Component withViewerPlaceholders(@Nullable Player viewer,
                                                            @NotNull Component component) {
        if (viewer == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return component;
        }
        String serialized = MiniMessage.miniMessage().serialize(component);
        String replaced = PlaceholderAPI.setPlaceholders(viewer, serialized);
        return MiniMessage.miniMessage().deserialize(replaced);
    }
}
