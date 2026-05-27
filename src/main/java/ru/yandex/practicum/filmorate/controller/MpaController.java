package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.dao.MpaDbStorage;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@Slf4j
@RequiredArgsConstructor
public class MpaController {

    private final MpaDbStorage mpaStorage;

    @GetMapping
    public ResponseEntity<List<Mpa>> getAllMpa() {
        log.info("GET /mpa - получение всех рейтингов MPA");
        return ResponseEntity.ok(mpaStorage.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mpa> getMpaById(@PathVariable Integer id) {
        log.info("GET /mpa/{} - получение рейтинга MPA по id", id);
        return ResponseEntity.ok(mpaStorage.getById(id));
    }
}