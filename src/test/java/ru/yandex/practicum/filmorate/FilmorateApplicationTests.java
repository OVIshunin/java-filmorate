package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import jakarta.validation.*;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {

    private final Validator validator;

    public FilmorateApplicationTests() {
        // Создаём валидатор для тестирования аннотаций
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidFilmShouldPassValidation() {
        Film film = createValidFilm();
        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Валидный фильм не должен иметь нарушений");
    }

    @Test
    void testEmptyFilmNameShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setName("");

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertEquals(1, violations.size());

        ConstraintViolation<Film> violation = violations.iterator().next();
        assertEquals("Название фильма не может быть пустым.", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testNullFilmNameShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setName(null);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertEquals(1, violations.size());

        ConstraintViolation<Film> violation = violations.iterator().next();
        assertEquals("Название фильма не может быть пустым.", violation.getMessage());
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    void testLongFilmDescriptionShouldThrowValidationException() {
        Film film = createValidFilm();
        String longDescription = "A".repeat(201);
        film.setDescription(longDescription);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertEquals(1, violations.size());

        ConstraintViolation<Film> violation = violations.iterator().next();
        assertEquals("Описание фильма не может превышать 200 символов.", violation.getMessage());
        assertEquals("description", violation.getPropertyPath().toString());
    }

    @Test
    void testFilmReleaseDateBefore1895ShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1890, 1, 1));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertEquals(1, violations.size());

        ConstraintViolation<Film> violation = violations.iterator().next();
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года.", violation.getMessage());
        assertEquals("releaseDate", violation.getPropertyPath().toString());
    }

    @Test
    void testFilmReleaseDateExactly1895ShouldPassValidation() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Дата 28 декабря 1895 года должна быть валидной");
    }

    @Test
    void testFilmZeroDurationShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDuration(0);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertEquals(1, violations.size());

        ConstraintViolation<Film> violation = violations.iterator().next();
        assertEquals("Продолжительность фильма должна быть положительным числом.", violation.getMessage());
        assertEquals("duration", violation.getPropertyPath().toString());
    }

    @Test
    void testFilmNegativeDurationShouldThrowValidationException() {
        Film film = createValidFilm();
        film.setDuration(-5);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertEquals(1, violations.size());

        ConstraintViolation<Film> violation = violations.iterator().next();
        assertEquals("Продолжительность фильма должна быть положительным числом.", violation.getMessage());
        assertEquals("duration", violation.getPropertyPath().toString());
    }

    @Test
    void testValidUserShouldPassValidation() {
        User user = createValidUser();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Валидный пользователь не должен иметь нарушений");
    }

    @Test
    void testEmptyEmailShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail("");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Электронная почта не может быть пустой.", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void testNullEmailShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Электронная почта не может быть пустой.", violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void testInvalidEmailFormatShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail("invalid-email");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Электронная почта должна содержать символ @ и быть корректным адресом.",
                violation.getMessage());
        assertEquals("email", violation.getPropertyPath().toString());
    }

    @Test
    void testEmailWithoutAtSymbolShouldThrowValidationException() {
        User user = createValidUser();
        user.setEmail("invalid.email.com");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Электронная почта должна содержать символ @ и быть корректным адресом.",
                violation.getMessage());
    }

    @Test
    void testEmptyLoginShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin("");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Логин не может быть пустым.", violation.getMessage());
        assertEquals("login", violation.getPropertyPath().toString());
    }

    @Test
    void testNullLoginShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Логин не может быть пустым.", violation.getMessage());
        assertEquals("login", violation.getPropertyPath().toString());
    }

    @Test
    void testLoginWithSpacesShouldThrowValidationException() {
        User user = createValidUser();
        user.setLogin("oleg 1986");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Логин не может содержать пробелы.", violation.getMessage());
        assertEquals("login", violation.getPropertyPath().toString());
    }

    @Test
    void testNullBirthdayShouldThrowValidationException() {
        User user = createValidUser();
        user.setBirthday(null);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Дата рождения не может быть пустой.", violation.getMessage());
        assertEquals("birthday", violation.getPropertyPath().toString());
    }

    @Test
    void testUserBirthdayInFutureShouldThrowValidationException() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());

        ConstraintViolation<User> violation = violations.iterator().next();
        assertEquals("Дата рождения не может быть в будущем.", violation.getMessage());
        assertEquals("birthday", violation.getPropertyPath().toString());
    }

    @Test
    void testUserBirthdayTodayShouldPassValidation() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now());

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());
    }

    @Test
    void testUserBirthdayPastShouldPassValidation() {
        User user = createValidUser();
        user.setBirthday(LocalDate.of(1990, 5, 15));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Прошлая дата рождения должна быть валидной");
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