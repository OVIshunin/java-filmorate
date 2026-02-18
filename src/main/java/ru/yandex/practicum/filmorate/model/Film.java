package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.time.LocalDate;

@Data
public class Film {
    private Integer id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Integer duration; // в минутах

    // Метод валидации
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Название фильма не может быть пустым.");
        }
        if (description != null && description.length() > 200) {
            throw new ValidationException("Описание фильма не может превышать 200 символов.");
        }
        if (releaseDate != null && releaseDate.isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года.");
        }
        if (duration != null && duration <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }
    }

}