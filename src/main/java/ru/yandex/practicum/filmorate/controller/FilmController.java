package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import java.util.*;


@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final Map<Integer, Film> filmStorage = new HashMap<>();
    private int nextId = 1;

    @PostMapping
    public ResponseEntity<Film> createFilm(@RequestBody Film film) {
        log.info("Создание фильма: название ='{}'", film.getName());

        try {
            film.validate();
            film.setId(nextId++);
            filmStorage.put(film.getId(), film);

            log.info("Успешно создан фильм с ID ={}", film.getId());
            return ResponseEntity.status(201).body(film);
        } catch (ValidationException ex) {
            log.warn("Ошибка валидации: {}", ex.getMessage());
            throw ex; // Передаётся в GlobalExceptionHandler
        }
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@RequestBody Film filmUpdate) {
        Integer id = filmUpdate.getId();
        log.info("Обновление фильма по ID ={}", id);

        if (id == null) {
            log.warn("Отсутсвует значение поля ID в структуре объекта.");
            return ResponseEntity.badRequest().build();
        }

        Film existingFilm = filmStorage.get(id);
        if (existingFilm == null) {
            log.warn("Фильм с указанным id не найден: id={}", id);
            throw new ResourceNotFoundException("Фильм с указанным id не найден.");
        }

        try {
            filmUpdate.validate();

            existingFilm.setName(filmUpdate.getName());
            existingFilm.setDescription(filmUpdate.getDescription());
            existingFilm.setReleaseDate(filmUpdate.getReleaseDate());
            existingFilm.setDuration(filmUpdate.getDuration());

            log.info("Фильм успешно обновлен: id={}, name='{}'", id, existingFilm.getName());
            return ResponseEntity.ok(existingFilm);
        } catch (ValidationException ex) {
            log.warn("Ошибка валидации при обновлении: {}", ex.getMessage());
            throw ex;
        }
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Получен запрос на вывод всех фильмов, кол-во: {}", filmStorage.size());
        List<Film> films = new ArrayList<>(filmStorage.values());
        return ResponseEntity.ok(films);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> getFilmById(@PathVariable Integer id) {
        log.info("Получен запрос на вывод фильма по ID ={}", id);

        Film film = filmStorage.get(id);
        if (film == null) {
            log.warn("Фильм не найден: id={}", id);
            return ResponseEntity.notFound().build();
        }

        log.debug("Фильм найден: id={}, name='{}'", id, film.getName());
        return ResponseEntity.ok(film);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Integer id) {
        log.info("Получен запрос на удаление фильма по ID ={}", id);

        if (!filmStorage.containsKey(id)) {
            log.warn("Фильм для удаления не найден: id={}", id);
            return ResponseEntity.notFound().build();
        }

        filmStorage.remove(id);
        log.info("Фильм успешно удалён: id={}", id);
        return ResponseEntity.noContent().build();
    }
}