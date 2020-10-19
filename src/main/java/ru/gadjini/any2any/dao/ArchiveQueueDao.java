package ru.gadjini.any2any.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.sql.*;
import java.util.List;

@Repository
public class ArchiveQueueDao {

    private JdbcTemplate jdbcTemplate;

    private FileLimitProperties fileLimitProperties;

    private ObjectMapper objectMapper;

    @Autowired
    public ArchiveQueueDao(JdbcTemplate jdbcTemplate, FileLimitProperties fileLimitProperties, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileLimitProperties = fileLimitProperties;
        this.objectMapper = objectMapper;
    }

    public int create(ArchiveQueueItem archiveQueueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO archive_queue(user_id, files, total_file_size, type, status) " +
                            "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, archiveQueueItem.getUserId());

                    Object[] files = archiveQueueItem.getFiles().stream().map(TgFile::sqlObject).toArray();
                    Array array = con.createArrayOf(TgFile.TYPE, files);
                    ps.setArray(2, array);
                    ps.setLong(3, archiveQueueItem.getTotalFileSize());
                    ps.setObject(4, archiveQueueItem.getType().name());
                    ps.setInt(5, archiveQueueItem.getStatus().getCode());

                    return ps;
                },
                keyHolder
        );

        return ((Number) keyHolder.getKeys().get(ArchiveQueueItem.ID)).intValue();
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE archive_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public void setProgressMessageId(int id, int progressMessageId) {
        jdbcTemplate.update("UPDATE archive_queue SET progress_message_id = ? WHERE id = ?",
                ps -> {
                    ps.setInt(1, progressMessageId);
                    ps.setInt(2, id);
                });
    }

    public void resetProcessing() {
        jdbcTemplate.update("UPDATE archive_queue SET status = 0 WHERE status = 1");
    }

    public List<ArchiveQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE archive_queue SET status = 1 WHERE id IN (SELECT id FROM archive_queue WHERE status = 0 " +
                        "AND total_file_size " + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ? ORDER BY created_at LIMIT ?) RETURNING *\n" +
                        ")\n" +
                        "SELECT *\n" +
                        "FROM r cv INNER JOIN (SELECT id, json_agg(files) as files_json FROM archive_queue WHERE status = 0 GROUP BY id) cc ON cv.id = cc.id\n",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public ArchiveQueueItem deleteWithReturning(int id) {
        return jdbcTemplate.query(
                "WITH del AS (DELETE FROM archive_queue WHERE id = ? RETURNING total_file_size) SELECT total_file_size FROM del",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        ArchiveQueueItem queueItem = new ArchiveQueueItem();
                        queueItem.setId(rs.getInt(ArchiveQueueItem.ID));
                        queueItem.setTotalFileSize(rs.getLong(ArchiveQueueItem.TOTAL_FILE_SIZE));

                        return queueItem;
                    }

                    return null;
                }
        );
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM archive_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public ArchiveQueueItem deleteByUserId(int userId) {
        return jdbcTemplate.query("WITH r as(DELETE FROM archive_queue WHERE user_id = ? RETURNING id, total_file_size) SELECT * FROM r",
                ps -> ps.setInt(1, userId),
                rs -> {
                    if (rs.next()) {
                        ArchiveQueueItem queueItem = new ArchiveQueueItem();
                        queueItem.setId(rs.getInt(ArchiveQueueItem.ID));
                        queueItem.setTotalFileSize(rs.getLong(ArchiveQueueItem.TOTAL_FILE_SIZE));

                        return queueItem;
                    }

                    return null;
                });
    }

    public Boolean exists(int jobId) {
        return jdbcTemplate.query("SELECT TRUE FROM archive_queue WHERE id = ?", ps -> ps.setInt(1, jobId), ResultSet::next);
    }

    private ArchiveQueueItem map(ResultSet resultSet) throws SQLException {
        ArchiveQueueItem item = new ArchiveQueueItem();
        item.setId(resultSet.getInt(ArchiveQueueItem.ID));
        item.setTotalFileSize(resultSet.getLong(ArchiveQueueItem.TOTAL_FILE_SIZE));

        item.setFiles(mapFiles(resultSet));
        item.setType(Format.valueOf(resultSet.getString(ArchiveQueueItem.TYPE)));
        item.setUserId(resultSet.getInt(ArchiveQueueItem.USER_ID));
        item.setProgressMessageId(resultSet.getInt(ArchiveQueueItem.PROGRESS_MESSAGE_ID));

        return item;
    }

    private List<TgFile> mapFiles(ResultSet rs) throws SQLException {
        PGobject jsonArr = (PGobject) rs.getObject("files_json");
        if (jsonArr != null) {
            try {
                List<List<TgFile>> lists = objectMapper.readValue(jsonArr.getValue(), new TypeReference<>() {
                });

                return lists.iterator().next();
            } catch (JsonProcessingException e) {
                throw new SQLException(e);
            }
        }

        return null;
    }
}
