package pro.sky.telegrambot.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.model.NotificationTask;

import java.time.LocalDateTime;

@Repository
public interface BotRepository extends JpaRepository<NotificationTask, Long > {

    NotificationTask getNotificationTaskByDateTime(LocalDateTime dateTime);
}
