package ru.overwrite.ublocker.listeners.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.configuration.data.ChatCharsSettings;

public class ChatFilter extends ChatListener {

    private static final String[] searchList = {"%player%", "%symbol%", "%msg%"};

    public ChatFilter(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChatMessage(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (super.isAdmin(p, "ublocker.bypass.chatchars")) {
            return;
        }
        ChatCharsSettings chatCharsSettings = pluginConfig.getChatCharsSettings();
        String message = e.getMessage();
        String blockedChar = switch (chatCharsSettings.mode()) {
            case STRING -> getFirstBlockedChar(message, chatCharsSettings.charSet());
            case PATTERN -> getFirstBlockedChar(message, chatCharsSettings.pattern());
        };
        if (blockedChar != null) {
            e.setCancelled(true);
            String[] replacementList = {p.getName(), blockedChar, message};
            super.executeActions(p, searchList, replacementList, chatCharsSettings.actionsToExecute());
        }
    }
}
