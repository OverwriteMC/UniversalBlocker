package ru.overwrite.ublocker.configuration.data;

import it.unimi.dsi.fastutil.objects.ObjectList;
import ru.overwrite.ublocker.actions.Action;

public record SameMessagesSettings(
        int samePercents,
        int maxSameMessage,
        int minMessageLength,
        int historySize,
        boolean stripColor,
        ObjectList<Action> actionsToExecute
) {
}
