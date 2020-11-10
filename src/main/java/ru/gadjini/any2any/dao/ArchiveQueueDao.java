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
import ru.gadjini.telegram.smart.bot.commons.dao.QueueDaoDelegate;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.property.QueueProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.utils.JdbcUtils;

import java.sql.*;
import java.util.List;
import java.util.Set;

@Repository
public class ArchiveQueueDao implements QueueDaoDelegate<ArchiveQueueItem> {

    private JdbcTemplate jdbcTemplate;

    private FileLimitProperties fileLimitProperties;

    private QueueProperties queueProperties;

    private ObjectMapper objectMapper;

    @Autowired
    public ArchiveQueueDao(JdbcTemplate jdbcTemplate, FileLimitProperties fileLimitProperties,
                           QueueProperties queueProperties, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileLimitProperties = fileLimitProperties;
        this.queueProperties = queueProperties;
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

    public Integer getQueuePosition(int id, SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COALESCE(queue_position, 1) as queue_position\n" +
                        "FROM (SELECT id, row_number() over (ORDER BY created_at) AS queue_position\n" +
                        "      FROM archive_queue c, unnest(c.files) cf\n" +
                        "      WHERE status = 0\n" +
                        "        GROUP BY c.id HAVING sum(cf.size)" + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ?\n" +
                        ") as file_q\n" +
                        "WHERE id = ?",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
                },
                rs -> {
                    if (rs.next()) {
                        return rs.getInt(ArchiveQueueItem.QUEUE_POSITION);
                    }

                    return 1;
                }
        );
    }

    @Override
    public List<ArchiveQueueItem> poll(SmartExecutorService.JobWeight weight, int limit) {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE archive_queue SET status = 1, last_run_at = now(), attempts = attempts + 1, " +
                        "started_at = COALESCE(started_at, now()) " +
                        "WHERE attempts <= ? AND id IN (SELECT id FROM archive_queue WHERE status = 0 " +
                        "AND total_file_size " + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ? ORDER BY created_at LIMIT ?) RETURNING *\n" +
                        ")\n" +
                        "SELECT *, 1 as queue_position\n" +
                        "FROM r cv INNER JOIN (SELECT id, json_agg(files) as files_json FROM archive_queue WHERE status = 0 GROUP BY id) cc ON cv.id = cc.id\n",
                ps -> {
                    ps.setLong(1, queueProperties.getMaxAttempts());
                    ps.setLong(2, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(3, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public SmartExecutorService.JobWeight getWeight(int id) {
        Long size = jdbcTemplate.query(
                "SELECT sum(cf.size) as sm FROM archive_queue cv, unnest(cv.files) cf WHERE id = ?",
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? rs.getLong("sm") : null
        );

        return size == null ? null : size > fileLimitProperties.getLightFileMaxWeight() ? SmartExecutorService.JobWeight.HEAVY : SmartExecutorService.JobWeight.LIGHT;
    }

    @Override
    public ArchiveQueueItem getById(int id) {
        SmartExecutorService.JobWeight weight = getWeight(id);

        if (weight == null) {
            return null;
        }
        return jdbcTemplate.query(
                "SELECT f.*, COALESCE(queue_place.queue_position, 1) as queue_position, cc.files_json\n" +
                        "FROM archive_queue f\n" +
                        "         LEFT JOIN (SELECT id, row_number() over (ORDER BY created_at) as queue_position\n" +
                        "                     FROM archive_queue c, unnest(c.files) cf\n" +
                        "                     WHERE status = 0 " +
                        "        GROUP BY c.id HAVING sum(cf.size) " + (weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">") + " ?\n" +
                        ") queue_place ON f.id = queue_place.id\n" +
                        "         INNER JOIN (SELECT id, json_agg(files) as files_json FROM archive_queue WHERE id = ? GROUP BY id) cc ON f.id = cc.id\n" +
                        "WHERE f.id = ?\n",
                ps -> {
                    ps.setLong(1, fileLimitProperties.getLightFileMaxWeight());
                    ps.setInt(2, id);
                    ps.setInt(3, id);
                },
                rs -> {
                    if (rs.next()) {
                        return map(rs);
                    }

                    return null;
                }
        );
    }

    @Override
    public List<ArchiveQueueItem> deleteAndGetProcessingOrWaitingByUserId(int userId) {
        return jdbcTemplate.query("WITH r as(DELETE FROM archive_queue WHERE user_id = ? RETURNING id, total_file_size) SELECT * FROM r",
                ps -> ps.setInt(1, userId),
                (rs, num) -> {
                    ArchiveQueueItem queueItem = new ArchiveQueueItem();
                    queueItem.setId(rs.getInt(ArchiveQueueItem.ID));
                    queueItem.setTotalFileSize(rs.getLong(ArchiveQueueItem.TOTAL_FILE_SIZE));

                    return queueItem;
                });
    }

    @Override
    public ArchiveQueueItem deleteAndGetById(int id) {
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

    @Override
    public String getQueueName() {
        return ArchiveQueueItem.NAME;
    }

    private ArchiveQueueItem map(ResultSet resultSet) throws SQLException {
        Set<String> columnNames = JdbcUtils.getColumnNames(resultSet.getMetaData());
        ArchiveQueueItem item = new ArchiveQueueItem();
        item.setId(resultSet.getInt(ArchiveQueueItem.ID));
        item.setTotalFileSize(resultSet.getLong(ArchiveQueueItem.TOTAL_FILE_SIZE));

        item.setFiles(mapFiles(resultSet));
        item.setType(Format.valueOf(resultSet.getString(ArchiveQueueItem.TYPE)));
        item.setUserId(resultSet.getInt(ArchiveQueueItem.USER_ID));
        item.setProgressMessageId(resultSet.getInt(ArchiveQueueItem.PROGRESS_MESSAGE_ID));
        if (columnNames.contains(QueueItem.QUEUE_POSITION)) {
            item.setQueuePosition(resultSet.getInt(QueueItem.QUEUE_POSITION));
        }

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
