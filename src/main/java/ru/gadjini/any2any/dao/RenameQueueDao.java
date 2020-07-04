package ru.gadjini.any2any.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.RenameQueueItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
                    PreparedStatement ps = con.prepareStatement("INSERT INTO rename_queue(user_id, file_id, file_name, new_file_name, mime_type, reply_to_message_id, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, renameQueueItem.getUserId());
                    ps.setString(2, renameQueueItem.getFileId());
                    ps.setString(3, renameQueueItem.getFileName());
                    ps.setString(4, renameQueueItem.getNewFileName());
                    ps.setString(5, renameQueueItem.getMimeType());
                    ps.setInt(6, renameQueueItem.getReplyToMessageId());
                    ps.setInt(7, renameQueueItem.getStatus().getCode());

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

    public RenameQueueItem poll() {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE rename_queue SET status = 1 WHERE id = (SELECT id FROM rename_queue WHERE status = 0 ORDER BY created_at LIMIT 1) RETURNING *\n" +
                        ")\n" +
                        "SELECT *\n" +
                        "FROM r",
                rs -> {
                    if (rs.next()) {
                        return map(rs);
                    }

                    return null;
                }
        );
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM rename_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    private RenameQueueItem map(ResultSet resultSet) throws SQLException {
        RenameQueueItem item = new RenameQueueItem();
        item.setId(resultSet.getInt(RenameQueueItem.ID));
        item.setFileId(resultSet.getString(RenameQueueItem.FILE_ID));
        item.setFileName(resultSet.getString(RenameQueueItem.FILE_NAME));
        item.setNewFileName(resultSet.getString(RenameQueueItem.NEW_FILE_NAME));
        item.setMimeType(resultSet.getString(RenameQueueItem.MIME_TYPE));
        item.setReplyToMessageId(resultSet.getInt(RenameQueueItem.REPLY_TO_MESSAGE_ID));
        item.setUserId(resultSet.getInt(RenameQueueItem.USER_ID));

        return item;
    }
}
