package ru.gadjini.any2any.service.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.common.MessagesProperties;
import ru.gadjini.any2any.dao.command.state.CommandStateDao;
import ru.gadjini.any2any.exception.UserException;
import ru.gadjini.any2any.service.LocalisationService;
import ru.gadjini.any2any.service.UserService;

@Service
public class CommandStateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandStateService.class);

    private CommandStateDao commandStateDao;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public CommandStateService(@Qualifier("redis") CommandStateDao commandStateDao,
                               LocalisationService localisationService, UserService userService) {
        this.commandStateDao = commandStateDao;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    public void setState(long chatId, String command, Object state) {
        commandStateDao.setState(chatId, command, state);
    }

    public <T> T getState(long chatId, String command, boolean expiredCheck) {
        T state = commandStateDao.getState(chatId, command);

        if (expiredCheck && state == null) {
            LOGGER.warn("State not found({}, {})", chatId, command);
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SESSION_EXPIRED, userService.getLocaleOrDefault((int) chatId)), true);
        }

        return state;
    }

    public boolean hasState(long chatId, String command) {
        return commandStateDao.hasState(chatId, command);
    }

    public void deleteState(long chatId, String command) {
        commandStateDao.deleteState(chatId, command);
    }
}
