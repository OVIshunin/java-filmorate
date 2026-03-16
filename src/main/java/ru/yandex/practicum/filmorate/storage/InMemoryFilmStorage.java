package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        film.setLikes(new HashSet<>()); // Инициализируем пустое множество лайков
        films.put(film.getId(), film);
        log.debug("Фильм создан: id={}, name='{}'", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {
        Integer id = film.getId();
        if (!films.containsKey(id)) {
            throw new ResourceNotFoundException("Фильм с id=" + id + " не найден");
        }

        // Теперь лайки в объекте Film и мы сохраняем их, если обновляем поля фильма
        Film existingFilm = films.get(id);
        film.setLikes(existingFilm.getLikes());

        films.put(id, film);
        log.debug("Фильм обновлён: id={}, name='{}'", id, film.getName());
        return film;
    }

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Optional<Film> findById(Integer id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public boolean delete(Integer id) {
        if (films.containsKey(id)) {
            films.remove(id);
            log.debug("Фильм удалён: id={}", id);
            return true;
        }
        return false;
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        Film film = films.get(filmId);
        if (film == null) {
            throw new ResourceNotFoundException("Фильм с id=" + filmId + " не найден");
        }

        film.getLikes().add(userId);
        log.debug("Лайк добавлен: filmId={}, userId={}", filmId, userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        Film film = films.get(filmId);
        if (film == null) {
            throw new ResourceNotFoundException("Фильм с id=" + filmId + " не найден");
        }

        film.getLikes().remove(userId);
        log.debug("Лайк удалён: filmId={}, userId={}", filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = f1.getLikes().size();
                    int likes2 = f2.getLikes().size();
                    return Integer.compare(likes2, likes1); // убывание
                })
                .limit(count)
                .collect(Collectors.toList());
    }

}