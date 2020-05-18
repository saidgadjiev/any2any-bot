package ru.gadjini.any2any.dao;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.TgUser;

import java.sql.Statement;
import java.sql.Types;

@Repository
public class UserDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String createOrUpdate(TgUser user) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO tg_user(user_id, username) VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET last_logged_in_at = now(), username = excluded.username " +
                                    "RETURNING CASE WHEN XMAX::text::int > 0 THEN 'updated' ELSE 'inserted' END AS state",
                            Statement.RETURN_GENERATED_KEYS
                    );

                    ps.setInt(1, user.getUserId());
                    if (StringUtils.isBlank(user.getUsername())) {
                        ps.setNull(2, Types.NULL);
                    } else {
                        ps.setString(2, user.getUsername());
                    }

                    return ps;
                },
                keyHolder
        );

        return (String) keyHolder.getKeys().get("state");
    }
}
