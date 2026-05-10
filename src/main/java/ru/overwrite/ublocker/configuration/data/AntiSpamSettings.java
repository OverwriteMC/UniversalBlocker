package ru.overwrite.ublocker.configuration.data;

import it.unimi.dsi.fastutil.objects.ObjectList;
import ru.overwrite.ublocker.actions.Action;

public record AntiSpamSettings(
        long cooldown,
        ObjectList<Action> actionsToExecute
) {
}
