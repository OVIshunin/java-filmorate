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
        List<User> users = jdbcTemplate.query(sql, userRowMapper);
        users.forEach(this::loadFriends);
        return users;
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM practicum.users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            if (user != null) {
                loadFriends(user);
            }
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
            String sql = "INSERT INTO practicum.friendships (user_id, friend_id, status) VALUES (?, ?, ?)";
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
        // Проверяем, что оба пользователя существуют
        if (!userExists(userId) || !userExists(friendId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }

        // Проверяем, что дружба существует
        String checkSql = "SELECT COUNT(*) FROM practicum.friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count == null || count == 0) {
            // Если дружбы нет, ничего не удаляем
            log.debug("Дружба не найдена: userId={}, friendId={}", userId, friendId);
            return;
        }

        // Удаляем дружбу в обе стороны
        String sql = "DELETE FROM practicum.friendships WHERE (user_id = ? AND friend_id = ?) " +
                "OR (user_id = ? AND friend_id = ?)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);
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

        List<User> friends = jdbcTemplate.query(sql, userRowMapper, userId);
        friends.forEach(this::loadFriends);

        return friends;
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

        List<User> friends = jdbcTemplate.query(sql, userRowMapper, userId, FriendshipStatus.CONFIRMED.name());
        friends.forEach(this::loadFriends);

        return friends;
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        String sql = "SELECT u.* FROM practicum.users u " +
                "WHERE u.id IN (" +
                "    SELECT f1.friend_id FROM practicum.friendships f1 " +
                "    WHERE f1.user_id = ? " +
                "    INTERSECT " +
                "    SELECT f2.friend_id FROM practicum.friendships f2 " +
                "    WHERE f2.user_id = ?" +
                ")";

        List<User> commonFriends = jdbcTemplate.query(
                sql, userRowMapper,
                userId,
                otherId
        );

        commonFriends.forEach(this::loadFriends);

        return commonFriends;
    }

    // Вспомогательные методы

    private boolean userExists(Integer id) {
        String sql = "SELECT COUNT(*) FROM practicum.users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private void loadFriends(User user) {
        String sql = "SELECT friend_id, status FROM practicum.friendships WHERE user_id = ?";

        jdbcTemplate.query(sql, rs -> {
            Integer friendId = rs.getInt("friend_id");
            FriendshipStatus status = FriendshipStatus.valueOf(rs.getString("status"));
            user.addFriend(friendId, status);
        }, user.getId());
    }
}