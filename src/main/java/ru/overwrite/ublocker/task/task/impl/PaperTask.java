package ru.overwrite.ublocker.task.task.impl;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import ru.overwrite.ublocker.task.task.Task;

public class PaperTask implements Task {

    private final ScheduledTask scheduledTask;

    public PaperTask(ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
    }

    @Override
    public void cancel() {
        this.scheduledTask.cancel();
    }
}
