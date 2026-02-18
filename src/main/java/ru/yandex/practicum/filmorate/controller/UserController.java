package ru.yandex.practicum.filmorate.controller;


import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final Map<Integer, User> userStorage = new HashMap<>();
    private int nextId = 1;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        log.info("Получен запрос на создание пользователя: login='{}'", user.getLogin());

        try {
            // Если name пустое — используем login
            if (user.getName() == null || user.getName().trim().isEmpty()) {
                user.setName(user.getLogin());
                log.debug("Имя не заполнено, был использован логин: '{}'", user.getName());
            }

            user.validate();
            user.setId(nextId++);
            userStorage.put(user.getId(), user);

            log.info("Пользователь успешно создан с ID ={}", user.getId());
            return ResponseEntity.status(201).body(user);
        } catch (ValidationException ex) {
            log.warn("Ошибка валидации данных пользователя: {}", ex.getMessage());
            throw ex;
        }
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User userUpdate) {
        Integer id = userUpdate.getId();
        log.info("Получен запрос на обновление пользователя по ID ={}", id);

        if (id == null) {
            log.warn("Отсутсвует значение поля ID в структуре объекта.");
            return ResponseEntity.badRequest().build();
        }

        User existingUser = userStorage.get(id);
        if (existingUser == null) {
            log.warn("Пользователь для обновления данных не найден: id={}", id);
            throw new ResourceNotFoundException("Пользователь с указанным id не найден.");
        }

        try {
            // Если name пустое — используем login
            if (userUpdate.getName() == null || userUpdate.getName().trim().isEmpty()) {
                userUpdate.setName(userUpdate.getLogin());
                log.debug("Имя было пустым, использовался логин: '{}'", userUpdate.getName());
            }

            userUpdate.validate();

            existingUser.setEmail(userUpdate.getEmail());
            existingUser.setLogin(userUpdate.getLogin());
            existingUser.setName(userUpdate.getName());
            existingUser.setBirthday(userUpdate.getBirthday());

            log.info("Данные пользователя успешно обновлены: id={}, login='{}'", id, existingUser.getLogin());
            return ResponseEntity.ok(existingUser);
        } catch (ValidationException ex) {
            log.warn("Ошибка валидации данных пользователя: {}", ex.getMessage());
            throw ex;
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Получен запрос на вывод всех пользователей, количество: {}", userStorage.size());
        List<User> users = new ArrayList<>(userStorage.values());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        log.info("Получен запрос на получение данных пользователя по ID ={}", id);

        User user = userStorage.get(id);
        if (user == null) {
            log.warn("Пользователь не найден: id={}", id);
            return ResponseEntity.notFound().build();
        }

        log.debug("Пользователь найден: id={}, login='{}'", id, user.getLogin());
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        log.info("Получен запрос на удаление данных пользователя по ID ={}", id);

        if (!userStorage.containsKey(id)) {
            log.warn("Пользователь не найден: id={}", id);
            return ResponseEntity.notFound().build();
        }

        userStorage.remove(id);
        log.info("Пользователь успешно удален: id={}", id);
        return ResponseEntity.noContent().build();
    }
}