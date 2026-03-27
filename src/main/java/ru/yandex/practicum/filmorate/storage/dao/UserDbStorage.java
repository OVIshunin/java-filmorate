package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Repository("userDbStorage")
@Slf4j
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    @Override
    public User create(User user) {
        String sql = "INSERT INTO practicum.users (email, login, name, birthday) " +
                "VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, user.getBirthday() != null ? java.sql.Date.valueOf(user.getBirthday()) : null);
            return ps;
        }, keyHolder);

        Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        user.setId(id);

        log.debug("Создан пользователь: id={}, login={}", id, user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE practicum.users SET email = ?, login = ?, name = ?, birthday = ? " +
                "WHERE id = ?";

        int rowsAffected = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday() != null ? java.sql.Date.valueOf(user.getBirthday()) : null,
                user.getId()
        );

        if (rowsAffected == 0) {
            throw new ResourceNotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }

        log.debug("Обновлён пользователь: id={}, login={}", user.getId(), user.getLogin());
        return user;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM practicum.users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM practicum.users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM practicum.users WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        if (rowsAffected > 0) {
            log.debug("Удалён пользователь: id={}", id);
            return true;
        }
        return false;
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        if (!userExists(userId) || !userExists(friendId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        String checkSql = "SELECT COUNT(*) FROM practicum.friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count == null || count == 0) {
            String sql = "INSERT INTO practicum.friendships (user_id, friend_id, status) " +
                    "VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, userId, friendId, FriendshipStatus.PENDING.name());
            log.debug("Запрос в друзья отправлен: userId={}, friendId={}", userId, friendId);
        }
    }

    @Override
    public void confirmFriend(Integer userId, Integer friendId) {
        String sql = "UPDATE practicum.friendships SET status = ? " +
                "WHERE user_id = ? AND friend_id = ?";

        int rowsAffected = jdbcTemplate.update(sql, FriendshipStatus.CONFIRMED.name(), userId, friendId);

        if (rowsAffected == 0) {
            throw new ResourceNotFoundException("Запрос на дружбу не найден");
        }

        log.debug("Дружба подтверждена: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        if (!userExists(userId) || !userExists(friendId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }

        String checkSql = "SELECT COUNT(*) FROM practicum.friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count == null || count == 0) {
            log.debug("Попытка удалить несуществующую дружбу: userId={}, friendId={}", userId, friendId);
            return;
        }

        String sql = "DELETE FROM practicum.friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.debug("Дружба удалена: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public List<User> getUserFriends(Integer userId) {
        if (!userExists(userId)) {
            throw new ResourceNotFoundException("Пользователь с id=" + userId + " не найден");
        }

        // Возвращаем всех друзей (и PENDING, и CONFIRMED)
        String sql = "SELECT u.* FROM practicum.users u " +
                "INNER JOIN practicum.friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ?";

        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    @Override
    public List<User> getConfirmedFriends(Integer userId) {
        if (!userExists(userId)) {
            throw new ResourceNotFoundException("Пользователь с id=" + userId + " не найден");
        }

        // Возвращаем только подтверждённых друзей
        String sql = "SELECT u.* FROM practicum.users u " +
                "INNER JOIN practicum.friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status = ?";

        return jdbcTemplate.query(sql, userRowMapper, userId, FriendshipStatus.CONFIRMED.name());
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        if (!userExists(userId) || !userExists(otherId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }

        // Общие друзья — все, кто есть в списках друзей у обоих пользователей
        // без проверки статуса
        String sql = "SELECT u.* FROM practicum.users u " +
                "WHERE u.id IN (" +
                "    SELECT f1.friend_id FROM practicum.friendships f1 " +
                "    WHERE f1.user_id = ? " +
                "    INTERSECT " +
                "    SELECT f2.friend_id FROM practicum.friendships f2 " +
                "    WHERE f2.user_id = ?" +
                ")";

        return jdbcTemplate.query(sql, userRowMapper, userId, otherId);
    }

    private boolean userExists(Integer id) {
        String sql = "SELECT COUNT(*) FROM practicum.users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}