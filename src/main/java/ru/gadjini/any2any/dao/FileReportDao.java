package ru.gadjini.any2any.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.FileReport;

@Repository
public class FileReportDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public FileReportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(FileReport fileReport) {
        jdbcTemplate.update(
                "INSERT INTO file_report(user_id, queue_item_id) VALUES (?, ?) ON CONFLICT (user_id, queue_item_id) DO NOTHING",
                ps -> {
                    ps.setInt(1, fileReport.getUserId());
                    ps.setInt(2, fileReport.getQueueItemId());
                }
        );
    }
}
