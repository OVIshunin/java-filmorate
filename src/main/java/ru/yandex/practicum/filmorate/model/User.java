package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import jakarta.validation.constraints.*;
import ru.yandex.practicum.filmorate.validator.ValidLogin;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class User {
    private Integer id;

    @NotBlank(message = "Электронная почта не может быть пустой.")
    @Email(message = "Электронная почта должна содержать символ @ и быть корректным адресом.")
    private String email;

    @ValidLogin
    private String login;

    private String name;

    @NotNull(message = "Дата рождения не может быть пустой.")
    @Past(message = "Дата рождения не может быть в будущем.")
    private LocalDate birthday;
     private Map<Integer, FriendshipStatus> friends = new HashMap<>();

    public void addFriend(Integer friendId) {
        friends.put(friendId, FriendshipStatus.PENDING);
    }

    public void addFriend(Integer friendId, FriendshipStatus status) {
        friends.put(friendId, status);
    }

    public void removeFriend(Integer friendId) {
        friends.remove(friendId);
    }

    public boolean isFriend(Integer friendId) {
        return friends.containsKey(friendId);
    }

    public FriendshipStatus getFriendStatus(Integer friendId) {
        return friends.get(friendId);
    }

    public void confirmFriend(Integer friendId) {
        if (friends.containsKey(friendId)) {
            friends.put(friendId, FriendshipStatus.CONFIRMED);
        }
    }

    public Set<Integer> getConfirmedFriends() {
        return friends.entrySet().stream()
                .filter(entry -> entry.getValue() == FriendshipStatus.CONFIRMED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Set<Integer> getPendingFriends() {
        return friends.entrySet().stream()
                .filter(entry -> entry.getValue() == FriendshipStatus.PENDING)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}