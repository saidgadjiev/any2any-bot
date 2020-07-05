package ru.gadjini.any2any.dao.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.domain.UnzipQueueItem;
import ru.gadjini.any2any.service.conversion.api.Format;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Repository
public class UnzipQueueDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UnzipQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int create(UnzipQueueItem unzipQueueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO unzip_queue(user_id, file, type, status) " +
                            "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, unzipQueueItem.getUserId());
                    ps.setObject(2, unzipQueueItem.getFile().sqlObject());
                    ps.setString(3, unzipQueueItem.getType().name());
                    ps.setInt(4, unzipQueueItem.getStatus().getCode());

                    return ps;
                },
                keyHolder
        );

        return ((Number) keyHolder.getKeys().get(UnzipQueueItem.ID)).intValue();
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE unzip_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public void resetProcessing() {
        jdbcTemplate.update("UPDATE unzip_queue SET status = 0 WHERE status = 1");
    }

    public UnzipQueueItem poll() {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE unzip_queue SET status = 1 WHERE id = (SELECT id FROM unzip_queue WHERE status = 0 ORDER BY created_at LIMIT 1) RETURNING *\n" +
                        ")\n" +
                        "SELECT *, (file).*\n" +
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
        jdbcTemplate.update("DELETE FROM unzip_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    private UnzipQueueItem map(ResultSet resultSet) throws SQLException {
        UnzipQueueItem item = new UnzipQueueItem();
        item.setId(resultSet.getInt(UnzipQueueItem.ID));

        TgFile tgFile = new TgFile();
        tgFile.setFileId(resultSet.getString(TgFile.FILE_ID));
        item.setFile(tgFile);

        item.setType(Format.valueOf(resultSet.getString(UnzipQueueItem.TYPE)));
        item.setUserId(resultSet.getInt(UnzipQueueItem.USER_ID));

        return item;
    }
}
