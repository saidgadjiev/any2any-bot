package ru.gadjini.any2any.dao.queue;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.RenameQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService.JobWeight;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class RenameQueueDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public RenameQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int create(RenameQueueItem renameQueueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO rename_queue(user_id, file, thumb, new_file_name, reply_to_message_id, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, renameQueueItem.getUserId());
                    ps.setObject(2, renameQueueItem.getFile().sqlObject());
                    ps.setObject(3, renameQueueItem.getThumb().sqlObject());
                    ps.setString(4, renameQueueItem.getNewFileName());
                    ps.setInt(5, renameQueueItem.getReplyToMessageId());
                    ps.setInt(6, renameQueueItem.getStatus().getCode());

                    return ps;
                },
                keyHolder
        );

        return ((Number) keyHolder.getKeys().get(RenameQueueItem.ID)).intValue();
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE rename_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public void resetProcessing() {
        jdbcTemplate.update("UPDATE rename_queue SET status = 0 WHERE status = 1");
    }

    public List<RenameQueueItem> poll(JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE rename_queue SET status = 1 WHERE id IN (SELECT id FROM rename_queue WHERE status = 0 " +
                        "AND (file).size " + (weight.equals(JobWeight.LIGHT) ? "<=" : ">") + " ? ORDER BY created_at LIMIT ?) RETURNING *\n" +
                        ")\n" +
                        "SELECT *, (file).*, (thumb).file_id as th_file_id, (thumb).file_name as th_file_name, (thumb).mime_type as th_mime_type\n" +
                        "FROM r",
                ps -> {
                    ps.setLong(1, MemoryUtils.MB_100);
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM rename_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public List<Integer> deleteByUserId(int userId) {
        return jdbcTemplate.query("WITH r as(DELETE FROM rename_queue WHERE user_id = ? RETURNING id) SELECT id FROM r",
                ps -> ps.setInt(1, userId), (rs, rowNum) -> rs.getInt("id"));
    }

    public Boolean exists(int jobId) {
        return jdbcTemplate.query("SELECT TRUE FROM rename_queue WHERE id = ?", ps -> ps.setInt(1, jobId), ResultSet::next);
    }

    private RenameQueueItem map(ResultSet resultSet) throws SQLException {
        RenameQueueItem item = new RenameQueueItem();
        item.setId(resultSet.getInt(RenameQueueItem.ID));

        TgFile tgFile = new TgFile();
        tgFile.setFileId(resultSet.getString(TgFile.FILE_ID));
        tgFile.setFileName(resultSet.getString(TgFile.FILE_NAME));
        tgFile.setMimeType(resultSet.getString(TgFile.MIME_TYPE));
        tgFile.setThumb(resultSet.getString(TgFile.THUMB));
        item.setFile(tgFile);

        String thumbFileId = resultSet.getString("th_" + TgFile.FILE_ID);
        if (StringUtils.isNotBlank(thumbFileId)) {
            TgFile thumb = new TgFile();
            thumb.setFileId(thumbFileId);
            thumb.setFileName(resultSet.getString("th_" + TgFile.FILE_NAME));
            thumb.setMimeType(resultSet.getString("th_" + TgFile.MIME_TYPE));
            item.setThumb(thumb);
        }

        item.setNewFileName(resultSet.getString(RenameQueueItem.NEW_FILE_NAME));
        item.setReplyToMessageId(resultSet.getInt(RenameQueueItem.REPLY_TO_MESSAGE_ID));
        item.setUserId(resultSet.getInt(RenameQueueItem.USER_ID));

        return item;
    }
}
