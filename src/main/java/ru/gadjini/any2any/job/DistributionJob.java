package ru.gadjini.any2any.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.Distribution;
import ru.gadjini.any2any.model.SendMessageContext;
import ru.gadjini.any2any.service.DistributionService;
import ru.gadjini.any2any.service.MessageService;

import java.util.List;

@Component
public class DistributionJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionJob.class);

    private MessageService messageService;

    private DistributionService distributionService;

    @Autowired
    public DistributionJob(@Qualifier("limits") MessageService messageService, DistributionService distributionService) {
        this.messageService = messageService;
        this.distributionService = distributionService;
        LOGGER.debug("Distribution job started");
    }

    @Scheduled(cron = "* * * * * *")
    public void distribute() {
        List<Distribution> distributions = distributionService.popDistributions(10);
        for (Distribution distribution : distributions) {
            try {
                sendDistribution(distribution);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    private void sendDistribution(Distribution distribution) {
        String message = distribution.getLocalisedMessage();
        messageService.sendMessage(new SendMessageContext(distribution.getUserId(), message));
    }
}
