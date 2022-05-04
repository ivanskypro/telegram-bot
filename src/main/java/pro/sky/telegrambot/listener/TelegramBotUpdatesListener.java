package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.BotRepository;

import javax.annotation.PostConstruct;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

   private final BotRepository botRepository;

    private final TelegramBot telegramBot;

   public TelegramBotUpdatesListener(BotRepository botRepository, TelegramBot telegramBot) {
        this.botRepository = botRepository;
       this.telegramBot = telegramBot;
   }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        String startMessageText = "Привет! Напиши напоминание в следующем формате: ДД.ММ.ГГГГ ЧЧ:ММ текст напоминания и я тебе напомню";
        updates.forEach(update -> {
            try {
                Long chatId = update.message().chat().id();
                logger.info("Processing update: {}", update);
                if (update.message().text().equals("/start")) {
                    SendMessage message = new SendMessage(chatId, startMessageText);
                    telegramBot.execute(message);
                }
            else{
                saveTask(update);
            }}
            catch (NullPointerException e){
                System.out.println("Бот перезапущен");
            }

        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void saveTask (Update update) {
        NotificationTask task = new NotificationTask();
        Long chatId = update.message().chat().id();
        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)",Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(update.message().text());
        if (matcher.matches()) {
            logger.info("try to save task");
            try {
                LocalDateTime dateTime = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                task.setDateTime(dateTime);
                String message = matcher.group(3);
                task.setMessage(message);
                task.setChatId(chatId);
            botRepository.save(task);
            logger.info("saving was successful");
            telegramBot.execute(new SendMessage(chatId, "Напоминание сохранено"));
            }
            catch (DateTimeParseException d){
                telegramBot.execute(new SendMessage(chatId,"Нормально напиши запрос, дружочек, формата ДД.ММ.ГГГГ ЧЧ:ММ текст напоминания. Напоминать ничего не буду."));
                logger.info("update was wrong. saving was interrupted");
            }}
            else{
                telegramBot.execute(new SendMessage(chatId,"Нормально напиши запрос, дружочек, формата ДД.ММ.ГГГГ ЧЧ:ММ текст напоминания. Напоминать ничего не буду."));
        } }

@Scheduled (cron = "0 0/1 * * * *")
    public void notificationChecking (){
       logger.info("checking base");
       try {
       botRepository.getNotificationTaskByDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
       NotificationTask task = botRepository.getNotificationTaskByDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
           if(task.getDateTime().equals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))){
           String messageForChat = "Привет! Напоминаю, что ты просил напомнить о " + task.getMessage();
           SendMessage message = new SendMessage(task.getChatId(), messageForChat);
           telegramBot.execute(message);
       }}
       catch (NullPointerException e){
           System.out.println("Ничего не нашёл, ищу дальше");
       }}}
