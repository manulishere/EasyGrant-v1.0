package com.manul.easygrant;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

public class Commands implements CommandExecutor, TabCompleter {

    private final EasyGrant plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public Commands(EasyGrant plugin) {
        this.plugin = plugin;
        loadDataFile();
    }

    private void loadDataFile() {
        File dataFolder = new File(plugin.getDataFolder(), "Data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        dataFile = new File(dataFolder, "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                dataConfig = YamlConfiguration.loadConfiguration(dataFile);
                dataConfig.set("cooldowns", new HashMap<String, Long>());
                dataConfig.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.getKeys(false).isEmpty()) {
            dataConfig.set("cooldowns", new HashMap<String, Long>());
            saveDataConfig();
        }
    }

    private void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Long getCooldownEnd(UUID playerUUID) {
        if (!dataConfig.contains("cooldowns." + playerUUID.toString())) return null;
        return dataConfig.getLong("cooldowns." + playerUUID.toString());
    }

    private void setCooldown(UUID playerUUID, long endTimestamp) {
        dataConfig.set("cooldowns." + playerUUID.toString(), endTimestamp);
        saveDataConfig();
    }

    private void removeCooldown(UUID playerUUID) {
        dataConfig.set("cooldowns." + playerUUID.toString(), null);
        saveDataConfig();
    }

    private boolean hasActiveCooldown(UUID playerUUID) {
        Long end = getCooldownEnd(playerUUID);
        if (end == null) return false;
        long now = System.currentTimeMillis();
        return now < end;
    }

    private long getCooldownRemaining(UUID playerUUID) {
        Long end = getCooldownEnd(playerUUID);
        if (end == null) return 0;
        long now = System.currentTimeMillis();
        long diff = (end - now) / 1000;
        return diff > 0 ? diff : 0;
    }

