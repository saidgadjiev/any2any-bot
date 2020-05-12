package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.FileReportDao;
import ru.gadjini.any2any.domain.FileReport;

@Service
public class FileReportService {

    private FileReportDao fileReportDao;

    @Autowired
    public FileReportService(FileReportDao fileReportDao) {
        this.fileReportDao = fileReportDao;
    }

    public void createReport(int userId, int queueItemId) {
        FileReport fileReport = new FileReport();
        fileReport.setUserId(userId);
        fileReport.setQueueItemId(queueItemId);
        fileReportDao.create(fileReport);
    }
}
