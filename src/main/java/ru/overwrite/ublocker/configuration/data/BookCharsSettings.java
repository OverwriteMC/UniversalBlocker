package ru.overwrite.ublocker.configuration.data;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockType;

import java.util.regex.Pattern;

public record BookCharsSettings(
        BlockType mode,
        IntSet charSet,
        Pattern pattern,
        ObjectList<Action> actionsToExecute
) {
}
