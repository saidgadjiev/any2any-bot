package ru.gadjini.any2any.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.filter.BotFilter;
import ru.gadjini.any2any.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.any2any.model.TgMessage;
import ru.gadjini.any2any.model.bot.api.object.Update;
import ru.gadjini.any2any.service.UserService;
import ru.gadjini.any2any.service.message.MessageService;

@RestController
@RequestMapping("/bot")
public class BotController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotController.class);

    private BotFilter botFilter;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public BotController(BotFilter botFilter,
                         @Qualifier("limits") MessageService messageService,
                         UserService userService) {
        this.botFilter = botFilter;
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("/health")
    public Mono<ServerResponse> hello() {
        return ServerResponse.ok().build();
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody Update update) {
        try {
            botFilter.doFilter(update);
        } catch (UserException ex) {
            LOGGER.error(ex.getMessage(), ex);
            messageService.sendMessage(new HtmlMessage(TgMessage.getChatId(update), ex.getMessage()));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            TgMessage tgMessage = TgMessage.from(update);
            messageService.sendErrorMessage(tgMessage.getChatId(), userService.getLocaleOrDefault(tgMessage.getUser().getId()));
        }

        return ResponseEntity.ok().build();
    }
}
