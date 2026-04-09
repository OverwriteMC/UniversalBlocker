package ru.overwrite.ublocker.listeners.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEditBookEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.configuration.data.BookCharsSettings;

public class BookFilter extends ChatListener {

    private static final String[] searchList = {"%player%", "%symbol%"};

    public BookFilter(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBookEvent(PlayerEditBookEvent e) {
        Player p = e.getPlayer();
        if (super.isAdmin(p, "ublocker.bypass.bookchars")) {
            return;
        }

        BookCharsSettings bookCharsSettings = pluginConfig.getBookCharsSettings();

        for (String page : e.getNewBookMeta().getPages()) {
            String serialisedMessage = page.replace("\n", "");
            String blockedChar = switch (bookCharsSettings.mode()) {
                case STRING -> getFirstBlockedChar(serialisedMessage, bookCharsSettings.charSet());
                case PATTERN -> getFirstBlockedChar(serialisedMessage, bookCharsSettings.pattern());
            };
            if (blockedChar != null) {
                e.setCancelled(true);
                String[] replacementList = {p.getName(), blockedChar};
                super.executeActions(p, searchList, replacementList, bookCharsSettings.actionsToExecute());
                return;
            }
        }
    }
}
