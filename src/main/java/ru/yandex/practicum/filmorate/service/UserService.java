package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public User createUser(User user) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName(user.getLogin());
        }
        return userStorage.create(user);
    }

    public User updateUser(User user) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName(user.getLogin());
        }
        return userStorage.update(user);
    }

    public List<User> getAllUsers() {
        return userStorage.findAll();
    }

    public User getUserById(Integer id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public void deleteUser(Integer id) {
        if (!userStorage.delete(id)) {
            throw new ResourceNotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    public void addFriend(Integer userId, Integer friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Нельзя добавить самого себя в друзья");
        }

        userStorage.addFriend(userId, friendId);
        log.info("Дружба добавлена: userId={}, friendId={}", userId, friendId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        userStorage.removeFriend(userId, friendId);
        log.info("Дружба удалена: userId={}, friendId={}", userId, friendId);
    }

    public List<User> getUserFriends(Integer userId) {
        getUserById(userId);
        return userStorage.getUserFriends(userId);
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        getUserById(userId);
        getUserById(otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }
}