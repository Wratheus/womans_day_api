package com.womansday.api.config;

import com.womansday.api.entity.Task;
import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import com.womansday.api.enums.TaskType;
import com.womansday.api.repository.TaskRepository;
import com.womansday.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

        @Value("${admin.login}")
        private String adminLogin;

        @Value("${admin.password}")
        private String adminPassword;

        @Value("${spring.profiles.active:dev}")
        private String activeProfile;

        @Override
        public void run(String... args) {
                if (activeProfile.contains("prod")) {
                        if (adminPassword == null || adminPassword.isBlank()) {
                                throw new IllegalStateException(
                                                "Admin password must be configured for production! " +
                                                                "Set ADMIN_PASSWORD environment variable.");
                        }
                        if (adminPassword.length() < 12) {
                                throw new IllegalStateException(
                                                "Admin password must be at least 12 characters in production!");
                        }
                }

                if (userRepository.count() == 0) {
                        User admin = User.builder()
                                        .login(adminLogin.trim().toLowerCase())
                                        .passwordHash(passwordEncoder.encode(adminPassword))
                                        .firstName("Adminchik")
                                        .lastName("Administrator")
                                        .department("Administration")
                                        .role(Role.ADMIN)
                                        .build();
                        userRepository.save(admin);
                }

                if (taskRepository.count() == 0) {
                        List<Task> tasks = List.of(
                                        // ======= Сложное (50) =======
                                        Task.builder()
                                                        .title("Гламурная фотосессия «Богини офиса»")
                                                        .description("Гламурная фотосессия «Богини офиса». Не менее 3 человек.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Самое нелепое модельное позирование")
                                                        .description("Самое нелепое модельное позирование втроём с мужчиной.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Фото между двумя людьми с одинаковым именем")
                                                        .description("Сделайте фото между двумя людьми с одинаковым именем.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Повторить сцену из фильма офисным составом")
                                                        .description("Повторите известную сцену из фильма офисным составом. Не более 3 человек.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Фото с Денисом и табличкой «Я руковожу этим цирком»")
                                                        .description("Фото с Денисом с табличкой «Я руковожу этим цирком». Не засчитывается, если делать в его кабинете. Вылавливать в офисе.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Мини-флешмоб из 3 человек синхронно")
                                                        .description("Мини-флешмоб из 3 человек синхронно. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Скороговорка без ошибок")
                                                        .description("Произнесите скороговорку «Сизо-сиреневенький подплинтусозаврик с изподподвыподвертом» без ошибок. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("«Эйяфьядлайёкюдль» без ошибок")
                                                        .description("Произнесите «Эйяфьядлайёкюдль» без ошибок. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Синхронное фото-позирование как близнецы")
                                                        .description("Сделайте синхронное фото-позирование как близнецы.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Сцена ссоры из сериала")
                                                        .description("Сыграйте сцену ссоры из сериала. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Интервью друг у друга как будто вы звезды")
                                                        .description("Сделайте интервью друг у друга как будто вы звезды. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Строчка из любимой песни с любым парнем")
                                                        .description("Исполните с любым парнем строчку из любимой песни. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Живое сердечко из людей")
                                                        .description("Постройте живую фигуру сердечка из людей. Фото сверху.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Трейлер «фильма про ваш офис»")
                                                        .description("Снимите трейлер «фильма про ваш офис». Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Ревность без слов")
                                                        .description("Изобразите ревность, не говоря ни слова. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Пародия на новости про ваш офис")
                                                        .description("Снимите пародию на новости про ваш офис. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Freeze-frame сцена")
                                                        .description("Организуйте сцену freeze-frame: все замирают. Формат: видео.")
                                                        .reward(50)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),

                                        // ======= Среднее (35) =======
                                        Task.builder()
                                                        .title("Рассмешить самого серьёзного айтишника")
                                                        .description("Найдите самого серьёзного айтишника и рассмешите. Ну или хотя бы попробуйте это сделать.. Формат: видео.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Косички парню с самыми длинными волосами")
                                                        .description("Найдите парня с самыми длинными волосами, заплетите косички и сделайте селфи. Грустные фото не принимаются.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Романтично объяснить «что он делает»")
                                                        .description("Убедите айтишника объяснить «что он делает» максимально романтично во время написания кода. Формат: видео.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Фото с бухгалтером в милом образе")
                                                        .description("Сделайте фото с бухгалтером в милом образе.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Фото с самым галантным джентльменом офиса")
                                                        .description("Сделайте фото с самым галантным джентльменом офиса.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Мужчины офиса — ваш фан-клуб")
                                                        .description("Сцена: мужчины офиса — ваш фан-клуб. Формат: фото.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Признание в любви рабочему стулу")
                                                        .description("Снимите драматичное видео признания в любви своему рабочему стулу.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Комплименты синхронно без смеха")
                                                        .description("Синхронно скажите комплименты друг другу без смеха. Формат: видео.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Случайный папарацци поймал меня")
                                                        .description("Сделайте фото «случайный папарацци поймал меня».")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("5 искренних комплиментов разным людям")
                                                        .description("Раздайте 5 искренних комплиментов разным людям. Формат: видео-нарезка.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Пройтись по офису как модель")
                                                        .description("Пройдитесь по офису как модель на показе высокой моды. Формат: видео.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Собрать 3 разных смеха")
                                                        .description("Соберите 3 разных смеха людей на видео.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Иностранный язык: выучить 3 слова")
                                                        .description("Найдите коллегу, знающего иностранный язык. Выучите 3 слова и без ошибок повторите на видео. Английский не использовать.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Попросить техподдержку «починить настроение»")
                                                        .description("Попросите техподдержку «починить настроение». Формат: видео.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Сообщение в общий чат «Я никогда не ...»")
                                                        .description("Напишите в общий чат сообщение: «Я никогда не ...». Продолжите. Желательно кринжовое.")
                                                        .reward(35)
                                                        .type(TaskType.TEXT)
                                                        .build(),
                                        Task.builder()
                                                        .title("Парная скульптура «Дружба века»")
                                                        .description("Изобразите парную скульптуру «Дружба века».")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("10 причин любить понедельник")
                                                        .description("Назовите 10 причин любить понедельник. Формат: видео.")
                                                        .reward(35)
                                                        .type(TaskType.MEDIA)
                                                        .build(),

                                        // ======= Легкое (20) =======
                                        Task.builder()
                                                        .title("Повтори мем")
                                                        .description("Повтори любимый мем и сделай фото или видео")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Slow-mo: драматично нажимает Enter")
                                                        .description("Slow-mo видео, как кто-то драматично нажимает Enter.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("Фото «до/после: обычный день — 8 марта»")
                                                        .description("Фото «до/после: обычный день — 8 марта».")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Злодейский смех")
                                                        .description("Злодейский смех так, чтобы кто-то обернулся. Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Изобразить любимый мем")
                                                        .description("Изобразите любимый мем. Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Спародировать коллегу")
                                                        .description("Спародируйте коллегу. Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Фото с самым счастливым человеком")
                                                        .description("Сделайте фото с человеком, который выглядит счастливее всех сегодня.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(false)
                                                        .build(),
                                        Task.builder()
                                                        .title("5 эмоций за 10 секунд")
                                                        .description("Изобразите 5 эмоций за 10 секунд одним дублем. Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Реклама воды из кулера как люксового парфюма")
                                                        .description("Изобразите рекламу воды из кулера как люксового парфюма. Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("TikTok-танец без музыки")
                                                        .description("Снимите TikTok-танец без музыки (музыка «в голове»). Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Мини-интервью «что делает тебя счастливым?»")
                                                        .description("Запишите мини-интервью: «что делает тебя счастливым?». Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .collaborative(true)
                                                        .build(),
                                        Task.builder()
                                                        .title("Мем-фото в офисе")
                                                        .description("Создайте мем-фото прямо в офисе.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Фото «как будто вы застряли во времени»")
                                                        .description("Сделайте фото «как будто вы застряли во времени».")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Сцена «я выиграла миллиард»")
                                                        .description("Сыграйте сцену «я выиграла миллиард». Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Персонаж фильма ужасов в офисе")
                                                        .description("Изобразите персонажа фильма ужасов в офисе. Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build(),
                                        Task.builder()
                                                        .title("Изобразить животное")
                                                        .description("Изобразите животное. Формат: видео.")
                                                        .reward(20)
                                                        .type(TaskType.MEDIA)
                                                        .build());

                        taskRepository.saveAll(tasks);
                }
        }
}