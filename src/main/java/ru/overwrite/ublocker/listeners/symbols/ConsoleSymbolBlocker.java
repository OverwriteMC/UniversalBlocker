package ru.overwrite.ublocker.listeners.symbols;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import ru.overwrite.ublocker.UniversalBlocker;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.actions.ActionType;
import ru.overwrite.ublocker.blockgroups.BlockFactor;
import ru.overwrite.ublocker.blockgroups.SymbolGroup;
import ru.overwrite.ublocker.color.ColorizerProvider;
import ru.overwrite.ublocker.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleSymbolBlocker extends SymbolBlocker {

    public ConsoleSymbolBlocker(UniversalBlocker plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(ServerCommandEvent e) {
        String command = e.getCommand().toLowerCase();
        boolean checkRcon = e instanceof RemoteServerCommandEvent;
        BlockFactor checkType = checkRcon ? BlockFactor.RCON_COMMAND : BlockFactor.CONSOLE_COMMAND;
        outer:
        for (SymbolGroup group : pluginConfig.getSymbolBlockGroupSet()) {
            Utils.printDebug(() -> "Group checking now: " + group.groupId(), Utils.DEBUG_SYMBOLS);
            if (group.blockFactor().isEmpty() || !group.blockFactor().contains(checkType)) {
                Utils.printDebug(() -> "Group " + group.groupId() + " does not have 'console_command' or 'rcon_command' block factor. Skipping...", Utils.DEBUG_SYMBOLS);
                continue;
            }
            ObjectList<Action> actions = group.actionsToExecute();
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

    private boolean checkStringBlock(ServerCommandEvent e, String command, SymbolGroup group) {
        if (startWithExcludedString(Utils.cutCommand(command), group.excludedCommandsString())) {
            return false;
        }
        for (String symbol : group.symbolsToBlock()) {
            if (command.contains(symbol)) {
                Utils.printDebug(() -> "Command '" + command + "' contains blocked symbol" + symbol + ". (String)", Utils.DEBUG_SYMBOLS);
                ObjectList<Action> actions = group.actionsToExecute();
                this.executeActions(e, command, symbol, actions);
                return true;
            }
        }
        return false;
    }

    private boolean checkPatternBlock(ServerCommandEvent e, String command, SymbolGroup group) {
        if (startWithExcludedPattern(Utils.cutCommand(command), group.excludedCommandsPattern())) {
            return false;
        }
        for (Pattern pattern : group.patternsToBlock()) {
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                Utils.printDebug(() -> "Command '" + command + "' contains blocked symbol" + matcher.group() + ". (Pattern)", Utils.DEBUG_SYMBOLS);
                ObjectList<Action> actions = group.actionsToExecute();
                this.executeActions(e, command, matcher.group(), actions);
                return true;
            }
        }
        return false;
    }

    private static final String[] searchList = {"%player%", "%symbol%", "%msg%"};

    private void executeActions(Cancellable e, String command, String symbol, ObjectList<Action> actions) {
        Utils.printDebug(() -> "Starting executing actions for console/rcon and blocked symbol '" + symbol + "' (COMMAND)", Utils.DEBUG_SYMBOLS);
        final String[] replacementList = {"CONSOLE", symbol, command};

        for (Action action : actions) {
            ActionType type = action.type();

            if (shouldBlockAction(type)) {
                Utils.printDebug(() -> "Command event blocked for rcon", Utils.DEBUG_SYMBOLS);
                e.setCancelled(true);
                continue;
            }

            if (e.isCancelled()) {
                if (type == ActionType.LOG) {
                    logAction(action, replacementList);
                }
                if (type == ActionType.NOTIFY_CONSOLE) {
                    sendNotifyConsole(action, replacementList);
                }
            }
        }
    }

    private boolean shouldBlockAction(ActionType type) {
        return type == ActionType.BLOCK;
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
