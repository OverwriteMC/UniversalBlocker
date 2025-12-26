package ru.overwrite.ublocker.listeners.commands;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.CommandGroup;
import ru.overwrite.ublocker.conditions.ConditionChecker;
import ru.overwrite.ublocker.configuration.Config;
import ru.overwrite.ublocker.utils.Utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TabComplete implements Listener {

    private final UniversalBlocker plugin;
    private final Config pluginConfig;

    public TabComplete(UniversalBlocker plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
    }

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent e) {
        if (!(e.getSender() instanceof Player p)) {
            return;
        }
        if (plugin.isExcluded(p))
            return;
        String buffer = e.getBuffer();
        // Херня которая запрещает стилить плагины через читы, а за одно еще и предотвращает краш таб-комплитом
        if (buffer.length() > 256 || (buffer.split(" ").length == 1 && !(buffer.charAt(buffer.length() - 1) == ' ')) || !(buffer.charAt(0) == '/')) {
            Utils.printDebug(() -> "Preventing illegal tab complete action from player " + p.getName(), Utils.DEBUG_COMMANDS);
            Utils.printDebug(() -> "Tab complete buffer: " + buffer, Utils.DEBUG_COMMANDS);
            e.setCancelled(true);
            return;
        }

        Utils.printDebug(() -> "Tab complete buffer: " + buffer, Utils.DEBUG_COMMANDS);

        outer:
        for (CommandGroup group : pluginConfig.getCommandBlockGroupSet()) {
            Utils.printDebug(() -> "Group checking now: " + group.groupId(), Utils.DEBUG_COMMANDS);
            Utils.printDebug(() -> "Block type: " + group.blockType(), Utils.DEBUG_COMMANDS);

            List<Action> actions = group.actionsToExecute();
            if (actions.isEmpty()) {
                continue;
            }
            if (!ConditionChecker.isMeetsRequirements(p, group.conditionsToCheck())) {
                Utils.printDebug(() -> "Blocking does not fulfill the requirements. Skipping group...", Utils.DEBUG_COMMANDS);
                continue;
            }
            switch (group.blockType()) {
                case STRING: {
                    if (checkStringBlock(e, p, buffer, group)) {
                        break outer;
                    }
                    break;
                }
                case PATTERN: {
                    if (checkPatternBlock(e, p, buffer, group)) {
                        break outer;
                    }
                    break;
                }
            }
        }
    }

    private boolean checkStringBlock(AsyncTabCompleteEvent e, Player p, String buffer, CommandGroup group) {
        if (!shouldBlockTabComplete(p, group.actionsToExecute())) {
            return false;
        }
        String executedCommandBase = Utils.cutCommand(buffer).substring(1);
        for (String com : group.commandsToBlockString()) {
            Utils.printDebug(() -> "executedCommandBase: " + executedCommandBase, Utils.DEBUG_COMMANDS);
            Utils.printDebug(() -> "Checking command: " + com, Utils.DEBUG_COMMANDS);
            Command comInMap = group.blockAliases() ? Bukkit.getCommandMap().getCommand(executedCommandBase) : null;
            List<String> aliases = comInMap != null ? comInMap.getAliases() : List.of();
            if (!aliases.isEmpty() && !aliases.contains(comInMap.getName())) {
                aliases.add(comInMap.getName());
            }
            boolean check = executedCommandBase.equalsIgnoreCase(com) || aliases.contains(com);
            check = group.whitelistMode() != check;
            boolean finalCheck = check;
            Utils.printDebug(() -> "Final check '" + finalCheck + "'", Utils.DEBUG_COMMANDS);
            if (check) {
                Utils.printDebug(() -> "Tab complete blocked by string match for player '" + p.getName() + "'. Command: " + com, Utils.DEBUG_COMMANDS);
                e.setCancelled(true);
                return true;
            }
        }
        return false;
    }

    private boolean checkPatternBlock(AsyncTabCompleteEvent e, Player p, String buffer, CommandGroup group) {
        if (!shouldBlockTabComplete(p, group.actionsToExecute())) {
            return false;
        }
        for (Pattern pattern : group.commandsToBlockPattern()) {
            Matcher matcher = pattern.matcher(buffer);
            boolean check = matcher.matches();
            check = group.whitelistMode() != check;
            if (check) {
                Utils.printDebug(() -> "Tab complete blocked by pattern match for player '" + p.getName() + "'. Pattern: " + pattern.pattern(), Utils.DEBUG_COMMANDS);
                e.setCancelled(true);
                return true;
            }
        }
        return false;
    }

    private boolean shouldBlockTabComplete(Player p, List<Action> actions) {
        for (Action action : actions) {
            switch (action.type()) {
                case BLOCK_TAB_COMPLETE: {
                    return true;
                }
                case LITE_BLOCK_TAB_COMPLETE: {
                    String perm = Utils.getPermOrDefault(
                            Utils.extractValue(action.context(), Utils.PERM_PREFIX, "}"),
                            "ublocker.bypass.commands");
                    return !p.hasPermission(perm);
                }
                default:
                    break;
            }
        }
        return false;
    }
}