package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import jakarta.validation.constraints.*;
import ru.yandex.practicum.filmorate.validator.ValidLogin;

import java.time.LocalDate;

@Data
public class User {
    private Integer id;

    @NotBlank(message = "Электронная почта не может быть пустой.")
    @Email(message = "Электронная почта должна содержать символ @ и быть корректным адресом.")
    private String email;

    @ValidLogin
    private String login;

    private String name;

    @NotNull(message = "Дата рождения не может быть пустой.")
    @Past(message = "Дата рождения не может быть в будущем.")
    private LocalDate birthday;
}