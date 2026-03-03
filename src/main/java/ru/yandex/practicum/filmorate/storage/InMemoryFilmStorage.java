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
    private final Map<Integer, Set<Integer>> likes = new HashMap<>(); // filmId -> Set of userId
    private int nextId = 1;

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        likes.put(film.getId(), new HashSet<>());
        log.debug("Фильм создан: id={}, name='{}'", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {
        Integer id = film.getId();
        if (!films.containsKey(id)) {
            throw new ResourceNotFoundException("Фильм с id=" + id + " не найден");
        }
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
            likes.remove(id);
            log.debug("Фильм удалён: id={}", id);
            return true;
        }
        return false;
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        if (!films.containsKey(filmId)) {
            throw new ResourceNotFoundException("Фильм с id=" + filmId + " не найден");
        }
        Set<Integer> filmLikes = likes.get(filmId);
        if (filmLikes.add(userId)) {
            log.debug("Лайк добавлен: filmId={}, userId={}", filmId, userId);
        }
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        if (!films.containsKey(filmId)) {
            throw new ResourceNotFoundException("Фильм с id=" + filmId + " не найден");
        }
        Set<Integer> filmLikes = likes.get(filmId);
        if (filmLikes.remove(userId)) {
            log.debug("Лайк удалён: filmId={}, userId={}", filmId, userId);
        }
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1); // убывание
                })
                .limit(count)
                .collect(Collectors.toList());
    }
}