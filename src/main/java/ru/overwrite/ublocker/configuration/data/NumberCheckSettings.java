package ru.overwrite.ublocker.configuration.data;

import it.unimi.dsi.fastutil.objects.ObjectList;
import ru.overwrite.ublocker.actions.Action;

public record NumberCheckSettings(
        int maxNumbers,
        boolean strictCheck,
        boolean stripColor,
        ObjectList<Action> actionsToExecute
) {
}
