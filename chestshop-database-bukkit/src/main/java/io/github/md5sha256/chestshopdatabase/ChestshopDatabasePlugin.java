package io.github.md5sha256.chestshopdatabase;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.md5sha256.chestshopdatabase.adapters.fawe.FAWEHandler;
import io.github.md5sha256.chestshopdatabase.adapters.worldedit.WorldEditHandler;
import io.github.md5sha256.chestshopdatabase.adapters.worldguard.WorldGuardHandler;
import io.github.md5sha256.chestshopdatabase.command.CommandBean;
import io.github.md5sha256.chestshopdatabase.command.FindCommand;
import io.github.md5sha256.chestshopdatabase.command.ReloadCommand;
import io.github.md5sha256.chestshopdatabase.command.ResyncCommand;
import io.github.md5sha256.chestshopdatabase.database.ChestshopMapper;
import io.github.md5sha256.chestshopdatabase.database.DatabaseSession;
import io.github.md5sha256.chestshopdatabase.database.MariaChestshopMapper;
import io.github.md5sha256.chestshopdatabase.database.MariaDatabase;
import io.github.md5sha256.chestshopdatabase.database.MariaPreferenceMapper;
import io.github.md5sha256.chestshopdatabase.database.task.FindTaskFactory;
import io.github.md5sha256.chestshopdatabase.database.task.ResyncTaskFactory;
import io.github.md5sha256.chestshopdatabase.gui.ShopResultsGUI;
import io.github.md5sha256.chestshopdatabase.listener.ChestShopListener;
import io.github.md5sha256.chestshopdatabase.listener.PreviewListener;
import io.github.md5sha256.chestshopdatabase.model.ShopType;
import io.github.md5sha256.chestshopdatabase.preview.PreviewHandler;
import io.github.md5sha256.chestshopdatabase.settings.ComponentSerializer;
import io.github.md5sha256.chestshopdatabase.settings.DatabaseSettings;
import io.github.md5sha256.chestshopdatabase.settings.DummyData;
import io.github.md5sha256.chestshopdatabase.settings.MessageContainer;
import io.github.md5sha256.chestshopdatabase.settings.Settings;
import io.github.md5sha256.chestshopdatabase.util.SimpleItemStack;
import io.github.md5sha256.chestshopdatabase.util.UnsafeChestShopSign;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class ChestshopDatabasePlugin extends JavaPlugin {

    private final ExecutorService databaseExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final MessageContainer messageContainer = new MessageContainer();
    private ChestShopStateImpl shopState;
    private ItemDiscoverer discoverer;
    private Settings settings;
    private DatabaseSettings databaseSettings;
    private ShopResultsGUI gui;
    private ExecutorState executorState;
    private ReplacementRegistry replacements = new ReplacementRegistry();
    private PreviewHandler previewHandler;

    @Override
    public void onLoad() {
        try {
            initDataFolder();
            saveDummyData();
            this.settings = loadSettings();
            this.databaseSettings = loadDatabaseSettings();
            this.messageContainer.load(loadMessages());
        } catch (IOException ex) {
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        UnsafeChestShopSign.init();
        ShopReplacements.registerDefaults(this.replacements);
        shopState = new ChestShopStateImpl(Duration.ofMinutes(5));
        discoverer = new ItemDiscoverer(50, Duration.ofMinutes(5), 50, getServer(), getLogger());
        BukkitScheduler scheduler = getServer().getScheduler();
        executorState = new ExecutorState(databaseExecutor, scheduler.getMainThreadExecutor(this));
        gui = new ShopResultsGUI(this, this.replacements, () -> this.settings);
        SqlSessionFactory sessionFactory = MariaDatabase.buildSessionFactory(this.databaseSettings);
        Supplier<DatabaseSession> sessionSupplier =
                () -> new DatabaseSession(sessionFactory,
                        MariaChestshopMapper.class,
                        MariaPreferenceMapper.class);
        previewHandler = new PreviewHandler(this,
                sessionSupplier,
                this.executorState,
                () -> this.settings);
        cacheItemCodes(sessionFactory);
        registerCommands(sessionFactory);
        scheduleTasks(sessionFactory);
        registerAdapters();
        getServer().getPluginManager()
                .registerEvents(new ChestShopListener(shopState, discoverer, previewHandler), this);
        getServer().getPluginManager().registerEvents(new PreviewListener(
                getLogger(), sessionSupplier,
                this.executorState,
                previewHandler), this);
        getLogger().info("Plugin enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            this.databaseExecutor.shutdownNow();
            this.databaseExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        getLogger().info("Plugin disabled");
    }

    private void registerCommands(@NotNull SqlSessionFactory sessionFactory) {
        Supplier<DatabaseSession> sessionSupplier = () -> new DatabaseSession(sessionFactory,
                MariaChestshopMapper.class, MariaPreferenceMapper.class);
        FindTaskFactory findTaskFactory = new FindTaskFactory(sessionSupplier, executorState);
        ResyncTaskFactory resyncTaskFactory = new ResyncTaskFactory(this.shopState,
                this.discoverer,
                sessionSupplier,
                executorState,
                this,
                this.previewHandler);
        var findCommand = new FindCommand(this.shopState,
                this.discoverer,
                findTaskFactory,
                this.gui,
                this,
                this.previewHandler);
        List<CommandBean> commands = List.of(
                findCommand,
                new ResyncCommand(this, resyncTaskFactory),
                new ReloadCommand(this)
        );
        var csdb = Commands.literal("csdb");
        this.getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS, event -> {
                    commands.stream()
                            .map(CommandBean::commands)
                            .flatMap(List::stream)
                            .map(literal -> csdb.then(literal).build())
                            .forEach(event.registrar()::register);
                    findCommand.commands().stream()
                            .map(LiteralArgumentBuilder::build)
                            .forEach(event.registrar()::register);
                }
        );
    }

    private void registerAdapters() {
        PluginManager pluginManager = getServer().getPluginManager();
        if (pluginManager.isPluginEnabled("WorldEdit")) {
            new WorldEditHandler(this, this.shopState);
        }
        if (pluginManager.isPluginEnabled("FastAsyncWorldEdit")) {
            new FAWEHandler(this, this.shopState);
        }
        if (pluginManager.isPluginEnabled("WorldGuard")) {
            new WorldGuardHandler(this, this.replacements);
        }
    }

    private void cacheItemCodes(@NotNull SqlSessionFactory sessionFactory) {
        try (SqlSession session = sessionFactory.openSession()) {
            ChestshopMapper database = session.getMapper(MariaChestshopMapper.class);
            this.shopState.cacheItemCodes(getLogger(), database);
        }
    }

    private void scheduleTasks(@NotNull SqlSessionFactory sessionFactory) {
        BukkitScheduler scheduler = getServer().getScheduler();
        Logger logger = getLogger();
        long interval = 1;
        scheduler.runTaskTimer(this, () -> {
            Consumer<ChestshopMapper> flushTask = shopState.flushTask();
            if (flushTask == null) {
                return;
            }
            logger.fine("Beginning flush task...");
            CompletableFuture.runAsync(() -> {
                try (SqlSession session = sessionFactory.openSession(ExecutorType.BATCH, false)) {
                    ChestshopMapper chestshopMapper = session.getMapper(MariaChestshopMapper.class);
                    flushTask.accept(chestshopMapper);
                    session.commit();
                } catch (Exception ex) {
                    logger.severe("Failed to flush shop state to database!");
                    ex.printStackTrace();
                }
                logger.fine("Flush task complete!");
            });
        }, interval, interval);
        this.discoverer.schedulePollTask(this, scheduler, 20, 5);
    }

    private void saveDummyData() throws IOException {
        Settings dummy = new Settings(
                SimpleItemStack.fromItemStack(DummyData.shopToIcon(ShopType.BUY)),
                SimpleItemStack.fromItemStack(DummyData.shopToIcon(ShopType.SELL)),
                SimpleItemStack.fromItemStack(DummyData.shopToIcon(ShopType.BOTH)),
                "/commandName <x> <y> <z>",
                true
        );
        File file = new File(getDataFolder(), "dummy-settings.yml");
        YamlConfigurationLoader loader = yamlLoader().file(file).build();
        ConfigurationNode root = loader.createNode();
        root.set(dummy);
        loader.save(root);
        getLogger().info("dummy data saved!");
    }

    private void initDataFolder() throws IOException {
        File dataFolder = getDataFolder();
        if (!dataFolder.isDirectory()) {
            Files.createDirectory(dataFolder.toPath());
        }
    }

    private ConfigurationNode copyDefaultsYaml(@NotNull String resourceName) throws IOException {
        String fileName = resourceName + ".yml";
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                 InputStream inputStream = getResource(fileName)) {
                if (inputStream == null) {
                    getLogger().severe("Failed to copy default messages!");
                } else {
                    inputStream.transferTo(fileOutputStream);
                }
            }
        }
        YamlConfigurationLoader existingLoader = yamlLoader()
                .file(file)
                .build();
        return existingLoader.load();
    }

    private YamlConfigurationLoader.Builder yamlLoader() {
        return YamlConfigurationLoader.builder()
                .defaultOptions(options -> options.serializers(builder -> builder.register(Component.class,
                        ComponentSerializer.MINI_MESSAGE)))
                .nodeStyle(NodeStyle.BLOCK);
    }

    private Settings loadSettings() throws IOException {
        ConfigurationNode settingsRoot = copyDefaultsYaml("settings");
        return settingsRoot.get(Settings.class);
    }

    private DatabaseSettings loadDatabaseSettings() throws IOException {
        ConfigurationNode settingsRoot = copyDefaultsYaml("database-settings");
        return settingsRoot.get(DatabaseSettings.class);
    }

    public ConfigurationNode loadMessages() throws IOException {
        return copyDefaultsYaml("messages");
    }

    @NotNull
    public CompletableFuture<Boolean> reloadMessagesAndSettings() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            ConfigurationNode messagesNode;
            Settings settings;
            try {
                messagesNode = loadMessages();
                settings = loadSettings();
            } catch (IOException ex) {
                ex.printStackTrace();
                future.complete(Boolean.FALSE);
                return;
            }
            executorState.mainThreadExec().execute(() -> {
                this.settings = settings;
                this.messageContainer.clear();
                try {
                    this.messageContainer.load(messagesNode);
                    future.complete(Boolean.TRUE);
                } catch (IOException ex) {
                    getLogger().warning("Failed to load messages!");
                    ex.printStackTrace();
                    future.complete(Boolean.FALSE);
                }
            });
        });
        return future;
    }
}
