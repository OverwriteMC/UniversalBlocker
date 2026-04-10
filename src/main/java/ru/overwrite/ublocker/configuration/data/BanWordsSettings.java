package ru.overwrite.ublocker.configuration.data;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import ru.overwrite.ublocker.actions.Action;
import ru.overwrite.ublocker.blockgroups.BlockType;

import java.util.regex.Pattern;

public record BanWordsSettings(
        BlockType mode,
        ObjectSet<String> banWordsString,
        ObjectSet<Pattern> banWordsPattern,
        boolean strict,
        String censorSymbol,
        boolean stripColor,
        ObjectList<Action> actionsToExecute
) {
}
