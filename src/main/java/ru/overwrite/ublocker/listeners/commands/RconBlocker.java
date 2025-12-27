package ru.overwrite.ublocker.listeners.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.RemoteServerCommandEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.actions.ActionType;
import ru.overwrite.ublocker.blockgroups.CommandGroup;
import ru.overwrite.ublocker.color.ColorizerProvider;
import ru.overwrite.ublocker.configuration.Config;
import ru.overwrite.ublocker.utils.Utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RconBlocker implements Listener {

    private final UniversalBlocker plugin;
    private final Config pluginConfig;

    public static boolean FULL_LOCK;

    public RconBlocker(UniversalBlocker plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
    }

    @EventHandler
    public void onRconCommand(RemoteServerCommandEvent e) {
        if (FULL_LOCK) {
            e.setCancelled(true);
            return;
        }
        String command = e.getCommand().toLowerCase();
        outer:
        for (CommandGroup group : pluginConfig.getCommandBlockGroupSet()) {
            Utils.printDebug(() -> "Group checking now: " + group.groupId(), Utils.DEBUG_COMMANDS);
            Utils.printDebug(() -> "Block type: " + group.blockType(), Utils.DEBUG_COMMANDS);
            List<Action> actions = group.actionsToExecute();
            if (actions.isEmpty()) {
                continue;
            }
            switch (group.blockType()) {
                case STRING: {
                    if (checkStringBlock(e, command, group)) {
                        break outer;
                    }
                    break;
                }
                case PATTERN: {
                    if (checkPatternBlock(e, command, group)) {
                        break outer;
                    }
                    break;
                }
            }
        }
    }

    private boolean checkStringBlock(RemoteServerCommandEvent e, String command, CommandGroup group) {
        String executedCommandBase = Utils.cutCommand(command);
        Command comInMap = group.blockAliases() ? Bukkit.getCommandMap().getCommand(executedCommandBase) : null;
        List<String> aliases = comInMap != null ? comInMap.getAliases() : List.of();
        if (!aliases.isEmpty() && !aliases.contains(comInMap.getName())) {
            aliases.add(comInMap.getName());
        }
        for (String com : group.commandsToBlockString()) {
            boolean check = com.equalsIgnoreCase(executedCommandBase) || aliases.contains(com);
            check = group.whitelistMode() != check;
            if (check) {
                executeActions(e, command, executedCommandBase, group.actionsToExecute());
                return true;
            }
        }
        return false;
    }

    private boolean checkPatternBlock(RemoteServerCommandEvent e, String command, CommandGroup group) {
        String executedCommandBase = Utils.cutCommand(command);
        for (Pattern pattern : group.commandsToBlockPattern()) {
            Matcher matcher = pattern.matcher(executedCommandBase);
            boolean check = matcher.matches();
            check = group.whitelistMode() != check;
            if (check) {
                executeActions(e, command, matcher.group(), group.actionsToExecute());
                return true;
            }
        }
        return false;
    }

    private static final String[] searchList = {"%player%", "%command%", "%msg%"};

    public boolean executeActions(Cancellable e, String fullCommand, String baseCommand, List<Action> actions) {
        Utils.printDebug(() -> "Starting executing actions for rcon and blocked command '" + baseCommand + "'", Utils.DEBUG_COMMANDS);
        final String[] replacementList = {"RCON", baseCommand, fullCommand};

        for (Action action : actions) {
            ActionType type = action.type();

            if (type == ActionType.BLOCK_RCON) {
                Utils.printDebug(() -> "Command event blocked for rcon", Utils.DEBUG_COMMANDS);
                e.setCancelled(true);
            }

            if (e.isCancelled()) {
                switch (type) {
                    case LOG: {
                        logAction(action, replacementList);
                        break;
                    }
                    case NOTIFY_CONSOLE: {
                        sendNotifyConsole(action, replacementList);
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        return e.isCancelled();
    }

    private void logAction(Action action, String[] replacementList) {
        String logMessage = Utils.extractMessage(action.context(), Utils.FILE_MARKER);
        String file = Utils.extractValue(action.context(), Utils.FILE_PREFIX, "}");
        plugin.logAction(Utils.replaceEach(logMessage, searchList, replacementList), file);
    }

    private void sendNotifyConsole(Action action, String[] replacementList) {
        String formattedMessage = formatActionMessage(action, replacementList);
        Bukkit.getConsoleSender().sendMessage(formattedMessage);
    }

    private String formatActionMessage(Action action, String[] replacementList) {
        return Utils.replaceEach(ColorizerProvider.COLORIZER.colorize(action.context()), searchList, replacementList);
    }
}
