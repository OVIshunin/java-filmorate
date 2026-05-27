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

    @Override
    public Film create(Film film) {
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

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        return findById(id).orElse(film);
    }

    @Override
    public Film update(Film film) {
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

        deleteGenres(film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        return findById(film.getId()).orElse(film);
    }

    @Override
    public List<Film> findAll() {
        String filmsSql = "SELECT f.*, m.name as mpa_name FROM practicum.films f " +
                "LEFT JOIN practicum.mpa m ON f.mpa_id = m.id";
        List<Film> films = jdbcTemplate.query(filmsSql, filmRowMapper);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String sql = "SELECT f.*, m.name as mpa_name FROM practicum.films f " +
                "LEFT JOIN practicum.mpa m ON f.mpa_id = m.id WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);
            if (film != null) {
                loadGenresForFilm(film);
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
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, " +
                "m.name as mpa_name, " +
                "COALESCE(l.likes_count, 0) as likes_count " +
                "FROM practicum.films f " +
                "LEFT JOIN practicum.mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN ( " +
                "    SELECT film_id, COUNT(user_id) as likes_count " +
                "    FROM practicum.film_likes " +
                "    GROUP BY film_id " +
                ") l ON f.id = l.film_id " +
                "ORDER BY likes_count DESC, f.id ASC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getInt("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date") != null
                    ? rs.getDate("release_date").toLocalDate() : null);
            film.setDuration(rs.getInt("duration"));

            Mpa mpa = new Mpa();
            mpa.setId(rs.getInt("mpa_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);

            return film;
        }, count);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

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
        if (genres == null || genres.isEmpty()) {
            return;
        }

        Set<Integer> genreIds = genres.stream()
                .map(Genre::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (genreIds.isEmpty()) {
            return;
        }

        String placeholders = genreIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT id FROM practicum.genres WHERE id IN (" + placeholders + ")";

        Set<Integer> existingIds = new HashSet<>(jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("id"), genreIds.toArray()));

        if (existingIds.size() != genreIds.size()) {
            Set<Integer> missingIds = genreIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toSet());

            String missing = missingIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            throw new ResourceNotFoundException("Жанры с id: " + missing + " не существуют");
        }
    }

    private void saveGenres(Integer filmId, Set<Genre> genres) {
        String sql = "INSERT INTO practicum.film_genres (film_id, genre_id) VALUES (?, ?)";

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

    private void loadGenresForFilm(Film film) {
        String sql = "SELECT g.id, g.name FROM practicum.genres g " +
                "INNER JOIN practicum.film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? ORDER BY g.id";

        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) ->
                new Genre(rs.getInt("id"), rs.getString("name")), film.getId());

        film.setGenres(new LinkedHashSet<>(genres));
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        String ids = films.stream()
                .map(Film::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String sql = "SELECT fg.film_id, g.id, g.name " +
                "FROM practicum.film_genres fg " +
                "INNER JOIN practicum.genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + ids + ") " +
                "ORDER BY fg.film_id, g.id";

        Map<Integer, Set<Genre>> genresByFilm = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            int filmId = rs.getInt("film_id");
            Genre genre = new Genre(rs.getInt("id"), rs.getString("name"));
            genresByFilm.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        });

        for (Film film : films) {
            film.setGenres(genresByFilm.getOrDefault(film.getId(), new LinkedHashSet<>()));
        }
    }
}