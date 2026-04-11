package io.github.md5sha256.chestshopdatabase.settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageContainer {

    private final Map<String, Component> messages = new ConcurrentHashMap<>();
    /** Raw MiniMessage templates for keys that use {@link #messageResolving(String, TagResolver...)}. */
    private final Map<String, String> miniMessageRaw = new ConcurrentHashMap<>();

    public String plaintextMessageFor(@Nonnull String key) {
        return PlainTextComponentSerializer.plainText().serialize(messageFor(key));
    }

    public String prefixedPlaintextMessageFor(@Nonnull String key) {
        return plaintextMessageFor("prefix") + " " + plaintextMessageFor(key);
    }

    public Component prefix() {
        return this.messages.getOrDefault("prefix", Component.empty());
    }

    @Nonnull
    public Component messageFor(@Nonnull String key) {
        return this.messages.getOrDefault(key, Component.text(key));
    }

    /**
     * Deserialize the raw MiniMessage template for {@code key} with tag resolvers.
     * Falls back to {@link #messageFor(String)} if no raw template was stored for the key.
     */
    @Nonnull
    public Component messageResolving(@Nonnull String key, @Nonnull TagResolver... resolvers) {
        String raw = this.miniMessageRaw.get(key);
        if (raw == null) {
            return messageFor(key);
        }
        return MiniMessage.miniMessage().deserialize(raw, resolvers);
    }

    @Nonnull
    public Component prefixedMessageFor(@Nonnull String key) {
        return prefix().appendSpace().append(messageFor(key));
    }

    @Nonnull
    public String miniMessageFormattedFor(@Nonnull String key) {
        return MiniMessage.miniMessage().serialize(messageFor(key));
    }

    public void setMessage(@Nonnull String key, @Nonnull Component message) {
        this.messages.put(key, message);
    }

    public void clear() {
        this.messages.clear();
        this.miniMessageRaw.clear();
    }

    public void load(@Nonnull ConfigurationNode root) throws ConfigurateException {
        Map<String, Component> temp = new HashMap<>();
        Map<String, String> rawTemp = new HashMap<>();
        loadInto("", root, temp, rawTemp);
        this.messages.putAll(temp);
        this.miniMessageRaw.putAll(rawTemp);
    }

    public void save(@Nonnull ConfigurationNode root) throws ConfigurateException {
        for (Map.Entry<String, Component> entry : this.messages.entrySet()) {
            root.node((Object[]) entry.getKey().split("\\.")).set(entry.getValue());
        }
    }

    private void loadInto(String path,
                          ConfigurationNode root,
                          Map<String, Component> temp,
                          Map<String, String> rawTemp) throws ConfigurateException {
        if (!root.empty()) {
            if (root.isList()) {
                List<String> strings = root.getList(String.class, Collections.emptyList());
                TextComponent.Builder builder = Component.text();
                var iterator = strings.iterator();
                while (iterator.hasNext()) {
                    String line = iterator.next().trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    builder.append(MiniMessage.miniMessage().deserialize(line));
                    if (iterator.hasNext()) {
                        builder.appendNewline();
                    }
                }
                temp.put(path, builder.build());
            } else {
                String raw = root.getString();
                if (raw != null) {
                    String trimmed = raw.trim();
                    Component component = MiniMessage.miniMessage().deserialize(trimmed);
                    temp.put(path, component);
                    rawTemp.put(path, trimmed);
                }
            }
        }
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.childrenMap().entrySet()) {
            String key = entry.getKey().toString();
            ConfigurationNode node = entry.getValue();
            String newPath = path.isEmpty() ? key : path + "." + key;
            loadInto(newPath, node, temp, rawTemp);
        }
    }

}
