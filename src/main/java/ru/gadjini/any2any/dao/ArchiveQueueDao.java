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
import ru.gadjini.telegram.smart.bot.commons.dao.QueueDao;
import ru.gadjini.telegram.smart.bot.commons.dao.WorkQueueDaoDelegate;
import ru.gadjini.telegram.smart.bot.commons.domain.QueueItem;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.property.FileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.property.ServerProperties;
import ru.gadjini.telegram.smart.bot.commons.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.utils.JdbcUtils;

import java.sql.*;
import java.util.List;
import java.util.Set;

@Repository
public class ArchiveQueueDao implements WorkQueueDaoDelegate<ArchiveQueueItem> {

    private JdbcTemplate jdbcTemplate;

    private FileLimitProperties fileLimitProperties;

    private ObjectMapper objectMapper;

    private ServerProperties serverProperties;

    @Autowired
    public ArchiveQueueDao(JdbcTemplate jdbcTemplate, FileLimitProperties fileLimitProperties,
                           ObjectMapper objectMapper, ServerProperties serverProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileLimitProperties = fileLimitProperties;
        this.objectMapper = objectMapper;
        this.serverProperties = serverProperties;
    }

    public int create(ArchiveQueueItem archiveQueueItem) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO archive_queue(user_id, files, type, status) " +
                            "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, archiveQueueItem.getUserId());

                    Object[] files = archiveQueueItem.getFiles().stream().map(TgFile::sqlObject).toArray();
                    Array array = con.createArrayOf(TgFile.TYPE, files);
                    ps.setArray(2, array);
                    ps.setObject(3, archiveQueueItem.getType().name());
                    ps.setInt(4, archiveQueueItem.getStatus().getCode());

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
                        "      FROM archive_queue c \n" +
                        "      WHERE status = 0\n" +
                        "        AND (SELECT sum(f.size) from unnest(c.files) f) " + getSign(weight) + " ?\n" +
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
                        "    UPDATE archive_queue SET" + QueueDao.getUpdateList(serverProperties.getNumber()) +
                        "WHERE id IN (SELECT id FROM archive_queue qu WHERE status = 0 AND archive_is_ready " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n" +
                        " ORDER BY qu.id LIMIT " + limit + ") RETURNING *\n" +
                        ")\n" +
                        "SELECT *, 1 as queue_position, (SELECT count(*) FROM downloading_queue dq WHERE dq.producer_id = cv.id AND dq.producer = 'archive_queue') as downloaded_files_count\n" +
                        "FROM r cv INNER JOIN (SELECT id, json_agg(files) as files_json FROM archive_queue WHERE status = 0 GROUP BY id) cc ON cv.id = cc.id\n",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
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
                        "                     FROM archive_queue c\n" +
                        "                     WHERE status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(c.files) f) " + getSign(weight) + " ?\n" +
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
        return jdbcTemplate.query("WITH r as(DELETE FROM archive_queue WHERE user_id = ? RETURNING *) SELECT * FROM r",
                ps -> ps.setInt(1, userId),
                (rs, num) -> mapDeleteItem(rs));
    }

    @Override
    public ArchiveQueueItem deleteAndGetById(int id) {
        return jdbcTemplate.query(
                "DELETE FROM archive_queue WHERE id = ? RETURNING *",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        return mapDeleteItem(rs);
                    }

                    return null;
                }
        );
    }

    public void setArchiveFilePath(int id, String archiveFilePath) {
        jdbcTemplate.update(
                "UPDATE archive_queue SET archive_file_path = ? where id = ?",
                ps -> {
                    ps.setString(1, archiveFilePath);
                    ps.setInt(2, id);
                }
        );
    }


    @Override
    public long countReadToComplete(SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COUNT(id) as cnt\n" +
                        "        FROM archive_queue qu WHERE qu.status = 0 " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n" +
                        " AND archive_is_ready\n"
                ,
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs) -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    @Override
    public long countProcessing(SmartExecutorService.JobWeight weight) {
        return jdbcTemplate.query(
                "SELECT COUNT(id) as cnt\n" +
                        "        FROM archive_queue qu WHERE qu.status = 1 " +
                        " AND (SELECT sum(f.size) from unnest(qu.files) f) " + getSign(weight) + " ?\n",
                ps -> ps.setLong(1, fileLimitProperties.getLightFileMaxWeight()),
                (rs) -> rs.next() ? rs.getLong("cnt") : 0
        );
    }

    public ArchiveQueueItem getArchiveTypeAndArchivePath(int id) {
        return jdbcTemplate.query(
                "SELECT id, archive_file_path, type FROM archive_queue WHERE id =?",
                ps -> ps.setInt(1, id),
                rs -> {
                    if (rs.next()) {
                        ArchiveQueueItem archiveQueueItem = new ArchiveQueueItem();
                        archiveQueueItem.setId(id);
                        archiveQueueItem.setArchiveFilePath(rs.getString(ArchiveQueueItem.ARCHIVE_FILE_PATH));
                        archiveQueueItem.setType(Format.valueOf(rs.getString(ArchiveQueueItem.TYPE)));

                        return archiveQueueItem;
                    }

                    return null;
                }
        );
    }

    public void setArchiveIsReady(int id, boolean ready) {
        jdbcTemplate.update(
                "UPDATE archive_queue SET archive_is_ready = ? where id = ?",
                ps -> {
                    ps.setBoolean(1, ready);
                    ps.setInt(2, id);
                }
        );
    }

    @Override
    public String getProducerName() {
        return getQueueName();
    }

    @Override
    public String getQueueName() {
        return ArchiveQueueItem.NAME;
    }

    private String getSign(SmartExecutorService.JobWeight weight) {
        return weight.equals(SmartExecutorService.JobWeight.LIGHT) ? "<=" : ">";
    }

    private ArchiveQueueItem mapDeleteItem(ResultSet rs) throws SQLException {
        ArchiveQueueItem fileQueueItem = new ArchiveQueueItem();

        fileQueueItem.setId(rs.getInt(ArchiveQueueItem.ID));
        fileQueueItem.setStatus(ArchiveQueueItem.Status.fromCode(rs.getInt(ArchiveQueueItem.STATUS)));
        fileQueueItem.setServer(rs.getInt(QueueItem.SERVER));

        return fileQueueItem;
    }

    private ArchiveQueueItem map(ResultSet resultSet) throws SQLException {
        Set<String> columnNames = JdbcUtils.getColumnNames(resultSet.getMetaData());
        ArchiveQueueItem item = new ArchiveQueueItem();
        item.setId(resultSet.getInt(ArchiveQueueItem.ID));

        item.setFiles(mapFiles(resultSet));
        item.setType(Format.valueOf(resultSet.getString(ArchiveQueueItem.TYPE)));
        item.setUserId(resultSet.getInt(ArchiveQueueItem.USER_ID));
        item.setProgressMessageId(resultSet.getInt(ArchiveQueueItem.PROGRESS_MESSAGE_ID));
        item.setArchiveFilePath(resultSet.getString(ArchiveQueueItem.ARCHIVE_FILE_PATH));
        item.setServer(resultSet.getInt(QueueItem.SERVER));
        if (columnNames.contains(QueueItem.QUEUE_POSITION)) {
            item.setQueuePosition(resultSet.getInt(QueueItem.QUEUE_POSITION));
        }
        if (columnNames.contains(ArchiveQueueItem.DOWNLOADED_FILES_COUNT)) {
            item.setDownloadedFilesCount(resultSet.getInt(ArchiveQueueItem.DOWNLOADED_FILES_COUNT));
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
