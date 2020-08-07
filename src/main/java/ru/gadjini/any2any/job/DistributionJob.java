package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.Distribution;
import ru.gadjini.any2any.exception.botapi.TelegramApiRequestException;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.service.DistributionService;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

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
    public DistributionJob(@Qualifier("messagelimits") MessageService messageService, UserService userService, DistributionService distributionService) {
        this.messageService = messageService;
        this.userService = userService;
        this.distributionService = distributionService;
        LOGGER.debug("Distribution job started");
    }

    @PostConstruct
    public void init() {
        checkDistributions();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void checkDistributions() {
        LOGGER.debug("Start checkDistributions");
        if (!distributionService.isExists()) {
            disableJob();
        } else {
            enableJob();
        }
        LOGGER.debug("Finish checkDistributions");
    }

    //@Scheduled(cron = "0 */5 * * * *")
    public void distribute() {
        if (isDisabled()) {
            return;
        }
        LOGGER.debug("Start distribute");
        List<Distribution> distributions = distributionService.popDistributions(15);
        for (Distribution distribution : distributions) {
            try {
                sendDistribution(distribution);
            } catch (Exception ex) {
                if (userService.deadlock(ex)) {
                    LOGGER.debug("Blocked user({})", ((TelegramApiRequestException) ex).getChatId());
                } else {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
        LOGGER.debug("Finish distribute");
        if (distributions.isEmpty()) {
            disableJob();
        }
    }

    public boolean isDisabled() {
        String property = System.getProperty(DISABLE_JOB);

        return "true".equals(property);
    }

    private void enableJob() {
        System.setProperty(DISABLE_JOB, "false");
        LOGGER.debug("Enable");
    }

    private void disableJob() {
        System.setProperty(DISABLE_JOB, "true");
        LOGGER.debug("Disable");
    }

    private void sendDistribution(Distribution distribution) {
        String message = distribution.getLocalisedMessage();
        messageService.sendMessage(new HtmlMessage((long) distribution.getUserId(), message));
    }
}
