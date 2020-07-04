package ru.gadjini.any2any.dao;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.ConversionQueueItem;
import ru.gadjini.any2any.domain.TgUser;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.utils.JdbcUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static ru.gadjini.any2any.domain.ConversionQueueItem.TYPE;

@Repository
public class ConversionQueueDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ConversionQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void add(ConversionQueueItem queueItem) {
        jdbcTemplate.query(
                "INSERT INTO " + TYPE + " (user_id, file_id, format, size, reply_to_message_id, file_name, target_format, mime_type, status)\n" +
                        "    VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING *",
                ps -> {
                    ps.setInt(1, queueItem.getUserId());
                    ps.setString(2, queueItem.getFileId());
                    ps.setString(3, queueItem.getFormat().name());
                    ps.setInt(4, queueItem.getSize());
                    ps.setInt(5, queueItem.getReplyToMessageId());
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
                    ps.setInt(9, queueItem.getStatus().getCode());
                },
                rs -> {
                    if (rs.next()) {
                        queueItem.setId(rs.getInt(ConversionQueueItem.ID));
                    }

                    return null;
                }
        );
    }

    public Integer getPlaceInQueue(int id) {
        return jdbcTemplate.query(
                "SELECT place_in_queue\n" +
                        "FROM (SELECT id, row_number() over (ORDER BY created_at) AS place_in_queue FROM "
                        + TYPE + " WHERE status = 0) as file_q\n" +
                        "WHERE id = ?",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        return rs.getInt(ConversionQueueItem.PLACE_IN_QUEUE);
                    }

                    return 0;
                }
        );
    }

    public List<ConversionQueueItem> takeItems(int limit) {
        return jdbcTemplate.query(
                "WITH queue_items AS (\n" +
                        "    UPDATE " + TYPE + " SET status = 1, last_run_at = now(), " +
                        "started_at = COALESCE(started_at, now()) WHERE id IN (\n" +
                        "        SELECT id\n" +
                        "        FROM " + TYPE + " WHERE status = 0\n" +
                        "        ORDER BY created_at\n" +
                        "        LIMIT " + limit + ")\n" +
                        "    RETURNING *\n" +
                        ")\n" +
                        "SELECT *\n" +
                        "FROM queue_items\n" +
                        "ORDER BY created_at;",
                (rs, rowNum) -> map(rs)
        );
    }

    public void resetProcessing() {
        jdbcTemplate.update(
                "UPDATE " + TYPE + " SET status = 0 WHERE status = 1"
        );
    }

    public void updateException(int id, int status, String exception) {
        jdbcTemplate.update(
                "UPDATE " + TYPE + " SET exception = ?, status = ? WHERE id = ?",
                ps -> {
                    ps.setString(1, exception);
                    ps.setInt(2, status);
                    ps.setInt(3, id);
                }
        );
    }

    public void updateCompletedAt(int id, int status) {
        jdbcTemplate.update(
                "UPDATE " + TYPE + " SET status = ?, completed_at = now() WHERE id = ?",
                ps -> {
                    ps.setInt(1, status);
                    ps.setInt(2, id);
                }
        );
    }

    public void delete(int id) {
        jdbcTemplate.update(
                "DELETE FROM " + TYPE + " WHERE id = ?",
                ps -> ps.setInt(1, id)
        );
    }

    public ConversionQueueItem getById(int id) {
        return jdbcTemplate.query(
                "SELECT f.*, queue_place.place_in_queue\n" +
                        "FROM " + TYPE + " f\n" +
                        "         LEFT JOIN (SELECT id, row_number() over (ORDER BY created_at) as place_in_queue\n" +
                        "                     FROM " + TYPE + "\n" +
                        "                     WHERE status = 0) queue_place ON f.id = queue_place.id\n" +
                        "WHERE f.id = ?\n",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        return map(rs);
                    }

                    return null;
                }
        );
    }

    public List<ConversionQueueItem> getActiveQueries(int userId) {
        return jdbcTemplate.query(
                "SELECT f.*, queue_place.place_in_queue\n" +
                        "FROM " + TYPE + " f\n" +
                        "         LEFT JOIN (SELECT id, row_number() over (ORDER BY created_at) as place_in_queue\n" +
                        "                     FROM " + TYPE + "\n" +
                        "                     WHERE status = 0) queue_place ON f.id = queue_place.id\n" +
                        "WHERE user_id = ?\n" +
                        "  AND status IN (0, 1, 2)",
                ps -> ps.setInt(1, userId),
                (rs, rowNum) -> map(rs)
        );
    }

    private ConversionQueueItem map(ResultSet rs) throws SQLException {
        Set<String> columns = JdbcUtils.getColumnNames(rs.getMetaData());
        ConversionQueueItem fileQueueItem = new ConversionQueueItem();

        fileQueueItem.setId(rs.getInt(ConversionQueueItem.ID));
        fileQueueItem.setReplyToMessageId(rs.getInt(ConversionQueueItem.REPLY_TO_MESSAGE_ID));
        fileQueueItem.setFileName(rs.getString(ConversionQueueItem.FILE_NAME));
        fileQueueItem.setFileId(rs.getString(ConversionQueueItem.FILE_ID));
        fileQueueItem.setUserId(rs.getInt(ConversionQueueItem.USER_ID));

        TgUser user = new TgUser();
        user.setUserId(fileQueueItem.getUserId());
        fileQueueItem.setUser(user);

        fileQueueItem.setFormat(Format.valueOf(rs.getString(ConversionQueueItem.FORMAT)));
        fileQueueItem.setTargetFormat(Format.valueOf(rs.getString(ConversionQueueItem.TARGET_FORMAT)));
        fileQueueItem.setSize(rs.getInt(ConversionQueueItem.SIZE));
        fileQueueItem.setMessage(rs.getString(ConversionQueueItem.MESSAGE));
        Timestamp lastRunAt = rs.getTimestamp(ConversionQueueItem.LAST_RUN_AT);
        if (lastRunAt != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(lastRunAt.toLocalDateTime(), ZoneOffset.UTC);
            fileQueueItem.setLastRunAt(zonedDateTime);
        }
        fileQueueItem.setStatus(ConversionQueueItem.Status.fromCode(rs.getInt(ConversionQueueItem.STATUS)));
        if (columns.contains(ConversionQueueItem.PLACE_IN_QUEUE)) {
            fileQueueItem.setPlaceInQueue(rs.getInt(ConversionQueueItem.PLACE_IN_QUEUE));
        }

        return fileQueueItem;
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE conversion_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

}
