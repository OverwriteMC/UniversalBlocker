package ru.overwrite.ublocker.listeners.symbols;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockFactor;
import ru.overwrite.ublocker.blockgroups.SymbolGroup;
import ru.overwrite.ublocker.conditions.ConditionChecker;
import ru.overwrite.ublocker.utils.Utils;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignBlocker extends SymbolBlocker {

    public SignBlocker(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSign(SignChangeEvent e) {
        Player p = e.getPlayer();
        if (plugin.isExcluded(p)) {
            return;
        }
        String line0 = e.getLine(0).toLowerCase();
        String line1 = e.getLine(1).toLowerCase();
        String line2 = e.getLine(2).toLowerCase();
        String line3 = e.getLine(3).toLowerCase();
        String combined = line0 + line1 + line2 + line3;
        outer:
        for (SymbolGroup group : pluginConfig.getSymbolBlockGroupSet()) {
            Utils.printDebug(() -> "Group checking now: " + group.groupId(), Utils.DEBUG_SYMBOLS);
            if (group.blockFactor().isEmpty() || !group.blockFactor().contains(BlockFactor.SIGN)) {
                Utils.printDebug(() -> "Group " + group.groupId() + " does not have 'sign' block factor. Skipping...", Utils.DEBUG_SYMBOLS);
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
                    if (checkStringBlock(e, p, combined, group.symbolsToBlock(), actions)) {
                        break outer;
                    }
                    break;
                }
                case PATTERN: {
                    if (checkPatternBlock(e, p, combined, group.patternsToBlock(), actions)) {
                        break outer;
                    }
                    break;
                }
            }
        }
    }

    private boolean checkStringBlock(SignChangeEvent e, Player p, String combined, Set<String> symbolsToBlock, List<Action> actions) {
        for (String symbol : symbolsToBlock) {
            if (combined.contains(symbol)) {
                Utils.printDebug(() -> "Sign message '" + combined + "' contains blocked symbol" + symbol + ". (String)", Utils.DEBUG_SYMBOLS);
                super.executeActions(e, p, combined, symbol, actions);
                return true;
            }
        }
        return false;
    }

    private boolean checkPatternBlock(SignChangeEvent e, Player p, String combined, Set<Pattern> patternsToBlock, List<Action> actions) {
        for (Pattern pattern : patternsToBlock) {
            Matcher matcher = pattern.matcher(combined.replace("\n", ""));
            if (matcher.find()) {
                Utils.printDebug(() -> "Sign message '" + combined + "' contains blocked symbol" + matcher.group() + ". (Patten)", Utils.DEBUG_SYMBOLS);
                super.executeActions(e, p, combined, matcher.group(), actions);
                return true;
            }
        }
        return false;
    }
}
