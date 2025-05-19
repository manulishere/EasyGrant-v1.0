package com.manul.easygrant;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (lore != null) {
            lore.replaceAll(line -> ChatColor.translateAlternateColorCodes('&', line));
            meta.setLore(lore);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    // Удобный статический метод для создания ItemStack из параметров
    public static ItemStack create(String materialName, String name, List<String> lore) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) mat = Material.STONE;
        return new ItemBuilder(mat).setName(name).setLore(lore).build();
    }
}
