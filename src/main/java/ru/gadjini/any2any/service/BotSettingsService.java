package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.BotSettingsDao;
import ru.gadjini.any2any.model.Any2AnyFile;

@Service
public class BotSettingsService {

    private BotSettingsDao botSettingsDao;

    @Autowired
    public BotSettingsService(BotSettingsDao botSettingsDao) {
        this.botSettingsDao = botSettingsDao;
    }

    public void deleteThumbnail(long chatId) {
        botSettingsDao.deleteThumb(chatId);
    }

    public void setThumbnail(long chatId, Any2AnyFile thumb) {
        botSettingsDao.setThumb(chatId, thumb);
    }

    public Any2AnyFile getThumbnail(long chatId) {
        return botSettingsDao.getThumb(chatId);
    }
}
