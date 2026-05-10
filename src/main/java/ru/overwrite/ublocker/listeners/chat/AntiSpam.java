package ru.overwrite.ublocker.listeners.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.configuration.data.AntiSpamSettings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpam extends ChatListener {

    private static final String[] SEARCH = {
            "%player%",
            "%cooldown%",
            "%time_left%",
            "%msg%"
    };

    private final AntiSpamSettings settings;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public AntiSpam(UniversalBlocker plugin) {
        super(plugin);
        this.settings = pluginConfig.getAntiSpamSettings();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChatSpam(AsyncPlayerChatEvent e) {
        long cooldown = settings.cooldown();

        if (cooldown <= 0) {
            return;
        }

        Player player = e.getPlayer();

        if (isAdmin(player, "ublocker.bypass.antispam")) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long lastMessageTime = cooldowns.get(uuid);

        if (lastMessageTime != null) {
            long timeLeft = cooldown - (now - lastMessageTime);

            if (timeLeft > 0) {
                e.setCancelled(true);

                executeActions(
                        player,
                        SEARCH,
                        new String[]{
                                player.getName(),
                                Long.toString(cooldown),
                                Long.toString(timeLeft),
                                e.getMessage()
                        },
                        settings.actionsToExecute()
                );

                return;
            }
        }

        cooldowns.put(uuid, now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cooldowns.remove(e.getPlayer().getUniqueId());
    }
}