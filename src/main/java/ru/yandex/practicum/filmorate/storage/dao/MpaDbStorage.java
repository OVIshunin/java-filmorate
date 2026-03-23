package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MpaRowMapper mpaRowMapper;

    public List<Mpa> findAll() {
        String sql = "SELECT * FROM practicum.mpa ORDER BY id";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    public Optional<Mpa> findById(Integer id) {
        String sql = "SELECT * FROM practicum.mpa WHERE id = ?";
        try {
            Mpa mpa = jdbcTemplate.queryForObject(sql, mpaRowMapper, id);
            return Optional.ofNullable(mpa);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Mpa getById(Integer id) {
        return findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Рейтинг MPA с id=" + id + " не найден"));
    }
}