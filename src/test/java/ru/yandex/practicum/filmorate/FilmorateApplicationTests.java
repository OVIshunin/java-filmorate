package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FilmorateApplicationTests {

    @Test
    void testValidFilmShouldPassValidation() {
        Film film = createValidFilm();
        assertDoesNotThrow(film::validate, "Валидный фильм должен проходить валидацию без ошибок");
    }

    @Test
    void testEmptyFilmNameShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setName("");
        ValidationException ex = assertThrows(ValidationException.class, film::validate);
        assertEquals("Название фильма не может быть пустым.", ex.getMessage());
    }

    @Test
    void testNullFilmNameShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setName(null);
        ValidationException ex = assertThrows(ValidationException.class, film::validate);
        assertEquals("Название фильма не может быть пустым.", ex.getMessage());
    }

    @Test
    void testLongFilmDescriptionShouldThrowValidationException() {
        Film film = createValidFilm();
        String longDescription = "A".repeat(201);
        film.setDescription(longDescription);
        ValidationException ex = assertThrows(ValidationException.class, film::validate);
        assertEquals("Описание фильма не может превышать 200 символов.", ex.getMessage());
    }

    @Test
    void testFilmReleaseDateBefore1895ShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1890, 1, 1));
        ValidationException ex = assertThrows(ValidationException.class, film::validate);
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года.", ex.getMessage());
    }

    @Test
    void testFilmZeroDurationShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDuration(0);
        ValidationException ex = assertThrows(ValidationException.class, film::validate);
        assertEquals("Продолжительность фильма должна быть положительным числом.", ex.getMessage());
    }

    @Test
    void testFilmNegativeDurationShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDuration(-5);
        ValidationException ex = assertThrows(ValidationException.class, film::validate);
        assertEquals("Продолжительность фильма должна быть положительным числом.", ex.getMessage());
    }

    @Test
    void testValidUserShouldPassValidation() {
        User user = createValidUser();
        assertDoesNotThrow(user::validate, "Валидный пользователь должен проходить валидацию без ошибок");
    }

    @Test
    void testEmptyEmailShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail("");
        ValidationException ex = assertThrows(ValidationException.class, user::validate);
        assertEquals("Электронная почта не может быть пустой.", ex.getMessage());
    }

    @Test
    void testNullEmailShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail(null);
        ValidationException ex = assertThrows(ValidationException.class, user::validate);
        assertEquals("Электронная почта не может быть пустой.", ex.getMessage());
    }

    @Test
    void testInvalidEmailFormatShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail("invalid-email");
        ValidationException ex = assertThrows(ValidationException.class, user::validate);
        assertEquals("Электронная почта должна содержать символ @.", ex.getMessage());
    }

    @Test
    void testEmptyLoginShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin("");
        ValidationException ex = assertThrows(ValidationException.class, user::validate);
        assertEquals("Логин не может быть пустым.", ex.getMessage());
    }

    @Test
    void testNullLoginShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin(null);
        ValidationException ex = assertThrows(ValidationException.class, user::validate);
        assertEquals("Логин не может быть пустым.", ex.getMessage());
    }

    @Test
    void testUserBirthdayInFutureShouldThrowValidationException() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));
        ValidationException ex = assertThrows(ValidationException.class, user::validate);
        assertEquals("Дата рождения не может быть в будущем.", ex.getMessage());
    }

    @Test
    void testUserBirthdayTodayShouldPassValidation() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now());
        assertDoesNotThrow(user::validate, "Дата рождения сегодня должна быть валидной");
    }

    @Test
    void testUserBirthdayPastShouldPassValidation() {
        User user = createValidUser();
        user.setBirthday(LocalDate.of(1990, 5, 15));
        assertDoesNotThrow(user::validate, "Прошлая дата рождения должна быть валидной");
    }


    //Создадим дефолтные рабочие экземпляры, чтобы потом вносить в них ошибки и тестировать
    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Приключения Шурика");
        film.setDescription("Комедия советских годов");
        film.setReleaseDate(LocalDate.of(1965, 6, 5));
        film.setDuration(120);
        return film;
    }

    private User createValidUser() {
        User user = new User();
        user.setEmail("oleg@yandex.ru");
        user.setLogin("oleg1986");
        user.setName("Олег Петров");
        user.setBirthday(LocalDate.of(1986, 10, 23));
        return user;
    }
}