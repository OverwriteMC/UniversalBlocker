package ru.overwrite.ublocker.configuration.data;

import it.unimi.dsi.fastutil.objects.ObjectList;
import ru.overwrite.ublocker.actions.Action;

public record CaseCheckSettings(
        int maxUpperCasePercent,
        boolean strictCheck,
        ObjectList<Action> actionsToExecute
) {
}