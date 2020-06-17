package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.Distribution;
import ru.gadjini.any2any.exception.TelegramRequestException;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.DistributionService;
import ru.gadjini.any2any.service.MessageService;
import ru.gadjini.any2any.service.UserService;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class DistributionJob {

    private static final String DISABLE_JOB = "disableJob";

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionJob.class);

    private MessageService messageService;

    private UserService userService;

    private DistributionService distributionService;

    @Autowired
    public DistributionJob(@Qualifier("limits") MessageService messageService, UserService userService, DistributionService distributionService) {
        this.messageService = messageService;
        this.userService = userService;
        this.distributionService = distributionService;
        LOGGER.debug("Distribution job started");
    }

    @PostConstruct
    public void init() {
        checkDistributions();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkDistributions() {
        LOGGER.debug("Start checkDistributions");
        if (!distributionService.isExists()) {
            disableJob();
        }
        LOGGER.debug("Finish checkDistributions");
    }

    @Scheduled(cron = "* * * * * *")
    public void distribute() {
        if (isDisabled()) {
            LOGGER.debug("Distribution job is disabled");
            return;
        }
        LOGGER.debug("Start send distributions");
        List<Distribution> distributions = distributionService.popDistributions(10);
        for (Distribution distribution : distributions) {
            try {
                sendDistribution(distribution);
            } catch (Exception ex) {
                if (userService.deadlock(ex)) {
                    LOGGER.debug("Blocked user " + ((TelegramRequestException) ex).getChatId());
                } else {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
        LOGGER.debug("Finish send distributions");
        if (distributions.isEmpty()) {
            disableJob();
        }
    }

    public boolean isDisabled() {
        String property = System.getProperty(DISABLE_JOB);
        if ("true".equals(property)) {
            return true;
        }

        return false;
    }

    private void disableJob() {
        System.setProperty(DISABLE_JOB, "true");
        LOGGER.debug("Disable distribution job");
    }

    private void sendDistribution(Distribution distribution) {
        String message = distribution.getLocalisedMessage();
        messageService.sendMessage(new SendMessageContext(distribution.getUserId(), message));
    }
}
