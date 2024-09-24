package io.proj3ct.Spring.service;

import io.proj3ct.Spring.config.BotConfig;
import io.proj3ct.Spring.model.Restaurants;
import io.proj3ct.Spring.model.User;
import io.proj3ct.Spring.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantService restaurantService;

    final BotConfig config;
    private static final String ERROR_TEXT = "Сталася помилка: ";

    static final String HELP_TEXT =
            "Ви можете виконувати команди з головного меню ліворуч або шляхом введення команди:\n\n" +
                    "Введіть /start , щоб побачити вітальне повідомлення\n" +
                    "Введіть /help щоб знову побачити це повідомлення";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "отримати вітальне повідомлення"));
        listofCommands.add(new BotCommand("/help", "інформація як користуватися цим ботом"));

        try {
            registerBotCommand(listofCommands);

        } catch (TelegramApiException e) {
            log.error("Помилка встановлення списку команд бота: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    prepareAndSendMessage(chatId, HELP_TEXT);
                    break;
                case "Пошук за районом міста":
                    handleDistrictSearchButton(chatId);
                    break;
                case "Випадковий заклад":
                    sendRandomRestaurant(chatId);
                    break;
                case "Назад":
                case "Повернутися":
                case "Повернутися о головного меню":
                    sendMessage(chatId, "Ви повернулися до головного меню");
                    break;
                case "Ще 5 закладів":
                    handleMoreRestaurantsButton(chatId);
                    break;
                case "Ще 5 закладів за концепцією":
                    handleMoreRestaurantsByConceptButton(chatId);
                    break;
                case "Пошук за концепцією закладу":
                    handleConceptSearchButton(chatId);
                    break;
                case "Пошук за видом кухні":
                    handleCuisineSearchButton(chatId);
                    break;
                case "Фастфуд":
                    handleConceptSelection(chatId, messageText);
                    break;
                default:
                    // Check if the message is related to district selection
                    if (userDistrictSearchState.containsKey(chatId) && userDistrictSearchState.get(chatId).selectedDistrict == null) {
                        handleDistrictSelection(chatId, messageText);
                    }
                    // Check if the message is related to concept selection
                    else if (userConceptSearchState.containsKey(chatId) && userConceptSearchState.get(chatId).selectedConcept == null) {
                        handleConceptSelection(chatId, messageText);
                    }
                    // Check if the message is related to cuisine type selection
                    else if (userCuisineSearchState.containsKey(chatId) && userCuisineSearchState.get(chatId).selectedCuisineType == null) {
                        handleCuisineSelection(chatId, messageText);
                    } else {
                        sendMessage(chatId, "Невідома команда");
                    }
            }
        }
    }

    private void registerUser(org.telegram.telegrambots.meta.api.objects.Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("користувач збережений: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = ("Доброго дня, " + name + ", радий Вас вітати у боті!");
        sendMessage(chatId, answer);
        log.info("Відповіли користувачеві " + name);
    }

    private void sendRandomRestaurant(long chatId) {
        Restaurants randomRestaurant = restaurantService.getRandomRestaurant();
        if (randomRestaurant != null) {
            String restaurantInfo = formatRestaurantInfo(randomRestaurant);
            sendMessage(chatId, restaurantInfo);
        } else {
            sendMessage(chatId, "На жаль, не вдалося знайти інформацію про випадковий заклад.");
        }
    }

    private String formatRestaurantInfo(Restaurants restaurant) {
        return "Назва: " + restaurant.getName() +
                "\nАдреса: " + restaurant.getAddress() +
                "\nРайон: " + restaurant.getDistrict() +
                "\nТип кухні: " + restaurant.getCuisineType() +
                "\nКонцепція: " + restaurant.getConcept() +
                "\nГрафік роботи: " + restaurant.getWorkingHours() +
                "\nФото закладу: " + restaurant.getImageUrl() +
                "\nПосилання на меню: " + restaurant.getMenuLink() +
                "\nПосилання на відгуки: " + restaurant.getReviewsLink();
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Перша рядок клавіатури
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Пошук за концепцією закладу");
        keyboardRows.add(row1);

        // Друга рядок клавіатури
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Пошук за видом кухні");
        row2.add("Випадковий заклад");
        keyboardRows.add(row2);

        // Третя рядок клавіатури
        KeyboardRow row3 = new KeyboardRow();
        row3.add("Пошук за районом міста");
        keyboardRows.add(row3);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }


    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            log.error("Сталася помилка: " + e.getMessage());
        }

    }

    private void handleDistrictSearchButton(long chatId) {
        String messageText = "Оберіть район міста Одеса";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Київський");
        row1.add("Пересипський");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Приморський");
        row2.add("Хаджибейський");
        keyboardRows.add(row2);

        KeyboardRow backButtonRow = new KeyboardRow();
        backButtonRow.add("Назад");
        keyboardRows.add(backButtonRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }

        userDistrictSearchState.put(chatId, new DistrictSearchState());
    }

    class DistrictSearchState {
        String selectedDistrict;
        int displayedRestaurantsCount;

        public DistrictSearchState() {
            this.selectedDistrict = null;
            this.displayedRestaurantsCount = 0;
        }
    }

    Map<Long, DistrictSearchState> userDistrictSearchState = new HashMap<>();

    private void handleDistrictSelection(long chatId, String district) {
        DistrictSearchState state = userDistrictSearchState.get(chatId);
        state.selectedDistrict = district;
        state.displayedRestaurantsCount = 0;
        sendRestaurantsByDistrict(chatId, district);
    }

    private void sendRestaurantsByDistrict(long chatId, String district) {
        List<Restaurants> restaurantsList = restaurantService.getRestaurantsByDistrict(district);
        if (restaurantsList.isEmpty()) {
            sendMessage(chatId, "На жаль, не вдалося знайти заклади у вибраному районі.");
        } else {
            sendFirstFiveRestaurants(chatId, restaurantsList);
        }
    }

    private void sendFirstFiveRestaurants(long chatId, List<Restaurants> restaurantsList) {
        int startIndex = userDistrictSearchState.get(chatId).displayedRestaurantsCount;
        int endIndex = Math.min(startIndex + 5, restaurantsList.size());
        for (int i = startIndex; i < endIndex; i++) {
            Restaurants restaurant = restaurantsList.get(i);
            String restaurantInfo = formatRestaurantInfo(restaurant);
            sendMessage(chatId, restaurantInfo);
        }

        userDistrictSearchState.get(chatId).displayedRestaurantsCount = endIndex;

        if (endIndex < restaurantsList.size()) {
            sendAdditionalButtons(chatId);
        } else {
            sendMainMenuButton(chatId);
        }
    }

    private void sendAdditionalButtons(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Ще 5 закладів");
        row.add("Повернутися ");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Бажаєте переглянути ще заклади?");
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void sendMainMenuButton(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Повернутися ");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Ви переглянули всі заклади за обраним фільтром. Бажаєте повернутися до головного меню?");
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void handleMoreRestaurantsButton(long chatId) {
        DistrictSearchState state = userDistrictSearchState.get(chatId);
        String district = state.selectedDistrict;
        List<Restaurants> restaurantsList = restaurantService.getRestaurantsByDistrict(district);
        sendRestaurantsByDistrict(chatId, district);
    }

    private void handleConceptSearchButton(long chatId) {
        String messageText = "Оберіть концепцію закладу";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Фастфуд");
        row1.add("Ресторан");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Кафе");
        row2.add("Бар");
        keyboardRows.add(row2);

        KeyboardRow backButtonRow = new KeyboardRow();
        backButtonRow.add("Назад");
        keyboardRows.add(backButtonRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }

        userConceptSearchState.put(chatId, new ConceptSearchState());
    }

    class ConceptSearchState {
        String selectedConcept;
        int displayedRestaurantsCount;

        public ConceptSearchState() {
            this.selectedConcept = null;
            this.displayedRestaurantsCount = 0;
        }
    }

    Map<Long, ConceptSearchState> userConceptSearchState = new HashMap<>();

    private void handleConceptSelection(long chatId, String concept) {
        ConceptSearchState state = userConceptSearchState.get(chatId);
        state.selectedConcept = concept;
        state.displayedRestaurantsCount = 0;
        sendRestaurantsByConcept(chatId, concept);
    }

    private void sendRestaurantsByConcept(long chatId, String concept) {
        List<Restaurants> restaurantsList = restaurantService.getRestaurantsByConcept(concept);
        if (restaurantsList.isEmpty()) {
            sendMessage(chatId, "На жаль, не вдалося знайти заклади за обраною концепцією.");
        } else {
            sendFirstFiveRestaurantsByConcept(chatId, restaurantsList);
        }
    }


    private void sendFirstFiveRestaurantsByConcept(long chatId, List<Restaurants> restaurantsList) {
        int startIndex = userConceptSearchState.get(chatId).displayedRestaurantsCount;
        int endIndex = Math.min(startIndex + 5, restaurantsList.size());
        for (int i = startIndex; i < endIndex; i++) {
            Restaurants restaurant = restaurantsList.get(i);
            String restaurantInfo = formatRestaurantInfo(restaurant);
            sendMessage(chatId, restaurantInfo);
        }

        userConceptSearchState.get(chatId).displayedRestaurantsCount = endIndex;

        if (endIndex < restaurantsList.size()) {
            sendAdditionalButtonsByConcept(chatId);
        } else {
            sendMainMenuButton(chatId);
        }
    }
    private void registerBotCommand(List<BotCommand> listofCommands) throws TelegramApiException {
        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(listofCommands);
        execute(setMyCommands);
    }


    private void handleMoreRestaurantsByConceptButton(long chatId) {
        ConceptSearchState state = userConceptSearchState.get(chatId);
        String concept = state.selectedConcept;
        List<Restaurants> restaurantsList = restaurantService.getRestaurantsByConcept(concept);
        sendRestaurantsByConcept(chatId, concept);
    }

    private void sendAdditionalButtonsByConcept(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Ще 5 закладів за концепцією");
        row.add("Повернутися");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Бажаєте переглянути ще заклади за цією концепцією?");
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }


    private void handleCuisineSearchButton(long chatId) {
        String messageText = "Оберіть вид кухні:";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Вегетеріанська");
        row1.add("Грузинська");
        row1.add("Українська");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Французька");
        row2.add("Японська");
        row2.add("Корейська");
        keyboardRows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Італійська");
        row3.add("Американська");
        row3.add("Європейська");
        keyboardRows.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Кава та десерти");
        keyboardRows.add(row4);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }

        userCuisineSearchState.put(chatId, new CuisineSearchState());
    }
    private void handleCuisineSelection(long chatId, String cuisineType) {
        CuisineSearchState state = userCuisineSearchState.get(chatId);
        state.selectedCuisineType = cuisineType;
        state.displayedRestaurantsCount = 0;
        sendRestaurantsByCuisineType(chatId, cuisineType);
    }

    private void sendRestaurantsByCuisineType(long chatId, String cuisineType) {
        List<Restaurants> restaurantsList = restaurantService.getRestaurantsByCuisineType(cuisineType);
        if (restaurantsList.isEmpty()) {
            sendMessage(chatId, "На жаль, не вдалося знайти заклади з вибраним видом кухні.");
        } else {
            sendFirstFiveRestaurantsByCuisineType(chatId, restaurantsList);
        }
    }

    private void sendFirstFiveRestaurantsByCuisineType(long chatId, List<Restaurants> restaurantsList) {
        int startIndex = userCuisineSearchState.get(chatId).displayedRestaurantsCount;
        int endIndex = Math.min(startIndex + 5, restaurantsList.size());
        for (int i = startIndex; i < endIndex; i++) {
            Restaurants restaurant = restaurantsList.get(i);
            String restaurantInfo = formatRestaurantInfo(restaurant);
            sendMessage(chatId, restaurantInfo);
        }

        userCuisineSearchState.get(chatId).displayedRestaurantsCount = endIndex;

        if (endIndex < restaurantsList.size()) {
            sendAdditionalButtonsByCuisineType(chatId);
        } else {
            sendMainMenuButton(chatId);
        }
    }

    private void sendAdditionalButtonsByCuisineType(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Ще 5 закладів за видом кухні");
        row.add("Повернутися ");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Бажаєте переглянути ще заклади за цим видом кухні?");
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }
    class CuisineSearchState {
        String selectedCuisineType;
        int displayedRestaurantsCount;

        public CuisineSearchState() {
            this.selectedCuisineType = null;
            this.displayedRestaurantsCount = 0;
        }
    }

    Map<Long, CuisineSearchState> userCuisineSearchState = new HashMap<>();

}