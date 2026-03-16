package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Film createFilm(Film film) {
        return filmStorage.create(film);
    }

    public Film updateFilm(Film film) {
        return filmStorage.update(film);
    }

    public List<Film> getAllFilms() {
        return filmStorage.findAll();
    }

    public Film getFilmById(Integer id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Фильм с id=" + id + " не найден"));
    }

    public void deleteFilm(Integer id) {
        if (!filmStorage.delete(id)) {
            throw new ResourceNotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    public void addLike(Integer filmId, Integer userId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + userId + " не найден"));

        filmStorage.addLike(filmId, userId);
        log.info("Лайк добавлен: filmId={}, userId={}", filmId, userId);
    }

    public void removeLike(Integer filmId, Integer userId) {
        userStorage.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с id=" + userId + " не найден"));

        filmStorage.removeLike(filmId, userId);
        log.info("Лайк удалён: filmId={}, userId={}", filmId, userId);
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = count != null ? count : 10;
        return filmStorage.getPopularFilms(limit);
    }
}