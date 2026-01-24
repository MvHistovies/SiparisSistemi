package org.larune.siparis.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.larune.siparis.OrderPlugin;
import org.larune.siparis.npc.NPCData;
import org.larune.siparis.util.Text;

import java.util.ArrayList;
import java.util.List;

public class SiparisCommand implements CommandExecutor, TabExecutor {

    private final OrderPlugin plugin;

    public SiparisCommand(OrderPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("siparis").setExecutor(this);
        plugin.getCommand("siparis").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§7[DEBUG] Komut alindi: " + label + " args=" + args.length);

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Bu komut sadece oyuncular icin.");
            return true;
        }

        if (args.length == 0) {
            plugin.gui().openMainMenu(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!p.hasPermission("siparis.admin")) {
                p.sendMessage(Text.msg("messages.noPermission"));
                return true;
            }
            plugin.gui().openAdmin(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("npc")) {
            p.sendMessage("§aNPC komutu algilandi!");
            if (!p.hasPermission("siparis.admin")) {
                p.sendMessage(Text.msg("messages.noPermission"));
                return true;
            }
            p.sendMessage("§aYetki kontrolu gecti!");
            handleNPCCommand(p, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("siparis.admin")) {
                p.sendMessage(Text.msg("messages.noPermission"));
                return true;
            }
            plugin.reloadConfig();
            Text.init(plugin);
            p.sendMessage(Text.msg("messages.adminReloaded"));
            return true;
        }

        p.sendMessage("§cKullanim: /siparis");
        return true;
    }

    private void handleNPCCommand(Player p, String[] args) {
        p.sendMessage("§ehandleNPCCommand cagirildi!");

        if (plugin.getNpcManager() == null) {
            p.sendMessage("§cNPC sistemi devre disi. (getNpcManager null)");
            return;
        }

        p.sendMessage("§aNPCManager mevcut!");

        if (args.length < 2) {
            p.sendMessage("§eNPC Komutlari:");
            p.sendMessage("§7/siparis npc create <isim> §8- §fBulundugun yerde NPC olustur");
            p.sendMessage("§7/siparis npc remove <id> §8- §fNPC'yi sil");
            p.sendMessage("§7/siparis npc list §8- §fNPC'leri listele");
            p.sendMessage("§7/siparis npc tp <id> §8- §fNPC'ye isinlan");
            return;
        }

        String sub = args[1].toLowerCase();

        if (sub.equals("create") || sub.equals("olustur")) {
            String name = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "§eSiparis";
            p.sendMessage("§7NPC olusturuluyor: " + name);
            NPCData npc = plugin.getNpcManager().createNPC(name, p.getLocation());
            if (npc != null) {
                p.sendMessage("§aNPC olusturuldu! §7ID: §f" + npc.getId());
                p.sendMessage("§7Entity UUID: §f" + (npc.getEntityUuid() != null ? npc.getEntityUuid().toString() : "null"));
            } else {
                p.sendMessage("§cNPC olusturulamadi!");
            }
            return;
        }

        if (sub.equals("remove") || sub.equals("delete") || sub.equals("sil")) {
            if (args.length < 3) {
                p.sendMessage("§cKullanim: /siparis npc remove <id>");
                return;
            }
            try {
                int id = Integer.parseInt(args[2]);
                if (plugin.getNpcManager().removeNPC(id)) {
                    p.sendMessage("§aNPC silindi. §7ID: §f" + id);
                } else {
                    p.sendMessage("§cNPC bulunamadi.");
                }
            } catch (NumberFormatException e) {
                p.sendMessage("§cGecersiz ID.");
            }
            return;
        }

        if (sub.equals("list") || sub.equals("liste")) {
            var npcs = plugin.getNpcManager().getAllNPCs();
            if (npcs.isEmpty()) {
                p.sendMessage("§eKayitli NPC yok.");
            } else {
                p.sendMessage("§eKayitli NPC'ler:");
                for (NPCData npc : npcs) {
                    p.sendMessage("§7- §fID: " + npc.getId() + " §7| §f" + npc.getName() + " §7| §f" + npc.getLocation().getWorld().getName());
                }
            }
            return;
        }

        if (sub.equals("tp") || sub.equals("teleport")) {
            if (args.length < 3) {
                p.sendMessage("§cKullanim: /siparis npc tp <id>");
                return;
            }
            try {
                int id = Integer.parseInt(args[2]);
                NPCData npc = plugin.getNpcManager().getNPC(id);
                if (npc != null && npc.getLocation() != null) {
                    p.teleport(npc.getLocation());
                    p.sendMessage("§aNPC'ye isinlandin.");
                } else {
                    p.sendMessage("§cNPC bulunamadi.");
                }
            } catch (NumberFormatException e) {
                p.sendMessage("§cGecersiz ID.");
            }
            return;
        }

        p.sendMessage("§cBilinmeyen komut. /siparis npc");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();

        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            if (p.hasPermission("siparis.admin")) {
                out.add("admin");
                out.add("npc");
                out.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("npc")) {
            if (p.hasPermission("siparis.admin")) {
                out.add("create");
                out.add("remove");
                out.add("list");
                out.add("tp");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("npc")) {
            if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("tp")) {
                if (plugin.getNpcManager() != null) {
                    for (NPCData npc : plugin.getNpcManager().getAllNPCs()) {
                        out.add(String.valueOf(npc.getId()));
                    }
                }
            }
        }

        return out;
    }
}