    private Map<String, Long> formatTime(long totalSeconds) {
        Map<String, Long> map = new HashMap<>();
        long days = TimeUnit.SECONDS.toDays(totalSeconds);
        long hours = TimeUnit.SECONDS.toHours(totalSeconds) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(totalSeconds));
        long seconds = totalSeconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(totalSeconds));
        map.put("cooldown_days", days);
        map.put("cooldown_hours", hours);
        map.put("cooldown_minutes", minutes);
        map.put("cooldown_seconds", seconds);
        return map;
    }

    // Новый метод для получения префикса или названия группы
    private String getGroupDisplayName(String groupName) {
        LuckPerms api = plugin.getLuckPerms();
        Group group = api.getGroupManager().getGroup(groupName);
        if (group != null) {
            String prefix = group.getCachedData().getMetaData().getPrefix();
            if (prefix != null && !prefix.isEmpty()) {
                return prefix;
            }
        }
        return groupName;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("sendgrant")) {
            handleSendGrant(sender, args);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                if (!sender.hasPermission("easygrant.command.help")) {
                    sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
                    return true;
                }
                sendHelp(sender);
                break;

            case "give":
                if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
                    return true;
                }
                if (!sender.hasPermission("easygrant.command.give")) {
                    sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getLangMessage("prefix") + " &cUsage: /easygrant give <player> <group>".replace("&", "§"));
                    return true;
                }
                handleGive(sender, args[1], args[2]);
                break;

            case "import":
                if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
                    return true;
                }
                if (!sender.hasPermission("easygrant.command.import")) {
                    sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
                    return true;
                }
                handleImport(sender);
                break;

            case "info":
                if (!sender.hasPermission("easygrant.command.info")) {
                    sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
                    return true;
                }
                sender.sendMessage("".replace("&", "§"));
                sender.sendMessage("".replace("&", "§"));
                sender.sendMessage("&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-".replace("&", "§"));
                sender.sendMessage("&c&lEasy&f&lGrant &f&lv&c&l1&f&l.&c&l0&r &fby &e&lmanulishere".replace("&", "§"));
                sender.sendMessage("§f- §fCreated on §c16§f/§c05§f/§c25".replace("&", "§"));
                sender.sendMessage("§f- §fFor §6§lPaperSpigot§r §c§l1§f§l.§c§l21§f§l+".replace("&", "§"));
                sender.sendMessage("&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-&c-&f-".replace("&", "§"));
                sender.sendMessage("".replace("&", "§"));
                sender.sendMessage("".replace("&", "§"));
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpLines = plugin.getLangConfig().getStringList("help_message");
        for (String line : helpLines) {
            if (!line.contains("|")) {
                sender.sendMessage(line.replace("&", "§"));
                continue;
            }
            String[] parts = line.split("\\|");
            String msg = parts[0].trim();
            String perm = parts[1].trim();
            if (sender.hasPermission(perm) || sender.hasPermission("easygrant.commands.use")) {
                sender.sendMessage(msg.replace("&", "§"));
            }
        }
    }

    private void handleGive(CommandSender sender, String targetName, String grantGroup) {
        String permission = "easygrant.grant." + grantGroup.toLowerCase();
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(plugin.getLangMessage("grant_player_notfound", Map.of("prefix", plugin.getLangMessage("prefix"), "player", targetName)));
            return;
        }

        List<String> groups = plugin.getMainConfig().getStringList("grant_groups");
        if (!groups.contains(grantGroup)) {
            sender.sendMessage(plugin.getLangMessage("group_error_message", Map.of("prefix", plugin.getLangMessage("prefix"), "grant_group", getGroupDisplayName(grantGroup))));
            return;
        }

        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (p.getUniqueId().equals(target.getUniqueId())) {
                sender.sendMessage(plugin.getLangMessage("sender_grant_give_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
                return;
            }
        }

        LuckPerms api = plugin.getLuckPerms();
        User user = api.getUserManager().getUser(target.getUniqueId());
        if (user == null) {
            sender.sendMessage(plugin.getLangMessage("grant_player_notfound", Map.of("prefix", plugin.getLangMessage("prefix"), "player", targetName)));
            return;
        }

        String displayGroup = getGroupDisplayName(grantGroup);

        boolean hasGroup = user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .anyMatch(g -> g.equalsIgnoreCase(grantGroup));
        if (hasGroup) {
            sender.sendMessage(plugin.getLangMessage("sender_group_coincidence_message", Map.of("prefix", plugin.getLangMessage("prefix"), "grant_group", displayGroup)));
            return;
        }

        List<String> groupOrder = plugin.getGroupsConfig().getStringList("groups");
        if (groupOrder.isEmpty()) {
            sender.sendMessage(plugin.getLangMessage("imports_not_found", Map.of("prefix", plugin.getLangMessage("prefix"))));
            return;
        }
        int targetGroupIndex = groupOrder.indexOf(grantGroup);
        if (targetGroupIndex == -1) {
            sender.sendMessage(plugin.getLangMessage("group_error_message", Map.of("prefix", plugin.getLangMessage("prefix"), "grant_group", displayGroup)));
            return;
        }
        for (String g : groupOrder) {
            if (g.equalsIgnoreCase(grantGroup)) break;
            boolean hasHigher = user.getNodes().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> ((InheritanceNode) node).getGroupName())
                    .anyMatch(gr -> gr.equalsIgnoreCase(g));
            if (hasHigher) {
                sender.sendMessage(plugin.getLangMessage("sender_group_hight_message", Map.of("prefix", plugin.getLangMessage("prefix"), "grant_group", displayGroup)));
                return;
            }
        }

        user.data().clear(node -> node instanceof InheritanceNode);
        InheritanceNode node = InheritanceNode.builder(grantGroup).build();
        user.data().add(node);
        user.setPrimaryGroup(grantGroup);
        api.getUserManager().saveUser(user);

        sender.sendMessage(plugin.getLangMessage("sender_grant_successfully_message", Map.of(
                "prefix", plugin.getLangMessage("prefix"),
                "grant_group", displayGroup,
                "player", targetName)));

