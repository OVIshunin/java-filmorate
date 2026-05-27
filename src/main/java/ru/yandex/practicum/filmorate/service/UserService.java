package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

    public User createUser(User user) {
        log.info("Создание пользователя: login={}", user.getLogin());

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName(user.getLogin());
        }

        return userStorage.create(user);
    }

    public User updateUser(User user) {
        log.info("Обновление пользователя: id={}", user.getId());

        if (user.getId() == null) {
            throw new ValidationException("ID пользователя должен быть указан");
        }

        userStorage.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + user.getId() + " не найден"));

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName(user.getLogin());
        }

        return userStorage.update(user);
    }

    public List<User> getAllUsers() {
        log.info("Получение всех пользователей");
        return userStorage.findAll();
    }

    public User getUserById(Integer id) {
        log.info("Получение пользователя по id={}", id);
        return userStorage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public void deleteUser(Integer id) {
        log.info("Удаление пользователя: id={}", id);
        if (!userStorage.delete(id)) {
            throw new ResourceNotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    public void addFriend(Integer userId, Integer friendId) {
        log.info("Отправка запроса в друзья: userId={}, friendId={}", userId, friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        getUserById(userId);
        getUserById(friendId);

        userStorage.addFriend(userId, friendId);
        log.info("Запрос в друзья отправлен");
    }

    public void confirmFriend(Integer userId, Integer friendId) {
        log.info("Подтверждение дружбы: userId={}, friendId={}", userId, friendId);

        getUserById(userId);
        getUserById(friendId);

        userStorage.confirmFriend(userId, friendId);
        log.info("Дружба подтверждена");
    }

    public void removeFriend(Integer userId, Integer friendId) {
        log.info("Удаление из друзей: userId={}, friendId={}", userId, friendId);

        userStorage.removeFriend(userId, friendId);
        log.info("Дружба удалена");
    }

    public List<User> getUserFriends(Integer userId) {
        log.info("Получение списка друзей пользователя: userId={}", userId);
        getUserById(userId);
        return userStorage.getUserFriends(userId);
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        log.info("Получение общих друзей: userId={}, otherId={}", userId, otherId);
        getUserById(userId);
        getUserById(otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }
}