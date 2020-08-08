package ru.gadjini.any2any.dao.queue;

import org.apache.commons.lang3.StringUtils;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.ArchiveQueueItem;
import ru.gadjini.any2any.domain.RenameQueueItem;
import ru.gadjini.any2any.domain.TgFile;
import ru.gadjini.any2any.service.concurrent.SmartExecutorService;
import ru.gadjini.any2any.service.conversion.api.Format;
import ru.gadjini.any2any.utils.MemoryUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class ArchiveQueueDao {

    private static final Pattern PG_TYPE_PATTERN = Pattern.compile("[^,]*,?");

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ArchiveQueueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

        return ((Number) keyHolder.getKeys().get(RenameQueueItem.ID)).intValue();
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
                        "FROM r",
                ps -> {
                    ps.setLong(1, MemoryUtils.MB_100);
                    ps.setInt(2, limit);
                },
                (rs, rowNum) -> map(rs)
        );
    }

    public ArchiveQueueItem deleteWithReturning(int id) {
        return jdbcTemplate.query(
                "WITH del AS (DELETE FROM archive_queue WHERE id = ? RETURNING *) SELECT * FROM del",
                ps -> ps.setInt(1, id),
                rs -> rs.next() ? map(rs) : null
        );
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM archive_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public List<Integer> deleteByUserId(int userId) {
        return jdbcTemplate.query("WITH r as(DELETE FROM archive_queue WHERE user_id = ? RETURNING id) SELECT * FROM r", ps -> ps.setInt(1, userId),
                (rs, rowNum) -> rs.getInt("id"));
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
        item.setUserId(resultSet.getInt(RenameQueueItem.USER_ID));

        return item;
    }

    private List<TgFile> mapFiles(ResultSet rs) throws SQLException {
        List<TgFile> repeatTimes = new ArrayList<>();
        PgArray arr = (PgArray) rs.getArray(ArchiveQueueItem.FILES);
        Object[] unparsedRepeatTimes = (Object[]) arr.getArray();

        for (Object object : unparsedRepeatTimes) {
            if (object == null) {
                continue;
            }
            String t = ((PGobject) object).getValue().replace("\"", "");
            t = t.substring(1, t.length() - 1);
            Matcher argMatcher = PG_TYPE_PATTERN.matcher(t);

            TgFile file = new TgFile();
            if (argMatcher.find()) {
                String fileId = t.substring(argMatcher.start(), argMatcher.end() - 1);
                if (StringUtils.isNotBlank(fileId)) {
                    file.setFileId(fileId);
                }
            }
            if (argMatcher.find()) {
                String mimeType = t.substring(argMatcher.start(), argMatcher.end() - 1);
                if (StringUtils.isNotBlank(mimeType)) {
                    file.setMimeType(mimeType);
                }
            }
            if (argMatcher.find()) {
                String fileName = t.substring(argMatcher.start(), argMatcher.end() - 1);
                if (StringUtils.isNotBlank(fileName)) {
                    file.setFileName(fileName);
                }
            }
            if (argMatcher.find()) {
                String size = t.substring(argMatcher.start(), argMatcher.end() - 1);
                if (StringUtils.isNotBlank(size)) {
                    file.setSize(Long.parseLong(size));
                }
            }
            repeatTimes.add(file);
        }

        return repeatTimes;
    }
}
