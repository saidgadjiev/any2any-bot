package ru.gadjini.any2any.service.command;

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

    public void setState(long chatId, Object state) {
        commandStateDao.setState(chatId, state);
    }

    public <T> T getState(long chatId, boolean expiredCheck) {
        T state = commandStateDao.getState(chatId);

        if (expiredCheck && state == null) {
            throw new UserException(localisationService.getMessage(MessagesProperties.MESSAGE_SESSION_EXPIRED, userService.getLocaleOrDefault((int) chatId)));
        }

        return state;
    }

    public boolean hasState(long chatId) {
        return commandStateDao.hasState(chatId);
    }

    public void deleteState(long chatId) {
        commandStateDao.deleteState(chatId);
    }
}
