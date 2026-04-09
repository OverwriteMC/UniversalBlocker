package ru.overwrite.ublocker.listeners.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.configuration.data.SignCharsSettings;

public class SignFilter extends ChatListener {

    private static final String[] searchList = {"%player%", "%symbol%", "%msg%"};

    public SignFilter(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignMessage(SignChangeEvent e) {
        Player p = e.getPlayer();
        if (super.isAdmin(p, "ublocker.bypass.signchars")) {
            return;
        }
        SignCharsSettings signCharsSettings = pluginConfig.getSignCharsSettings();
        String line0 = e.getLine(0);
        String line1 = e.getLine(1);
        String line2 = e.getLine(2);
        String line3 = e.getLine(3);
        String[] messages = {line0, line1, line2, line3};
        for (String message : messages) {
            if (message == null || message.isBlank())
                continue;
            String blockedChar = switch (signCharsSettings.mode()) {
                case STRING -> getFirstBlockedChar(message, signCharsSettings.charSet());
                case PATTERN -> getFirstBlockedChar(message, signCharsSettings.pattern());
            };
            if (blockedChar != null) {
                e.setCancelled(true);
                String[] replacementList = {p.getName(), blockedChar, line0 + line1 + line2 + line3};
                super.executeActions(p, searchList, replacementList, signCharsSettings.actionsToExecute());
                return;
            }
        }
    }
}