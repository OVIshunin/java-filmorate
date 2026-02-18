package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.time.LocalDate;

@Data
public class User {
    private Integer id;
    private String email;
    private String login;
    private String name;
    private LocalDate birthday;

    // Метод валидации
    public void validate() {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Электронная почта не может быть пустой.");
        }
        if (!email.contains("@")) {
            throw new ValidationException("Электронная почта должна содержать символ @.");
        }
        if (login == null || login.trim().isEmpty()) {
            throw new ValidationException("Логин не может быть пустым.");
        }
        if (login.contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы.");
        }
        if (birthday != null && birthday.isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }
    }
}