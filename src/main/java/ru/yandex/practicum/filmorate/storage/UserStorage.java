package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User create(User user);
    User update(User user);
    List<User> findAll();
    Optional<User> findById(Integer id);
    boolean delete(Integer id);
    void addFriend(Integer userId, Integer friendId);
    void removeFriend(Integer userId, Integer friendId);
    List<User> getUserFriends(Integer userId);
    List<User> getCommonFriends(Integer userId, Integer otherId);
}