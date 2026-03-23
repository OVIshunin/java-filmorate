package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Repository("filmDbStorage")
@Slf4j
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final GenreRowMapper genreRowMapper;

    @Override
    public Film create(Film film) {
        // Валидация MPA и жанров
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());

        String sql = "INSERT INTO practicum.films (name, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, film.getReleaseDate() != null ? java.sql.Date.valueOf(film.getReleaseDate()) : null);
            ps.setInt(4, film.getDuration());
            ps.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return ps;
        }, keyHolder);

        Integer id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        film.setId(id);

        // Сохраняем жанры
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        // Загружаем полные данные фильма
        return findById(id).orElse(film);
    }

    @Override
    public Film update(Film film) {
        // Валидация MPA и жанров
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());

        String sql = "UPDATE practicum.films SET name = ?, description = ?, " +
                "release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";

        int rowsAffected = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate() != null ? java.sql.Date.valueOf(film.getReleaseDate()) : null,
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId()
        );

        if (rowsAffected == 0) {
            throw new ResourceNotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        // Обновляем жанры (удаляем старые и добавляем новые)
        deleteGenres(film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        log.debug("Обновлён фильм: id={}, name={}", film.getId(), film.getName());

        // Загружаем полные данные фильма
        return findById(film.getId()).orElse(film);
    }

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.name as mpa_name FROM practicum.films f " +
                "LEFT JOIN practicum.mpa m ON f.mpa_id = m.id";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);
        films.forEach(this::loadGenresAndLikes);

        return films;
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String sql = "SELECT f.*, m.name as mpa_name FROM practicum.films f " +
                "LEFT JOIN practicum.mpa m ON f.mpa_id = m.id WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);
            if (film != null) {
                loadGenresAndLikes(film);
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM practicum.films WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        if (rowsAffected > 0) {
            log.debug("Удалён фильм: id={}", id);
            return true;
        }
        return false;
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        String sql = "INSERT INTO practicum.film_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Лайк добавлен: filmId={}, userId={}", filmId, userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        String sql = "DELETE FROM practicum.film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Лайк удалён: filmId={}, userId={}", filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, COUNT(l.user_id) as likes_count " +
                "FROM practicum.films f " +
                "LEFT JOIN practicum.mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN practicum.film_likes l ON f.id = l.film_id " +
                "GROUP BY f.id, m.id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);
        films.forEach(this::loadGenresAndLikes);

        return films;
    }

    private void validateMpa(Mpa mpa) {
        if (mpa != null && mpa.getId() != null) {
            String sql = "SELECT COUNT(*) FROM practicum.mpa WHERE id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mpa.getId());
            if (count == null || count == 0) {
                throw new ResourceNotFoundException("Рейтинг MPA с id=" + mpa.getId() + " не существует");
            }
        }
    }

    private void validateGenres(Set<Genre> genres) {
        if (genres != null && !genres.isEmpty()) {
            for (Genre genre : genres) {
                if (genre.getId() != null) {
                    String sql = "SELECT COUNT(*) FROM practicum.genres WHERE id = ?";
                    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genre.getId());
                    if (count == null || count == 0) {
                        throw new ResourceNotFoundException("Жанр с id=" + genre.getId() + " не существует");
                    }
                }
            }
        }
    }

    private void saveGenres(Integer filmId, Set<Genre> genres) {
        String sql = "INSERT INTO practicum.film_genres (film_id, genre_id) VALUES (?, ?)";

        // Удаляем дубликаты жанров
        Set<Genre> uniqueGenres = new LinkedHashSet<>(genres);

        List<Object[]> batchArgs = uniqueGenres.stream()
                .map(genre -> new Object[]{filmId, genre.getId()})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.debug("Сохранены жанры для фильма: filmId={}, genres={}", filmId, uniqueGenres.size());
    }

    private void deleteGenres(Integer filmId) {
        String sql = "DELETE FROM practicum.film_genres WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }

    private void loadGenresAndLikes(Film film) {
        // Загружаем жанры с именами
        String genreSql = "SELECT g.id, g.name FROM practicum.genres g " +
                "INNER JOIN practicum.film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? ORDER BY fg.genre_id";

        List<Genre> genres = jdbcTemplate.query(genreSql, genreRowMapper, film.getId());
        film.setGenres(new LinkedHashSet<>(genres));

        // Загружаем лайки
        String likesSql = "SELECT user_id FROM practicum.film_likes WHERE film_id = ?";
        Set<Integer> likes = new HashSet<>(jdbcTemplate.query(likesSql,
                (rs, rowNum) -> rs.getInt("user_id"), film.getId()));
        film.setLikes(likes);
    }
}