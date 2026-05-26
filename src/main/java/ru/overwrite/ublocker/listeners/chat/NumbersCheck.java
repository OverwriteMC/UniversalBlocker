package ru.overwrite.ublocker.listeners.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.configuration.data.NumberCheckSettings;
import ru.overwrite.ublocker.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumbersCheck extends ChatListener {

    private static final String[] searchList = {"%player%", "%limit%", "%msg%"};

    public NumbersCheck(UniversalBlocker plugin) {
        super(plugin);
    }

    private static final Pattern IP_PATTERN = Pattern.compile("(?<![\\d.])\\d{1,3}(?:\\.\\d{1,3}){3}(?![\\d.])");

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChatNumber(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (super.isAdmin(p, "ublocker.bypass.numbers")) {
            return;
        }
        NumberCheckSettings numberCheckSettings = pluginConfig.getNumberCheckSettings();
        String message = e.getMessage();
        if (numberCheckSettings.stripColor()) {
            message = Utils.stripColorCodes(message);
        }
        if (numberCheckSettings.strictCheck()) {
            int count = 0;
            for (int a = 0, b = message.length(); a < b; a++) {
                char c = message.charAt(a);
                if (Character.isDigit(c)) {
                    count++;
                }
            }
            if (count > numberCheckSettings.maxNumbers()) {
                e.setCancelled(true);
                String[] replacementList = {p.getName(), Integer.toString(numberCheckSettings.maxNumbers()), message};
                super.executeActions(p, searchList, replacementList, numberCheckSettings.actionsToExecute());
            }
        } else {
            Matcher matcher = IP_PATTERN.matcher(message);
            int digitsCount = 0;
            while (matcher.find()) {
                String ip = matcher.group();
                if (!isValidIp(ip)) {
                    continue;
                }
                for (int i = matcher.start(), j = matcher.end(); i < j; i++) {
                    char c = message.charAt(i);
                    if (c >= '0' && c <= '9') {
                        digitsCount++;
                    }
                }
            }
            if (digitsCount > numberCheckSettings.maxNumbers()) {
                e.setCancelled(true);
                String[] replacementList = {p.getName(), Integer.toString(numberCheckSettings.maxNumbers()), message};
                super.executeActions(p, searchList, replacementList, numberCheckSettings.actionsToExecute());
            }
        }
    }

    private boolean isValidIp(String ip) {
        int value = 0;
        for (int i = 0, length = ip.length(); i < length; i++) {
            char c = ip.charAt(i);

            if (c == '.') {
                if (value > 255) {
                    return false;
                }
                value = 0;
                continue;
            }
            value = value * 10 + c - '0';
        }
        return value <= 255;
    }
}
