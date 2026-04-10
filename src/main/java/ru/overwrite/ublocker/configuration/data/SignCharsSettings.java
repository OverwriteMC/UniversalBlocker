package ru.overwrite.ublocker.configuration.data;

import it.unimi.dsi.fastutil.chars.CharSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockType;

import java.util.regex.Pattern;

public record SignCharsSettings(
        BlockType mode,
        CharSet charSet,
        Pattern pattern,
        ObjectList<Action> actionsToExecute
) {
}
