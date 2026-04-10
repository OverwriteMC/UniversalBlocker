package ru.overwrite.ublocker.blockgroups;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.conditions.Condition;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public record SymbolGroup(
        String groupId,
        BlockType blockType,
        Set<BlockFactor> blockFactor,
        ObjectSet<String> symbolsToBlock,
        ObjectSet<Pattern> patternsToBlock,
        ObjectList<String> excludedCommandsString,
        ObjectList<Pattern> excludedCommandsPattern,
        ObjectList<Condition> conditionsToCheck,
        ObjectList<Action> actionsToExecute
) {

    public SymbolGroup(
            String groupId,
            BlockType blockType,
            Set<BlockFactor> blockFactor,
            List<String> symbolsToBlock,
            List<String> excludedCommand,
            ObjectList<Condition> conditionsToCheck,
            ObjectList<Action> actionsToExecute
    ) {
        this(
                groupId,
                blockType,
                blockFactor,
                blockType.isString() ? GroupUtils.createStringSet(symbolsToBlock) : null,
                blockType.isPattern() ? GroupUtils.createPatternSet(symbolsToBlock) : null,
                blockType.isString() ? GroupUtils.createStringList(excludedCommand) : null,
                blockType.isPattern() ? GroupUtils.createPatternList(excludedCommand) : null,
                conditionsToCheck,
                actionsToExecute
        );
    }
}
