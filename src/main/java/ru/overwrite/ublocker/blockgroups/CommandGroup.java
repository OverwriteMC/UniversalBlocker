package ru.overwrite.ublocker.blockgroups;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.conditions.Condition;

import java.util.List;
import java.util.regex.Pattern;

public record CommandGroup(
        String groupId,
        BlockType blockType,
        boolean blockAliases,
        boolean whitelistMode,
        ObjectSet<String> commandsToBlockString,
        ObjectSet<Pattern> commandsToBlockPattern,
        ObjectList<Condition> conditionsToCheck,
        ObjectList<Action> actionsToExecute
) {

    public CommandGroup(String groupId,
                        BlockType blockType,
                        boolean blockAliases,
                        boolean whitelistMode,
                        List<String> commandsToBlock,
                        ObjectList<Condition> conditionsToCheck,
                        ObjectList<Action> actionsToExecute) {
        this(
                groupId,
                blockType,
                blockAliases,
                whitelistMode,
                blockType.isString() ? GroupUtils.createStringSet(commandsToBlock) : null,
                blockType.isPattern() ? GroupUtils.createPatternSet(commandsToBlock) : null,
                conditionsToCheck,
                actionsToExecute
        );
    }
}
