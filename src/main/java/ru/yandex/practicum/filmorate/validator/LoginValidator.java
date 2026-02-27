package ru.yandex.practicum.filmorate.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LoginValidator implements ConstraintValidator<ValidLogin, String> {

    @Override
    public boolean isValid(String login, ConstraintValidatorContext context) {
        if (login == null || login.trim().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Логин не может быть пустым.")
                    .addConstraintViolation();
            return false;
        }

        if (login.contains(" ")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Логин не может содержать пробелы.")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}