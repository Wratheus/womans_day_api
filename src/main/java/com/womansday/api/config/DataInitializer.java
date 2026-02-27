package com.womansday.api.config;

import com.womansday.api.entity.Task;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.enums.TaskType;
import com.womansday.api.repository.TaskRepository;
import com.womansday.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DataInitializer implements CommandLineRunner {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Создаём админа если нет ни одного пользователя
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .login("admin")
                    .passwordHash(passwordEncoder.encode("admin"))
                    .department("Администрация")
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
        }

        // Создаём начальные задания если таблица пустая
        if (taskRepository.count() == 0) {
            List<Task> tasks = List.of(
                    Task.builder()
                            .title("Селфи с коллегой")
                            .description("Сделайте совместное селфи с коллегой из другого отдела")
                            .reward(500)
                            .type(TaskType.PHOTO)
                            .build(),
                    Task.builder()
                            .title("Комплимент дня")
                            .description("Напишите самый красивый комплимент коллеге")
                            .reward(300)
                            .type(TaskType.TEXT)
                            .build(),
                    Task.builder()
                            .title("Цветочный букет")
                            .description("Сфотографируйте самый красивый букет в офисе и напишите кому он предназначен")
                            .reward(700)
                            .type(TaskType.TEXT_AND_PHOTO)
                            .build(),
                    Task.builder()
                            .title("Поздравительная открытка")
                            .description("Напишите праздничное поздравление с 8 марта")
                            .reward(400)
                            .type(TaskType.TEXT)
                            .build(),
                    Task.builder()
                            .title("Командное фото")
                            .description("Сделайте групповое фото вашего отдела")
                            .reward(600)
                            .type(TaskType.PHOTO)
                            .build()
            );
            taskRepository.saveAll(tasks);
        }
    }
}
