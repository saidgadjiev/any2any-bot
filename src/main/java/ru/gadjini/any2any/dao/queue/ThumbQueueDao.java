package ru.gadjini.any2any.dao.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.RenameQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.domain.ThumbQueueItem;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService.JobWeight;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class ThumbQueueDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ThumbQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int create(ThumbQueueItem thumbQueueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO thumb_queue(user_id, file, thumb, reply_to_message_id, status) " +
                            "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, thumbQueueItem.getUserId());
                    ps.setObject(2, thumbQueueItem.getFile().sqlObject());
                    ps.setObject(3, thumbQueueItem.getThumb().sqlObject());
                    ps.setInt(4, thumbQueueItem.getReplyToMessageId());
                    ps.setInt(5, thumbQueueItem.getStatus().getCode());

                    return ps;
                },
                keyHolder
        );

        return ((Number) keyHolder.getKeys().get(RenameQueueItem.ID)).intValue();
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE thumb_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public void resetProcessing() {
        jdbcTemplate.update("UPDATE thumb_queue SET status = 0 WHERE status = 1");
    }

    public List<ThumbQueueItem> poll(JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE thumb_queue SET status = 1 WHERE id = (SELECT id FROM thumb_queue WHERE status = 0 " +
                        "AND (file).size " + (weight.equals(JobWeight.LIGHT) ? "<=" : ">") + " ? ORDER BY created_at LIMIT ?) RETURNING *\n" +
                        ")\n" +
                        "SELECT *, (file).*, (thumb).file_id as th_file_id, (thumb).mime_type as th_mime_type, (thumb).file_name as th_file_name, (thumb).size as th_size, (thumb).thumb as th_thumb\n" +
                        "FROM r",
                ps -> {
                    ps.setLong(1, MemoryUtils.MB_100);
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public ThumbQueueItem deleteWithReturning(int id) {
        return jdbcTemplate.query(
                "WITH del AS (DELETE FROM thumb_queue WHERE id = ? RETURNING *) SELECT *, (file).*, " +
                        "(thumb).file_id as th_file_id, (thumb).mime_type as th_mime_type, (thumb).file_name as th_file_name, (thumb).size as th_size, (thumb).thumb as th_thumb FROM del",
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? map(rs) : null
        );
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM thumb_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public List<Integer> deleteByUserId(int userId) {
        return jdbcTemplate.query("WITH r as(DELETE FROM thumb_queue WHERE user_id = ? RETURNING id) SELECT id FROM r",
                ps -> ps.setInt(1, userId), (rs, rowNum) -> rs.getInt("id"));
    }

    public Boolean exists(int jobId) {
        return jdbcTemplate.query("SELECT TRUE FROM thumb_queue WHERE id = ?", ps -> ps.setInt(1, jobId), ResultSet::next);
    }

    private ThumbQueueItem map(ResultSet resultSet) throws SQLException {
        ThumbQueueItem item = new ThumbQueueItem();
        item.setId(resultSet.getInt(RenameQueueItem.ID));

        TgFile tgFile = new TgFile();
        tgFile.setFileId(resultSet.getString(TgFile.FILE_ID));
        tgFile.setFileName(resultSet.getString(TgFile.FILE_NAME));
        tgFile.setMimeType(resultSet.getString(TgFile.MIME_TYPE));
        tgFile.setThumb(resultSet.getString(TgFile.THUMB));
        item.setFile(tgFile);

        TgFile thumbFile = new TgFile();
        thumbFile.setFileId(resultSet.getString("th_" + TgFile.FILE_ID));
        thumbFile.setFileName(resultSet.getString("th_" + TgFile.FILE_NAME));
        thumbFile.setMimeType(resultSet.getString("th_" + TgFile.MIME_TYPE));
        thumbFile.setThumb(resultSet.getString("th_" + TgFile.THUMB));
        item.setThumb(thumbFile);

        item.setReplyToMessageId(resultSet.getInt(RenameQueueItem.REPLY_TO_MESSAGE_ID));
        item.setUserId(resultSet.getInt(RenameQueueItem.USER_ID));

        return item;
    }
}
