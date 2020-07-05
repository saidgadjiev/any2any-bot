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
import ru.gadjini.any2any.service.conversion.api.Format;

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

        return ((Number) keyHolder.getKeys().get(RenameQueueItem.ID)).intValue();
    }

    public void setWaiting(int id) {
        jdbcTemplate.update("UPDATE archive_queue SET status = 0 WHERE id = ?",
                ps -> ps.setInt(1, id));
    }

    public void resetProcessing() {
        jdbcTemplate.update("UPDATE archive_queue SET status = 0 WHERE status = 1");
    }

    public ArchiveQueueItem poll() {
        return jdbcTemplate.query(
                "WITH r AS (\n" +
                        "    UPDATE archive_queue SET status = 1 WHERE id = (SELECT id FROM archive_queue WHERE status = 0 ORDER BY created_at LIMIT 1) RETURNING *\n" +
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
        jdbcTemplate.update("DELETE FROM archive_queue WHERE id = ?", ps -> ps.setInt(1, id));
    }

    private ArchiveQueueItem map(ResultSet resultSet) throws SQLException {
        ArchiveQueueItem item = new ArchiveQueueItem();
        item.setId(resultSet.getInt(RenameQueueItem.ID));

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
                    file.setSize(Integer.parseInt(size));
                }
            }
            repeatTimes.add(file);
        }

        return repeatTimes;
    }
}
