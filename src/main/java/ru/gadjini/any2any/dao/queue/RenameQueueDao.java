package ru.gadjini.any2any.dao.queue;

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
                    PreparedStatement ps = con.prepareStatement("INSERT INTO rename_queue(user_id, file, new_file_name, reply_to_message_id, status) " +
                            "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, renameQueueItem.getUserId());
                    ps.setObject(2, renameQueueItem.getFile().sqlObject());
                    ps.setString(3, renameQueueItem.getNewFileName());
                    ps.setInt(4, renameQueueItem.getReplyToMessageId());
                    ps.setInt(5, renameQueueItem.getStatus().getCode());

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
                        "    UPDATE rename_queue SET status = 1 WHERE id = (SELECT id FROM rename_queue WHERE status = 0 " +
                        "AND (file).size " + (weight.equals(JobWeight.LIGHT) ? "<=" : ">") + " ? ORDER BY created_at LIMIT ?) RETURNING *\n" +
                        ")\n" +
                        "SELECT *, (file).*\n" +
                        "FROM r",
                ps -> {
                    ps.setLong(1, MemoryUtils.MB_320);
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public RenameQueueItem deleteWithReturning(int id) {
        return jdbcTemplate.query(
                "WITH del AS (DELETE FROM rename_queue WHERE id = ? RETURNING *) SELECT *, (file).* FROM del",
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? map(rs) : null
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
        item.setFile(tgFile);

        item.setNewFileName(resultSet.getString(RenameQueueItem.NEW_FILE_NAME));
        item.setReplyToMessageId(resultSet.getInt(RenameQueueItem.REPLY_TO_MESSAGE_ID));
        item.setUserId(resultSet.getInt(RenameQueueItem.USER_ID));

        return item;
    }
}
