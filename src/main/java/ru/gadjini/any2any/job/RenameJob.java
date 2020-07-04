package ru.gadjini.any2any.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RenameJob {

    @Scheduled(cron = "*/5 * * * * *")
    public void processRenameTasks() {

    }
}
