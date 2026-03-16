package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Set<Integer>> friendships = new HashMap<>(); // userId -> Set of friendIds
    private int nextId = 1;

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        friendships.put(user.getId(), new HashSet<>());
        log.debug("Пользователь создан: id={}, login='{}'", user.getId(), user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        Integer id = user.getId();
        if (!users.containsKey(id)) {
            throw new ResourceNotFoundException("Пользователь с id=" + id + " не найден");
        }
        users.put(id, user);
        log.debug("Пользователь обновлён: id={}, login='{}'", id, user.getLogin());
        return user;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public boolean delete(Integer id) {
        if (users.containsKey(id)) {
            users.remove(id);
            friendships.remove(id);

            friendships.values().forEach(friends -> friends.remove(id));
            log.debug("Пользователь удалён: id={}", id);
            return true;
        }
        return false;
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        if (!users.containsKey(userId) || !users.containsKey(friendId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }

        Set<Integer> userFriends = friendships.get(userId);
        Set<Integer> friendFriends = friendships.get(friendId);

        userFriends.add(friendId);
        friendFriends.add(userId);

        log.debug("Дружба добавлена: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        if (!users.containsKey(userId) || !users.containsKey(friendId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }

        Set<Integer> userFriends = friendships.get(userId);
        Set<Integer> friendFriends = friendships.get(friendId);

        userFriends.remove(friendId);
        friendFriends.remove(userId);

        log.debug("Дружба удалена: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public List<User> getUserFriends(Integer userId) {
        if (!users.containsKey(userId)) {
            throw new ResourceNotFoundException("Пользователь с id=" + userId + " не найден");
        }

        return friendships.get(userId).stream()
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        if (!users.containsKey(userId) || !users.containsKey(otherId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }

        Set<Integer> userFriends = friendships.get(userId);
        Set<Integer> otherFriends = friendships.get(otherId);

        return userFriends.stream()
                .filter(otherFriends::contains)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}