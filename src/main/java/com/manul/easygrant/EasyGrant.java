package com.manul.easygrant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class EasyGrant extends JavaPlugin {

    private LuckPerms luckPerms;
    private FileConfiguration config, langConfig, dataConfig, logsConfig, groupsConfig;
    private File configFile, langFile, dataFile, logsFile, groupsFile;
    private String language;
    private Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        // ANCII текст при запуске плагина
        // Цвета
        final String RESET = "\u001B[0m";
        final String RED = "\u001B[31m";
        final String GREEN = "\u001B[32m";
        final String YELLOW = "\u001B[33m";
        final String CYAN = "\u001B[36m";
        final String WHITE = "\u001B[37m";

        //Текст
        getLogger().info(WHITE + "" + RESET);
        getLogger().info(CYAN + "===================================" + RESET);
        getLogger().info(RED + " Easy" + WHITE + "Grant " + GREEN + "Enabled " + RESET);
        getLogger().info(YELLOW + " Version" + CYAN + " 1.0 " + RESET);
        getLogger().info(YELLOW + " PaperMC" + CYAN + " 1.21+ " + RESET);
        getLogger().info(CYAN + "===================================" + RESET);
        getLogger().info(WHITE + "" + RESET);


        // Инициализация LuckPerms API
        luckPerms = LuckPermsProvider.get();

        // Загрузка конфигов
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        loadFileConfigs();

        // Сохраняем языковые файлы по умолчанию, если их нет
        saveDefaultLanguages();

        language = config.getString("language", "en_US");
        loadLanguageFile(language);

        // Регистрация команд
        getCommand("easygrant").setExecutor(new Commands(this));
        getCommand("easygrant").setTabCompleter(new Commands(this));
        getCommand("sendgrant").setExecutor(new Commands(this));
        getCommand("sendgrant").setTabCompleter(new Commands(this));

        // Запуск задачи проверки изменений конфигов
        startConfigWatcher();

        getLogger().info("EasyGrant enabled!");
    }

    /**
     * Метод копирует языковые файлы en_US.yml и ru_RU.yml из ресурсов в папку плагина,
     * если их там ещё нет.
     */
    private void saveDefaultLanguages() {
        String[] languages = {"en_US.yml", "ru_RU.yml"};
        for (String langFileName : languages) {
            File langFile = new File(getDataFolder() + "/Languages", langFileName);
            if (!langFile.exists()) {
                saveResource("Languages/" + langFileName, false);
            }
        }
    }

    private void loadFileConfigs() {
        // Загрузка data.yml
        dataFile = new File(getDataFolder(), "Data/data.yml");
        if (!dataFile.exists()) saveResource("Data/data.yml", false);
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Загрузка logs.yml
        logsFile = new File(getDataFolder(), "Data/logs.yml");
        if (!logsFile.exists()) saveResource("Data/logs.yml", false);
        logsConfig = YamlConfiguration.loadConfiguration(logsFile);

        // Загрузка groups.yml
        groupsFile = new File(getDataFolder(), "groups.yml");
        if (!groupsFile.exists()) saveResource("groups.yml", false);
        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
    }

    private void loadLanguageFile(String lang) {
    langFile = new File(getDataFolder() + "/Languages", lang + ".yml");
    if (!langFile.exists()) {
        saveResource("Languages/" + lang + ".yml", false);
    }

    langConfig = YamlConfiguration.loadConfiguration(langFile);

    // Загрузка дефолтов из ресурсов
    var defConfigStream = getResource("Languages/" + lang + ".yml");
    if (defConfigStream != null) {
        var defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defConfigStream));
        langConfig.setDefaults(defConfig);
        langConfig.options().copyDefaults(true);
        try {
            langConfig.save(langFile);
        } catch (Exception e) {
            getLogger().warning("Не удалось сохранить языковой файл " + langFile.getName());
            e.printStackTrace();
        }
    }
}

    public String getLangMessage(String path, Map<String, String> placeholders) {
        String msg = langConfig.getString(path, path);
        if (msg == null) return path;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        // Замена & на § для цвета
        return msg.replace("&", "§");
    }

    public void reloadLanguage(String lang) {
    this.language = lang;
    loadLanguageFile(lang);
    getLogger().info("Language reloaded to: " + lang);
    }

    public String getLangMessage(String path) {
        return getLangMessage(path, new HashMap<>());
    }

    private void startConfigWatcher() {
    new BukkitRunnable() {
        private long lastConfigModified = configFile.lastModified();
        private long lastLangModified = langFile.lastModified();
        private String lastLanguage = language;

        @Override
        public void run() {
            boolean changed = false;

            if (configFile.lastModified() != lastConfigModified) {
                reloadConfig();
                config = YamlConfiguration.loadConfiguration(configFile);
                lastConfigModified = configFile.lastModified();
                changed = true;

                String newLang = config.getString("language", "en_US");
                if (!newLang.equalsIgnoreCase(lastLanguage)) {
                    reloadLanguage(newLang);
                    lastLanguage = newLang;
                }
            }
        
            if (langFile.lastModified() != lastLangModified) {
                langConfig = YamlConfiguration.loadConfiguration(langFile);
                lastLangModified = langFile.lastModified();
                changed = true;
            }

            if (changed) {
                String reloadMsg = getLangMessage("reload_plugin_message", Map.of("prefix", getLangMessage("prefix")));
                Bukkit.getConsoleSender().sendMessage(reloadMsg);
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.isOp())
                        .forEach(p -> p.sendMessage(reloadMsg));
            }
        }
    }.runTaskTimer(this, 0L, 100L);
}

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    public FileConfiguration getLogsConfig() {
        return logsConfig;
    }

    public FileConfiguration getGroupsConfig() {
        return groupsConfig;
    }

    public File getDataFile() {
        return dataFile;
    }

    public File getLogsFile() {
        return logsFile;
    }

    public File getGroupsFile() {
        return groupsFile;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    public File getLangFile() {
        return langFile;
    }

    public File getConfigFile() {
        return configFile;
    }

    public FileConfiguration getMainConfig() {
        return config;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public void onDisable() {
        // ANCII текст при выключении плагина
        // Цвета
        final String RESET = "\u001B[0m";
        final String RED = "\u001B[31m";
        final String YELLOW = "\u001B[33m";
        final String CYAN = "\u001B[36m";
        final String WHITE = "\u001B[37m";

        //Текст
        getLogger().info(WHITE + "" + RESET);
        getLogger().info(CYAN + "===================================" + RESET);
        getLogger().info(RED + " Easy" + WHITE + "Grant " + RED + "Disabled " + RESET);
        getLogger().info(YELLOW + " Version" + CYAN + " 1.0 " + RESET);
        getLogger().info(YELLOW + " PaperMC" + CYAN + " 1.21+ " + RESET);
        getLogger().info(CYAN + "===================================" + RESET);
        getLogger().info(WHITE + "" + RESET);
    }
}
