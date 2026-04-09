package ru.overwrite.ublocker.listeners.symbols;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockFactor;
import ru.overwrite.ublocker.blockgroups.SymbolGroup;
import ru.overwrite.ublocker.conditions.ConditionChecker;
import ru.overwrite.ublocker.utils.Utils;

import java.util.List;

public class AnvilBlocker extends SymbolBlocker {

    public AnvilBlocker(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent e) {
        if (e.getInventory().getType() != InventoryType.ANVIL || e.getSlot() != 2) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        if (plugin.isExcluded(p)) {
            return;
        }
        ItemStack resultItem = e.getCurrentItem();
        ItemMeta itemMeta;
        if (resultItem == null || (itemMeta = resultItem.getItemMeta()) != null || !itemMeta.hasDisplayName()) {
            return;
        }
        String name = itemMeta.getDisplayName();
        outer:
        for (SymbolGroup group : pluginConfig.getSymbolBlockGroupSet()) {
            Utils.printDebug(() -> "Group checking now: " + group.groupId(), Utils.DEBUG_SYMBOLS);
            if (group.blockFactor().isEmpty() || !group.blockFactor().contains(BlockFactor.ANVIL)) {
                Utils.printDebug(() -> "Group " + group.groupId() + " does not have 'anvil' block factor. Skipping...", Utils.DEBUG_SYMBOLS);
                continue;
            }
            List<Action> actions = group.actionsToExecute();
            if (actions.isEmpty()) {
                continue;
            }
            if (!ConditionChecker.isMeetsRequirements(p, group.conditionsToCheck())) {
                Utils.printDebug(() -> "Blocking does not fulfill the requirements. Skipping group...", Utils.DEBUG_SYMBOLS);
                continue;
            }
            switch (group.blockType()) {
                case STRING: {
                    if (super.checkStringBlock(e, p, name, group.symbolsToBlock(), actions)) {
                        break outer;
                    }
                    break;
                }
                case PATTERN: {
                    if (super.checkPatternBlock(e, p, name, group.patternsToBlock(), actions)) {
                        break outer;
                    }
                    break;
                }
            }
        }
    }
}