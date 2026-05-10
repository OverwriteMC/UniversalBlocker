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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AntiSpam extends ChatListener {

    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final String[] searchList = {"%player%", "%cooldown%", "%time_left%", "%msg%"};

    public AntiSpam(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChatSpam(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (super.isAdmin(p, "ublocker.bypass.antispam")) {
            return;
        }

        AntiSpamSettings antiSpamSettings = pluginConfig.getAntiSpamSettings();
        long cooldown = antiSpamSettings.cooldown();
        if (cooldown <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = p.getUniqueId();
        AtomicBoolean blocked = new AtomicBoolean();
        AtomicLong remainingCooldown = new AtomicLong();
        lastMessageTime.compute(uuid, (key, previousMessageTime) -> {
            if (previousMessageTime == null) {
                return now;
            }

            long timeLeft = cooldown - (now - previousMessageTime);
            if (timeLeft > 0) {
                blocked.set(true);
                remainingCooldown.set(timeLeft);
                return previousMessageTime;
            }

            return now;
        });

        if (blocked.get()) {
            e.setCancelled(true);
            String[] replacementList = {
                    p.getName(),
                    Long.toString(cooldown),
                    Long.toString(remainingCooldown.get()),
                    e.getMessage()
            };
            super.executeActions(p, searchList, replacementList, antiSpamSettings.actionsToExecute());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastMessageTime.remove(e.getPlayer().getUniqueId());
    }
}
