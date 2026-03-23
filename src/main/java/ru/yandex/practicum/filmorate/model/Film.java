package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.yandex.practicum.filmorate.validator.ValidReleaseDate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class Film {
    private Integer id;

    @NotBlank(message = "Название фильма не может быть пустым.")
    private String name;

    @Size(max = 200, message = "Описание фильма не может превышать 200 символов.")
    private String description;

    @ValidReleaseDate
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительным числом.")
    private Integer duration;

    // Множество пользователей, поставивших лайк
    private Set<Integer> likes = new HashSet<>();

    // Жанры фильма (может быть несколько)
    private Set<Genre> genres = new LinkedHashSet<>();

    // Рейтинг MPA
    private Mpa mpa;

    // Вспомогательный метод для получения количества лайков
    public int getLikesCount() {
        return likes != null ? likes.size() : 0;
    }
}