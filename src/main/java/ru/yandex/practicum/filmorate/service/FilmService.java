package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;

    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

    public Film createFilm(Film film) {
        log.info("Создание фильма: {}", film.getName());
        return filmStorage.create(film);
    }

    public Film updateFilm(Film film) {
        log.info("Обновление фильма: id={}", film.getId());
        // Проверяем существование фильма
        if (film.getId() == null) {
            throw new ValidationException("ID фильма должен быть указан");
        }
        filmStorage.findById(film.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Фильм с id=" + film.getId() + " не найден"));
        return filmStorage.update(film);
    }

    public List<Film> getAllFilms() {
        log.info("Получение всех фильмов");
        return filmStorage.findAll();
    }

    public Film getFilmById(Integer id) {
        log.info("Получение фильма по id={}", id);
        return filmStorage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм с id=" + id + " не найден"));
    }

    public void deleteFilm(Integer id) {
        log.info("Удаление фильма: id={}", id);
        if (!filmStorage.delete(id)) {
            throw new ResourceNotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    public void addLike(Integer filmId, Integer userId) {
        log.info("Добавление лайка: filmId={}, userId={}", filmId, userId);

        // Проверяем существование фильма и пользователя
        getFilmById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + userId + " не найден"));

        filmStorage.addLike(filmId, userId);
        log.info("Лайк успешно добавлен");
    }

    public void removeLike(Integer filmId, Integer userId) {
        log.info("Удаление лайка: filmId={}, userId={}", filmId, userId);

        // Проверяем существование фильма и пользователя
        getFilmById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + userId + " не найден"));

        filmStorage.removeLike(filmId, userId);
        log.info("Лайк успешно удалён");
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = count != null ? count : 10;

        if (limit <= 0) {
            throw new ValidationException("Количество фильмов должно быть положительным числом");
        }

        log.info("Получение популярных фильмов: count={}", limit);
        return filmStorage.getPopularFilms(limit);
    }
}