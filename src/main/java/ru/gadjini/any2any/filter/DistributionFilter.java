package ru.gadjini.any2any.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.any2any.domain.Distribution;
import ru.gadjini.any2any.job.DistributionJob;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.model.bot.api.object.Update;
import ru.gadjini.any2any.service.DistributionService;
import ru.gadjini.any2any.service.message.MessageService;

@Component
public class DistributionFilter extends BaseBotFilter {

    private DistributionJob distributionJob;

    private DistributionService distributionService;

    private MessageService messageService;

    @Autowired
    public DistributionFilter(DistributionJob distributionJob, DistributionService distributionService,
                              @Qualifier("limits") MessageService messageService) {
        this.distributionJob = distributionJob;
        this.distributionService = distributionService;
        this.messageService = messageService;
    }

    @Override
    public void doFilter(Update update) {
        sendDistribution(TgMessage.getUserId(update));
        super.doFilter(update);
    }

    private void sendDistribution(int userId) {
        if (distributionJob.isDisabled()) {
            return;
        }
        Distribution distribution = distributionService.popDistribution(userId);
        if (distribution != null) {
            messageService.sendMessage(new HtmlMessage((long) userId, distribution.getLocalisedMessage()));
        }
    }
}
