package ru.overwrite.ublocker.listeners.commands;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockType;
import ru.overwrite.ublocker.blockgroups.CommandGroup;
import ru.overwrite.ublocker.configuration.Config;
import ru.overwrite.ublocker.utils.Utils;

import java.util.List;

public class CommandHider implements Listener {

    private final UniversalBlocker plugin;
    private final Config pluginConfig;

    public CommandHider(UniversalBlocker plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent e) {
        Player p = e.getPlayer();
        if (plugin.isExcluded(p)) {
            return;
        }
        e.getCommands().removeIf(command -> {
            for (CommandGroup group : pluginConfig.getCommandBlockGroupSet()) {
                if (group.blockType() == BlockType.PATTERN) {
                    continue;
                }
                ObjectList<Action> actions = group.actionsToExecute();
                if (actions.isEmpty()) {
                    continue;
                }
                if (!shouldHideCommand(p, actions)) {
                    continue;
                }
                if (checkStringBlock(command, group)) {
                    return true;
                }
            }
            return false;
        });
    }

    private boolean checkStringBlock(String command, CommandGroup group) {
        Command comInMap = group.blockAliases() ? Bukkit.getCommandMap().getCommand(command) : null;
        List<String> aliases = comInMap != null ? comInMap.getAliases() : List.of();
        boolean matched = false;
        for (String com : group.commandsToBlockString()) {
            if (command.equalsIgnoreCase(com) || aliases.contains(com)) {
                matched = true;
                break;
            }
        }
        return group.whitelistMode() != matched;
    }

    private boolean shouldHideCommand(Player p, ObjectList<Action> actions) {
        for (Action action : actions) {
            switch (action.type()) {
                case HIDE: {
                    return true;
                }
                case LITE_HIDE: {
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