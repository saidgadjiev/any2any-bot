package ru.gadjini.any2any.dao;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.FileQueueItem;
import ru.gadjini.any2any.service.converter.api.Format;

import java.sql.Types;
import java.util.List;

@Repository
public class FileQueueDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public FileQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void add(FileQueueItem queueItem) {
        jdbcTemplate.query(
                "INSERT INTO file_queue (user_id, file_id, format, size, message_id, file_name, target_format, mime_type)\n" +
                        "    VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *",
                ps -> {
                    ps.setInt(1, queueItem.getUserId());
                    ps.setString(2, queueItem.getFileId());
                    ps.setString(3, queueItem.getFormat().name());
                    ps.setInt(4, queueItem.getSize());
                    ps.setInt(5, queueItem.getMessageId());
                    if (queueItem.getFileName() != null) {
                        ps.setString(6, queueItem.getFileName());
                    } else {
                        ps.setNull(6, Types.VARCHAR);
                    }
                    ps.setString(7, queueItem.getTargetFormat().name());
                    if (StringUtils.isNotBlank(queueItem.getMimeType())) {
                        ps.setString(8, queueItem.getMimeType());
                    } else {
                        ps.setNull(8, Types.VARCHAR);
                    }
                },
                rs -> {
                    if (rs.next()) {
                        queueItem.setId(rs.getInt(FileQueueItem.ID));
                    }

                    return null;
                }
        );
    }

    public int getPlaceInQueue(int id) {
        return jdbcTemplate.query(
                "SELECT place_in_queue\n" +
                        "FROM (SELECT id, row_number() over (ORDER BY created_at) AS place_in_queue FROM file_queue) as file_q\n" +
                        "WHERE id = ?",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        return rs.getInt(FileQueueItem.PLACE_IN_QUEUE);
                    }

                    return 0;
                }
        );
    }

    public List<FileQueueItem> getItems(int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM file_queue ORDER BY created_at LIMIT " + limit,
                (rs, rowNum) -> {
                    FileQueueItem fileQueueItem = new FileQueueItem();

                    fileQueueItem.setId(rs.getInt(FileQueueItem.ID));
                    fileQueueItem.setMessageId(rs.getInt(FileQueueItem.MESSAGE_ID));
                    fileQueueItem.setFileName(rs.getString(FileQueueItem.FILE_NAME));
                    fileQueueItem.setFileId(rs.getString(FileQueueItem.FILE_ID));
                    fileQueueItem.setUserId(rs.getInt(FileQueueItem.USER_ID));
                    fileQueueItem.setFormat(Format.valueOf(rs.getString(FileQueueItem.FORMAT)));
                    fileQueueItem.setTargetFormat(Format.valueOf(rs.getString(FileQueueItem.TARGET_FORMAT)));

                    return fileQueueItem;
                }
        );
    }

    public void delete(int id) {
        jdbcTemplate.update(
                "DELETE FROM file_queue WHERE id = ?",
                ps -> ps.setInt(1, id)
        );
    }
}
