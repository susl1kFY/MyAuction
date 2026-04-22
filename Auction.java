package me.auction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Auction extends JavaPlugin implements Listener, CommandExecutor {
    private static Economy econ = null;
    private final List<AuctionItem> items = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault не найден! Плагин выключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("ah").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                p.sendMessage("§cВозьмите предмет в руку!");
                return true;
            }
            try {
                double price = Double.parseDouble(args[1]);
                if (price <= 0) { p.sendMessage("§cЦена должна быть больше 0!"); return true; }
                items.add(new AuctionItem(item.clone(), p.getUniqueId(), price));
                p.getInventory().setItemInMainHand(null);
                p.sendMessage("§a[AH] Предмет выставлен за §6" + price + "$");
            } catch (NumberFormatException e) {
                p.sendMessage("§cИспользуйте: /ah sell <цена>");
            }
        } else {
            Inventory gui = Bukkit.createInventory(null, 54, "§8Аукцион");
            for (AuctionItem ai : items) {
                ItemStack display = ai.item.clone();
                ItemMeta meta = display.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("§7---");
                lore.add("§fЦена: §6" + ai.price + "$");
                lore.add("§fПродавец: §7" + Bukkit.getOfflinePlayer(ai.seller).getName());
                lore.add("§eНажми, чтобы купить!");
                meta.setLore(lore);
                display.setItemMeta(meta);
                gui.addItem(display);
            }
            p.openInventory(gui);
        }
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Аукцион") && e.getCurrentItem() != null) {
            e.setCancelled(true);
            Player buyer = (Player) e.getWhoClicked();
            int slot = e.getRawSlot();
            if (slot >= 0 && slot < items.size()) {
                AuctionItem ai = items.get(slot);
                if (econ.getBalance(buyer) >= ai.price) {
                    econ.withdrawPlayer(buyer, ai.price);
                    econ.depositPlayer(Bukkit.getOfflinePlayer(ai.seller), ai.price);
                    buyer.getInventory().addItem(ai.item);
                    items.remove(slot);
                    buyer.closeInventory();
                    buyer.sendMessage("§a[AH] Предмет успешно куплен!");
                } else {
                    buyer.sendMessage("§cНедостаточно денег!");
                }
            }
        }
    }

    private record AuctionItem(ItemStack item, UUID seller, double price) {}
}
