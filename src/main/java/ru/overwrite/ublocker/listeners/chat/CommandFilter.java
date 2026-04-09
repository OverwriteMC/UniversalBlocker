package ru.overwrite.ublocker.listeners.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.configuration.data.CommandCharsSettings;

public class CommandFilter extends ChatListener {

    private static final String[] searchList = {"%player%", "%symbol%", "%msg%"};

    public CommandFilter(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandMessage(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (super.isAdmin(p, "ublocker.bypass.commandchars")) {
            return;
        }
        CommandCharsSettings commandCharsSettings = pluginConfig.getCommandCharsSettings();
        String message = e.getMessage();
        String blockedChar = switch (commandCharsSettings.mode()) {
            case STRING -> getFirstBlockedChar(message, commandCharsSettings.charSet());
            case PATTERN -> getFirstBlockedChar(message, commandCharsSettings.pattern());
        };
        if (blockedChar != null) {
            e.setCancelled(true);
            String[] replacementList = {p.getName(), blockedChar};
            super.executeActions(p, searchList, replacementList, commandCharsSettings.actionsToExecute());
        }
    }
}
