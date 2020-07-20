package ru.gadjini.any2any.dao.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class UnzipQueueDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UnzipQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int create(UnzipQueueItem unzipQueueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        if (unzipQueueItem.getItemType() == UnzipQueueItem.ItemType.UNZIP) {
            jdbcTemplate.update(
                    con -> {
                        PreparedStatement ps = con.prepareStatement("INSERT INTO unzip_queue(user_id, file, type, status, item_type) " +
                                "VALUES (?, ?, ?, ?, 0)", Statement.RETURN_GENERATED_KEYS);

                        ps.setInt(1, unzipQueueItem.getUserId());
                        ps.setObject(2, unzipQueueItem.getFile().sqlObject());
                        ps.setString(3, unzipQueueItem.getType().name());
                        ps.setInt(4, unzipQueueItem.getStatus().getCode());

                        return ps;
                    },
                    keyHolder
            );
        } else if (unzipQueueItem.getItemType() == UnzipQueueItem.ItemType.EXTRACT_FILE){
            jdbcTemplate.update(
                    con -> {
                        PreparedStatement ps = con.prepareStatement("INSERT INTO unzip_queue(user_id, extract_file_id, message_id, status, item_type, extract_file_size) " +
                                "VALUES (?, ?, ?, 1, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                        ps.setInt(1, unzipQueueItem.getUserId());
                        ps.setObject(2, unzipQueueItem.getExtractFileId());
                        ps.setInt(3, unzipQueueItem.getMessageId());
                        ps.setInt(4, unzipQueueItem.getStatus().getCode());
                        ps.setLong(5, unzipQueueItem.getExtractFileSize());

                        return ps;
                    },
                    keyHolder
            );
        } else {
            jdbcTemplate.update(
                    con -> {
                        PreparedStatement ps = con.prepareStatement("INSERT INTO unzip_queue(user_id, message_id, status, item_type, extract_file_size) " +
                                "VALUES (?, ?, ?, 2, ?)", Statement.RETURN_GENERATED_KEYS);

                        ps.setInt(1, unzipQueueItem.getUserId());
                        ps.setInt(2, unzipQueueItem.getMessageId());
                        ps.setInt(3, unzipQueueItem.getStatus().getCode());
                        ps.setLong(4, unzipQueueItem.getExtractFileSize());

                        return ps;
                    },
                    keyHolder
            );
        }

        return ((Number) keyHolder.getKeys().get(UnzipQueueItem.ID)).intValue();
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE unzip_queue SET status = 0 WHERE id = ?",
                ps -> {
                    ps.setInt(1, id);
                });
    }

    public void resetProcessing() {
        jdbcTemplate.update("UPDATE unzip_queue SET status = 0 WHERE status = 1");
    }

    public List<UnzipQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        String sign = weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">";

        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE unzip_queue SET status = 1 WHERE id = (SELECT id FROM unzip_queue " +
                        "WHERE status = 0 AND CASE WHEN item_type = 0 THEN " +
                        "(file).size " + sign + " ? ELSE " +
                        "extract_file_size " + sign + " ? END ORDER BY created_at LIMIT " + limit + ") RETURNING *\n" +
                        ")\n" +
                        "SELECT *, (file).*\n" +
                        "FROM r",
                ps -> {
                    ps.setLong(1, MemoryUtils.MB_100);
                    ps.setLong(2, MemoryUtils.MB_100);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM unzip_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public void setMessageId(int id, int messageId) {
        jdbcTemplate.update("UPDATE unzip_queue SET message_id = ? WHERE id = ?", ps -> {
            ps.setInt(1, messageId);
            ps.setInt(2, id);
        });
    }

    public UnzipQueueItem deleteWithReturning(int id) {
        return jdbcTemplate.query("WITH del AS(DELETE FROM unzip_queue WHERE id = ? RETURNING *) SELECT *, (file).* FROM del",
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? map(rs) : null
        );
    }

    public List<Integer> deleteByUserId(int userId) {
        return jdbcTemplate.query("WITH r AS (DELETE FROM unzip_queue WHERE user_id = ? RETURNING id) SELECT id FROM r", ps -> ps.setInt(1, userId),
                (rs, rowNum) -> rs.getInt(UnzipQueueItem.ID));
    }

    public Boolean exists(int id) {
        return jdbcTemplate.query("SELECT true FROM unzip_queue WHERE id = ?", ps -> ps.setInt(1, id), ResultSet::next);
    }

    private UnzipQueueItem map(ResultSet resultSet) throws SQLException {
        UnzipQueueItem item = new UnzipQueueItem();
        item.setId(resultSet.getInt(UnzipQueueItem.ID));
        item.setUserId(resultSet.getInt(UnzipQueueItem.USER_ID));
        item.setMessageId(resultSet.getInt(UnzipQueueItem.MESSAGE_ID));
        UnzipQueueItem.ItemType itemType = UnzipQueueItem.ItemType.fromCode(resultSet.getInt(UnzipQueueItem.ITEM_TYPE));
        item.setItemType(itemType);

        if (itemType == UnzipQueueItem.ItemType.UNZIP) {
            TgFile tgFile = new TgFile();
            tgFile.setFileId(resultSet.getString(TgFile.FILE_ID));
            item.setFile(tgFile);

            item.setType(Format.valueOf(resultSet.getString(UnzipQueueItem.TYPE)));
        } else if (itemType == UnzipQueueItem.ItemType.EXTRACT_FILE) {
            item.setExtractFileId(resultSet.getInt(UnzipQueueItem.EXTRACT_FILE_ID));
            item.setExtractFileSize(resultSet.getInt(UnzipQueueItem.EXTRACT_FILE_SIZE));
        } else {
            item.setExtractFileSize(resultSet.getInt(UnzipQueueItem.EXTRACT_FILE_SIZE));

        }

        return item;
    }
}
