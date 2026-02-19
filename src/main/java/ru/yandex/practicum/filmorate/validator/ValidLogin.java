package ru.yandex.practicum.filmorate.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LoginValidator.class)
public @interface ValidLogin {
    String message() default "Логин не может быть пустым или содержать пробелы.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}