if (target.isOnline()) {
    Player onlineTarget = (Player) target;

    Map<String, String> placeholders = Map.of(
        "prefix", plugin.getLangMessage("prefix"),
        "sender_name", sender.getName(),
        "grant_group", displayGroup
    );

    // Отправляем обычное сообщение с подстановкой
    onlineTarget.sendMessage(plugin.getLangMessage("reciever_grant_message", placeholders));

    if (plugin.getMainConfig().getBoolean("title", true)) {
        int start = plugin.getMainConfig().getInt("title_start", 1000);
        int duration = plugin.getMainConfig().getInt("title_duration", 1000);
        int end = plugin.getMainConfig().getInt("title_end", 1000);

        // Получаем и подставляем значения в title и subtitle
        String title = plugin.getLangMessage("reciever_title", placeholders).replace("&", "§");
        String subtitle = plugin.getLangMessage("reciever_subtitle", placeholders).replace("&", "§");

        onlineTarget.sendTitle(title, subtitle, start, duration, end);
        }
    }
}

    private void handleImport(CommandSender sender) {
        LuckPerms api = plugin.getLuckPerms();

        List<Group> groups = new ArrayList<>(api.getGroupManager().getLoadedGroups());

        if (groups.isEmpty()) {
        sender.sendMessage(plugin.getLangMessage("imports_not_found", Map.of("prefix", plugin.getLangMessage("prefix"))));
        return;
    }

        // Сортируем группы: сначала без веса, затем по весу от большего к меньшему
        groups.sort(Comparator.<Group>comparingInt(g -> g.getWeight().orElse(Integer.MAX_VALUE)).reversed()
        .thenComparing(Group::getName));

        // Получаем имена групп в отсортированном порядке
        List<String> groupNames = groups.stream()
        .map(Group::getName)
        .toList();

        plugin.getGroupsConfig().set("groups", groupNames);
        try {
        plugin.getGroupsConfig().save(plugin.getGroupsFile());
    }   catch (Exception e) {
        sender.sendMessage(plugin.getLangMessage("prefix") + " §cError saving groups.yml");
        e.printStackTrace();
        return;
    }
        sender.sendMessage(plugin.getLangMessage("grant_import_successfully", Map.of("prefix", plugin.getLangMessage("prefix"))));
}


    private void handleSendGrant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
            return;
        }
        if (args.length != 1) {
            player.sendMessage("§cИспользование: /sendgrant <ник>");
            return;
        }
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            player.sendMessage(plugin.getLangMessage("grant_player_notfound", Map.of("prefix", plugin.getLangMessage("prefix"), "player", targetName)));
            return;
        }
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(plugin.getLangMessage("sender_grant_give_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
            return;
        }

        // Проверка активного кулдауна перед выдачей
        if (!player.hasPermission("easygrant.cooldown.bypass")) {
            boolean cooldownEnabled = plugin.getMainConfig().getBoolean("grant_give_cooldown", true);
            Long cooldownEnd = getCooldownEnd(player.getUniqueId());
            long now = System.currentTimeMillis();

            if (cooldownEnd != null && now < cooldownEnd) {
                if (cooldownEnabled) {
                    long remainingSeconds = getCooldownRemaining(player.getUniqueId());
                    Map<String, Long> timeMap = formatTime(remainingSeconds);
                    String remainingStr = buildTimeString(timeMap);
                    player.sendMessage(plugin.getLangMessage("grant_cooldown_true", Map.of(
                            "prefix", plugin.getLangMessage("prefix"),
                            "cooldown_remaining", remainingStr,
                            "cooldown_days", String.valueOf(timeMap.get("cooldown_days")),
                            "cooldown_hours", String.valueOf(timeMap.get("cooldown_hours")),
                            "cooldown_minutes", String.valueOf(timeMap.get("cooldown_minutes")),
                            "cooldown_seconds", String.valueOf(timeMap.get("cooldown_seconds"))
                    )));
                    return;
                } else {
                    player.sendMessage(plugin.getLangMessage("grant_cooldown_false", Map.of("prefix", plugin.getLangMessage("prefix"))));
                    return;
                }
            }
        }

        // Определяем группу для выдачи
        List<String> grantGroups = plugin.getMainConfig().getStringList("grant_groups");
        String grantGroup = null;
        for (String group : grantGroups) {
            if (player.hasPermission("easygrant.grant." + group.toLowerCase())) {
                grantGroup = group;
                break;
            }
        }
        if (grantGroup == null) {
            player.sendMessage(plugin.getLangMessage("permission_error", Map.of("prefix", plugin.getLangMessage("prefix"))));
            return;
        }

        // Выдаём грант и после успешной выдачи ставим кулдаун
        grantGroupToPlayer(player, targetName, grantGroup);
    }

    private String buildTimeString(Map<String, Long> timeMap) {
        StringBuilder sb = new StringBuilder();
        long days = timeMap.getOrDefault("cooldown_days", 0L);
        long hours = timeMap.getOrDefault("cooldown_hours", 0L);
        long minutes = timeMap.getOrDefault("cooldown_minutes", 0L);
        long seconds = timeMap.getOrDefault("cooldown_seconds", 0L);

        if (days > 0) {
            sb.append(days).append(" ").append(plugin.getLangMessage("cooldown_days").replace("&", "§")).append(" ");
        }
        if (hours > 0) {
            sb.append(hours).append(" ").append(plugin.getLangMessage("cooldown_hours").replace("&", "§")).append(" ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(" ").append(plugin.getLangMessage("cooldown_minutes").replace("&", "§")).append(" ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append(" ").append(plugin.getLangMessage("cooldown_seconds").replace("&", "§"));
        }
        return sb.toString().trim();
    }

    private void grantGroupToPlayer(Player sender, String targetName, String grantGroup) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        LuckPerms api = plugin.getLuckPerms();
        User user = api.getUserManager().getUser(target.getUniqueId());
        if (user == null) {
            sender.sendMessage(plugin.getLangMessage("grant_player_notfound", Map.of("prefix", plugin.getLangMessage("prefix"), "player", targetName)));
            return;
        }

        String displayGroup = getGroupDisplayName(grantGroup);

        boolean hasGroup = user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .anyMatch(g -> g.equalsIgnoreCase(grantGroup));
        if (hasGroup) {
            sender.sendMessage(plugin.getLangMessage("sender_group_coincidence_message", Map.of("prefix", plugin.getLangMessage("prefix"), "grant_group", displayGroup)));
            return;
        }

        List<String> groupOrder = plugin.getGroupsConfig().getStringList("groups");
        for (String g : groupOrder) {
            if (g.equalsIgnoreCase(grantGroup)) break;
            boolean hasHigher = user.getNodes().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> ((InheritanceNode) node).getGroupName())
                    .anyMatch(gr -> gr.equalsIgnoreCase(g));
            if (hasHigher) {
                sender.sendMessage(plugin.getLangMessage("sender_group_hight_message", Map.of("prefix", plugin.getLangMessage("prefix"), "grant_group", displayGroup)));
                return;
            }
        }

        user.data().clear(node -> node instanceof InheritanceNode);
        InheritanceNode node = InheritanceNode.builder(grantGroup).build();
        user.data().add(node);
        user.setPrimaryGroup(grantGroup);
        api.getUserManager().saveUser(user);

        sender.sendMessage(plugin.getLangMessage("sender_grant_successfully_message", Map.of(
                "prefix", plugin.getLangMessage("prefix"),
                "grant_group", displayGroup,
                "player", targetName)));

        if (target.isOnline()) {
        Player onlineTarget = (Player) target;

        // Создаем карту с параметрами для подстановки
        Map<String, String> placeholders = Map.of(
        "prefix", plugin.getLangMessage("prefix"),
        "sender_name", sender.getName(),
        "grant_group", displayGroup
    );

    // Отправляем обычное сообщение с подстановкой
        onlineTarget.sendMessage(plugin.getLangMessage("reciever_grant_message", placeholders));

        if (plugin.getMainConfig().getBoolean("title", true)) {
        int start = plugin.getMainConfig().getInt("title_start", 1000);
        int duration = plugin.getMainConfig().getInt("title_duration", 1000);
        int end = plugin.getMainConfig().getInt("title_end", 1000);

        // Получаем шаблоны титула и подзаголовка
        String rawTitle = plugin.getLangMessage("reciever_title");
        String rawSubtitle = plugin.getLangMessage("reciever_subtitle");

        // Подставляем значения в шаблоны
        String processedTitle = replacePlaceholdersPercent(rawTitle, placeholders);
        String processedSubtitle = replacePlaceholdersPercent(rawSubtitle, placeholders);

        // Заменяем & на § для цветовых кодов
        processedTitle = processedTitle.replace("&", "§");
        processedSubtitle = processedSubtitle.replace("&", "§");

        // Отправляем титул
        onlineTarget.sendTitle(processedTitle, processedSubtitle, start, duration, end);
    }
}

        // Логирование
        String logLine = String.format("- %s : %s : %s : %s",
                sender.getName(),
                sender.getUniqueId(),
                targetName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        List<String> logs = plugin.getLogsConfig().getStringList("logs");
        logs.add(logLine);
        plugin.getLogsConfig().set("logs", logs);
        try {
            plugin.getLogsConfig().save(plugin.getLogsFile());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Установка кулдауна после успешной выдачи гранта
        if (!sender.hasPermission("easygrant.cooldown.bypass")) {
            long now = System.currentTimeMillis();
            boolean cooldownEnabled = plugin.getMainConfig().getBoolean("grant_give_cooldown", true);
            if (cooldownEnabled) {
                long cooldownSeconds = plugin.getMainConfig().getLong("cooldown", 60);
                setCooldown(sender.getUniqueId(), now + cooldownSeconds * 1000);
            } else {
                setCooldown(sender.getUniqueId(), Long.MAX_VALUE);
            }
        }
    }

    private String replacePlaceholdersPercent(String text, Map<String, String> placeholders) {
    String result = text;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        result = result.replace("%" + entry.getKey() + "%", entry.getValue());
    }
    return result;
}

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("sendgrant")) {
            if (args.length == 1) {
                String prefix = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .toList();
            }
        } else if (command.getName().equalsIgnoreCase("easygrant")) {
            if (args.length == 1) {
                List<String> subs = new ArrayList<>();
                if (sender.hasPermission("easygrant.command.give")) subs.add("give");
                if (sender.hasPermission("easygrant.command.import")) subs.add("import");
                if (sender.hasPermission("easygrant.command.help")) subs.add("help");
                if (sender.hasPermission("easygrant.command.info")) subs.add("info");

                String prefix = args[0].toLowerCase();
                return subs.stream()
                        .filter(s -> s.startsWith(prefix))
                        .toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                String prefix = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .toList();
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                List<String> groups = plugin.getMainConfig().getStringList("grant_groups");
                String prefix = args[2].toLowerCase();
                return groups.stream()
                        .filter(g -> g.toLowerCase().startsWith(prefix))
                        .toList();
            }
        }
        return Collections.emptyList();
    }
}
