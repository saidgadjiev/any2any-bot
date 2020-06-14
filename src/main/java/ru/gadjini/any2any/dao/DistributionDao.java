package ru.gadjini.any2any.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.Distribution;
import ru.gadjini.any2any.domain.TgUser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class DistributionDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public DistributionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isExists() {
        return jdbcTemplate.query(
                "SELECT COUNT(*) as cnt FROM distribution",
                rs -> {
                    if (rs.next()) {
                        return rs.getLong("cnt") > 0;
                    }

                    return false;
                }
        );
    }

    public List<Distribution> popDistributions(int limit) {
        return jdbcTemplate.query("WITH distr AS (\n" +
                        "    DELETE FROM distribution WHERE id IN (SELECT id FROM distribution ORDER BY id LIMIT " + limit + ") RETURNING *\n" +
                        ")\n" +
                        "SELECT distr.*, tu.locale\n" +
                        "FROM distr\n" +
                        "         INNER JOIN tg_user tu ON distr.user_id = tu.user_id;",
                (rs, rowNum) -> map(rs));
    }

    public Distribution popDistribution(int userId) {
        return jdbcTemplate.query("WITH distr AS (\n" +
                        "    DELETE FROM distribution WHERE id IN (SELECT id FROM distribution WHERE user_id = ? ORDER BY id LIMIT 1) RETURNING *\n" +
                        ")\n" +
                        "SELECT distr.*, tu.locale\n" +
                        "FROM distr\n" +
                        "         INNER JOIN tg_user tu ON distr.user_id = tu.user_id;",
                ps -> ps.setInt(1, userId),
                rs -> {
                    if (rs.next()) {
                        return map(rs);
                    }

                    return null;
                });
    }

    private Distribution map(ResultSet rs) throws SQLException {
        Distribution distribution = new Distribution();
        distribution.setUserId(rs.getInt(Distribution.USER_ID));
        distribution.setMessageRu(rs.getString(Distribution.MESSAGE_RU));
        distribution.setMessageEn(rs.getString(Distribution.MESSAGE_EN));

        TgUser user = new TgUser();
        user.setUserId(distribution.getUserId());
        user.setLocale(rs.getString(TgUser.LOCALE));
        distribution.setUser(user);

        return distribution;
    }
}
