package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class GenreDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper genreRowMapper;

    public List<Genre> findAll() {
        String sql = "SELECT * FROM practicum.genres ORDER BY id";
        return jdbcTemplate.query(sql, genreRowMapper);
    }

    public Optional<Genre> findById(Integer id) {
        String sql = "SELECT * FROM practicum.genres WHERE id = ?";
        try {
            Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper, id);
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Genre getById(Integer id) {
        return findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Жанр с id=" + id + " не найден"));
    }
}
