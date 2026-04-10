package ru.overwrite.ublocker.listeners.symbols;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockFactor;
import ru.overwrite.ublocker.blockgroups.SymbolGroup;
import ru.overwrite.ublocker.conditions.ConditionChecker;
import ru.overwrite.ublocker.utils.Utils;

public class ChatBlocker extends SymbolBlocker {

    public ChatBlocker(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (plugin.isExcluded(p)) {
            return;
        }
        String message = e.getMessage().toLowerCase();
        outer:
        for (SymbolGroup group : pluginConfig.getSymbolBlockGroupSet()) {
            Utils.printDebug(() -> "Group checking now: " + group.groupId(), Utils.DEBUG_SYMBOLS);
            if (group.blockFactor().isEmpty() || !group.blockFactor().contains(BlockFactor.CHAT)) {
                Utils.printDebug(() -> "Group " + group.groupId() + " does not have 'chat' block factor. Skipping...", Utils.DEBUG_SYMBOLS);
                continue;
            }
            ObjectList<Action> actions = group.actionsToExecute();
            if (actions.isEmpty()) {
                continue;
            }
            if (!ConditionChecker.isMeetsRequirements(p, group.conditionsToCheck())) {
                Utils.printDebug(() -> "Blocking does not fulfill the requirements. Skipping group...", Utils.DEBUG_SYMBOLS);
                continue;
            }
            switch (group.blockType()) {
                case STRING: {
                    if (super.checkStringBlock(e, p, message, group.symbolsToBlock(), actions)) {
                        break outer;
                    }
                    break;
                }
                case PATTERN: {
                    if (super.checkPatternBlock(e, p, message, group.patternsToBlock(), actions)) {
                        break outer;
                    }
                    break;
                }
            }
        }
    }
}
