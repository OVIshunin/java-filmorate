package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final Map<Integer, Film> filmStorage = new HashMap<>();
    private int nextId = 1;

    @PostMapping
    public ResponseEntity<Film> createFilm(@Valid @RequestBody Film film) {
        log.info("Создание фильма: название ='{}'", film.getName());

        film.setId(nextId++);
        filmStorage.put(film.getId(), film);

        log.info("Успешно создан фильм с ID ={}", film.getId());
        return ResponseEntity.status(201).body(film);
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@Valid @RequestBody Film filmUpdate) {
        Integer id = filmUpdate.getId();
        log.info("Обновление фильма по ID ={}", id);

        // Вместо возврата ResponseEntity.badRequest() бросаем исключение
        if (id == null) {
            log.warn("Отсутствует значение поля ID в структуре объекта.");
            throw new ValidationException("ID фильма должен быть указан.");
        }

        Film existingFilm = filmStorage.get(id);
        if (existingFilm == null) {
            log.warn("Фильм с указанным id не найден: id={}", id);
            throw new ResourceNotFoundException("Фильм с указанным id не найден.");
        }

        // Обновляем поля существующего фильма
        existingFilm.setName(filmUpdate.getName());
        existingFilm.setDescription(filmUpdate.getDescription());
        existingFilm.setReleaseDate(filmUpdate.getReleaseDate());
        existingFilm.setDuration(filmUpdate.getDuration());

        log.info("Фильм успешно обновлен: id={}, name='{}'", id, existingFilm.getName());
        return ResponseEntity.ok(existingFilm);
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
            // Здесь тоже бросаем ResourceNotFoundException для единообразия
            throw new ResourceNotFoundException("Фильм с указанным id не найден.");
        }

        log.debug("Фильм найден: id={}, name='{}'", id, film.getName());
        return ResponseEntity.ok(film);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Integer id) {
        log.info("Получен запрос на удаление фильма по ID ={}", id);

        if (!filmStorage.containsKey(id)) {
            log.warn("Фильм для удаления не найден: id={}", id);
            throw new ResourceNotFoundException("Фильм с указанным id не найден.");
        }

        filmStorage.remove(id);
        log.info("Фильм успешно удалён: id={}", id);
        return ResponseEntity.noContent().build();
    }
}