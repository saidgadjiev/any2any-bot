package ru.gadjini.any2any.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.any2any.domain.TgUser;

@Repository
public class UserDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(TgUser user) {
        jdbcTemplate.update(
                "INSERT INTO tg_user(user_id) VALUES (?) ON CONFLICT(user_id) DO NOTHING",
                ps -> ps.setInt(1, user.getUserId())
        );
    }
}
