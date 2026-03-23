package ru.yandex.practicum.filmorate.storage.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FilmRowMapper implements RowMapper<Film> {

    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date") != null
                ? rs.getDate("release_date").toLocalDate()
                : null);
        film.setDuration(rs.getInt("duration"));

        // Устанавливаем MPA с именем
        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            Mpa mpa = new Mpa();
            mpa.setId(mpaId);
            // Пытаемся получить имя MPA из результата запроса
            try {
                String mpaName = rs.getString("mpa_name");
                mpa.setName(mpaName);
            } catch (SQLException e) {
                // Если колонки mpa_name нет, оставляем name как null
            }
            film.setMpa(mpa);
        }

        return film;
    }
}