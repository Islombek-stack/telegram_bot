package com.example.telegrambot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MyBot extends TelegramLongPollingBot {

    // Хранилище пользовательских данных
    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки обновления: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMessage(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        UserSession session = userSessions.computeIfAbsent(chatId, k -> new UserSession());

        if (messageText.startsWith("/")) {
            handleCommand(messageText, chatId, session);
        } else {
            handleTextMessage(messageText, chatId, session);
        }
    }

    private void handleCommand(String command, Long chatId, UserSession session) {
        switch (command) {
            case "/start":
                session.reset();
                sendWelcomeMessage(chatId);
                break;
            case "/help":
                sendHelpMessage(chatId);
                break;
            case "/stats":
                sendUserStats(chatId, session);
                break;
            case "/search":
                session.setMode(UserSession.Mode.SEARCH);
                sendSearchPrompt(chatId);
                break;
            case "/compare":
                session.setMode(UserSession.Mode.COMPARE);
                sendComparePrompt(chatId);
                break;
            case "/random":
                sendRandomCar(chatId);
                break;
            case "/quiz":
                sendCarQuiz(chatId, session);
                break;
            case "/favorites":
                sendFavorites(chatId, session);
                break;
            case "/brands":
                sendBrandSelection(chatId);
                break;
            case "/categories":
                if (session.getSelectedBrand() != null) {
                    sendCategorySelection(chatId, session.getSelectedBrand());
                } else {
                    sendTextMessage(chatId, "⚠️ Сначала выберите марку автомобиля!");
                }
                break;
            default:
                sendTextMessage(chatId, "Неизвестная команда. Используйте /help для списка команд.");
                break;
        }
    }


    private void handleTextMessage(String text, Long chatId, UserSession session) {
        switch (session.getMode()) {
            case SEARCH:
                handleSearchQuery(text, chatId, session);
                break;
            case COMPARE:
                handleCompareQuery(text, chatId, session);
                break;
            case NORMAL:
                if (text.equals("🏁 Выбрать марку")) {
                    sendBrandSelection(chatId);
                } else if (text.equals("🔍 Поиск моделей")) {
                    session.setMode(UserSession.Mode.SEARCH);
                    sendSearchPrompt(chatId);
                } else if (text.equals("📊 Статистика")) {
                    sendUserStats(chatId, session);
                } else if (text.equals("🎮 Викторина")) {
                    sendCarQuiz(chatId, session);
                } else if (text.equals("⭐️ Избранное")) {
                    sendFavorites(chatId, session);
                } else if (text.equals("🔄 Случайная модель")) {
                    sendRandomCar(chatId);
                } else if (text.equals("🏆 Топ модели")) {
                    sendTopModels(chatId);
                } else if (text.equals("📈 Категории")) {
                    sendCategoryStats(chatId);
                } else {
                    sendMainMenu(chatId, "Не понимаю ваш запрос. Выберите опцию из меню:");
                }
                break;
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        UserSession session = userSessions.computeIfAbsent(chatId, k -> new UserSession());

        try {
            if (callbackData.startsWith("brand_")) {
                String brand = callbackData.substring(6);
                session.setSelectedBrand(brand);
                session.incrementBrandViews(brand);
                sendCategorySelection(chatId, brand);
            } else if (callbackData.startsWith("category_")) {
                String category = callbackData.substring(9);
                session.setSelectedCategory(category);
                sendModelsList(chatId, session.getSelectedBrand(), category, 0);
            } else if (callbackData.startsWith("model_")) {
                String model = callbackData.substring(6);
                sendModelDetails(chatId, session.getSelectedBrand(), model, session);
            } else if (callbackData.startsWith("favorite_")) {
                String model = callbackData.substring(9);
                toggleFavorite(chatId, model, session);
            } else if (callbackData.startsWith("quiz_")) {
                handleQuizAnswer(chatId, messageId, callbackData.substring(5), session);
            } else if (callbackData.startsWith("page_")) {
                String[] parts = callbackData.substring(5).split("_");
                String brand = parts[0];
                String category = parts[1];
                int page = Integer.parseInt(parts[2]);
                sendModelsList(chatId, brand, category, page);
            } else if (callbackData.equals("back_to_brands")) {
                sendBrandSelection(chatId);
            } else if (callbackData.equals("back_to_categories")) {
                if (session.getSelectedBrand() != null) {
                    sendCategorySelection(chatId, session.getSelectedBrand());
                } else {
                    sendBrandSelection(chatId);
                }
            } else if (callbackData.equals("restart_quiz")) {
                sendCarQuiz(chatId, session);
            } else if (callbackData.equals("main_menu")) {
                sendMainMenu(chatId, "Главное меню:");
            } else if (callbackData.equals("next_question")) {
                sendCarQuiz(chatId, session);
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки callback: " + e.getMessage());

            sendErrorMessage(chatId);
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🚗 *Добро пожаловать в Car Explorer Bot!*\n\n" +
                "Я помогу вам изучить модели автомобилей BMW и Dodge.\n\n" +
                "🌟 *Возможности:*\n" +
                "• Просмотр моделей по категориям\n" +
                "• Поиск моделей\n" +
                "• Добавление в избранное\n" +
                "• Автомобильная викторина\n" +
                "• Статистика и сравнение\n\n" +
                "👇 *Используйте кнопки ниже для навигации:*");
        message.setParseMode("Markdown");

        sendMainMenuKeyboard(message);
        executeMessage(message);
    }

    private void sendMainMenu(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");

        sendMainMenuKeyboard(message);
        executeMessage(message);
    }

    private void sendMainMenuKeyboard(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🏁 Выбрать марку");
        row1.add("🔍 Поиск моделей");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🎮 Викторина");
        row2.add("⭐️ Избранное");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🔄 Случайная модель");
        row3.add("📊 Статистика");
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("🏆 Топ модели");
        row4.add("📈 Категории");
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        message.setReplyMarkup(keyboardMarkup);
    }

    private void sendBrandSelection(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🏁 *Выберите марку автомобиля:*");
        message.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String brand : MyBotService.getAllBrands()) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();

            String emoji = brand.equals("BMW") ? "🇩🇪" : "🇺🇸";
            String description = brand.equals("BMW") ? "Немецкая премиум" : "Американская мощь";

            button.setText(emoji + " " + brand + " - " + description);
            button.setCallbackData("brand_" + brand);
            row.add(button);
            rows.add(row);
        }

        // Кнопка возврата в главное меню
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🏠 Главное меню");
        backButton.setCallbackData("main_menu");
        backRow.add(backButton);
        rows.add(backRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void sendCategorySelection(Long chatId, String brand) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("✅ *" + brand + "*\n" +
                "📝 " + MyBotService.getCarDescription(brand) + "\n\n" +
                "👇 *Выберите тип автомобиля:*");
        message.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();


        Map<String, List<String>> categories = MyBotService.getCarCategories(brand);
        List<String> categoryList = new ArrayList<>(categories.keySet());

        for (String category : categoryList) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();

            String emoji = getCategoryEmoji(category);
            int modelCount = categories.get(category).size();

            button.setText(emoji + " " + category + " (" + modelCount + ")");
            button.setCallbackData("category_" + category);
            row.add(button);
            rows.add(row);
        }

        // Кнопки навигации
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад к маркам");
        backButton.setCallbackData("back_to_brands");
        navRow.add(backButton);

        InlineKeyboardButton randomButton = new InlineKeyboardButton();
        randomButton.setText("🎲 Случайная категория");
        randomButton.setCallbackData("category_" +
                categoryList.get(random.nextInt(categoryList.size())));
        navRow.add(randomButton);

        rows.add(navRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void sendModelsList(Long chatId, String brand, String category, int page) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        List<String> models = MyBotService.getAvailableModels(brand, category);

        if (models.isEmpty()) {
            message.setText("📋 *" + brand + " - " + category + "*\n\n" +
                    "⚠️ Модели не найдены в этой категории.");
            message.setParseMode("Markdown");
            executeMessage(message);
            return;
        }

        // Пагинация
        int pageSize = 5;
        int totalPages = (int) Math.ceil((double) models.size() / pageSize);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * pageSize;
        int end = Math.min(start + pageSize, models.size());

        StringBuilder responseText = new StringBuilder();
        responseText.append("📋 *").append(brand).append(" - ").append(category).append("*\n\n");

        for (int i = start; i < end; i++) {
            responseText.append((i + 1)).append(". *").append(models.get(i)).append("*\n");
        }

        responseText.append("\n📄 Страница ").append(page + 1).append(" из ").append(totalPages);

        message.setText(responseText.toString());
        message.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки моделей
        for (int i = start; i < end; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            String model = models.get(i);

            InlineKeyboardButton modelButton = new InlineKeyboardButton();
            modelButton.setText("🚙 " + model);
            modelButton.setCallbackData("model_" + model);
            row.add(modelButton);

            InlineKeyboardButton favoriteButton = new InlineKeyboardButton();
            favoriteButton.setText("⭐️");
            favoriteButton.setCallbackData("favorite_" + model);
            row.add(favoriteButton);

            rows.add(row);
        }

        // Кнопки пагинации
        List<InlineKeyboardButton> paginationRow = new ArrayList<>();
        if (page > 0) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton();
            prevButton.setText("◀️ Назад");
            prevButton.setCallbackData("page_" + brand + "_" + category + "_" + (page - 1));
            paginationRow.add(prevButton);
        }


        if (page < totalPages - 1) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("Вперед ▶️");
            nextButton.setCallbackData("page_" + brand + "_" + category + "_" + (page + 1));
            paginationRow.add(nextButton);
        }

        if (!paginationRow.isEmpty()) {
            rows.add(paginationRow);
        }

        // Кнопки навигации
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад к категориям");
        backButton.setCallbackData("back_to_categories");
        navRow.add(backButton);

        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText("🏠 Главное меню");
        menuButton.setCallbackData("main_menu");
        navRow.add(menuButton);

        rows.add(navRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void sendModelDetails(Long chatId, String brand, String model, UserSession session) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        // Генерация детальной информации
        String description = MyBotService.getModelDescription(brand, model);
        boolean isMuscleCar = MyBotService.isMuscleCar(model);

        // Генерация случайных характеристик (для демонстрации)
        int year = 2000 + random.nextInt(25);
        int horsepower = isMuscleCar ? 400 + random.nextInt(400) : 150 + random.nextInt(250);
        int price = isMuscleCar ? 50000 + random.nextInt(100000) : 30000 + random.nextInt(50000);

        String muscleCarEmoji = isMuscleCar ? "🔥 " : "";

        StringBuilder text = new StringBuilder();
        text.append(muscleCarEmoji).append("*").append(model).append("*\n\n");
        text.append("🏭 *Производитель:* ").append(brand).append("\n");
        text.append("📅 *Год выпуска:* ").append(year).append("\n");
        text.append("⚡️ *Мощность:* ").append(horsepower).append(" л.с.\n");
        text.append("💰 *Примерная цена:* $").append(String.format("%,d", price)).append("\n");
        text.append("📝 *Описание:* ").append(description).append("\n\n");

        if (session.getFavorites().contains(model)) {
            text.append("⭐️ *В вашем избранном*\n");
        }

        if (isMuscleCar) {
            text.append("🔥 *Это маслкар!*\n");
        }

        message.setText(text.toString());
        message.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка избранного
        List<InlineKeyboardButton> favoriteRow = new ArrayList<>();
        InlineKeyboardButton favoriteButton = new InlineKeyboardButton();

        if (session.getFavorites().contains(model)) {
            favoriteButton.setText("❌ Удалить из избранного");
        } else {
            favoriteButton.setText("⭐️ Добавить в избранное");
        }
        favoriteButton.setCallbackData("favorite_" + model);
        favoriteRow.add(favoriteButton);
        rows.add(favoriteRow);

        // Кнопка возврата
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад к моделям");

        Optional<String> categoryOpt = MyBotService.getCategoryOfModel(brand, model);
        if (categoryOpt.isPresent()) {
            backButton.setCallbackData("category_" + categoryOpt.get());
        } else {
            backButton.setCallbackData("back_to_categories");
        }

        backRow.add(backButton);
        rows.add(backRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }


    private void sendCarQuiz(Long chatId, UserSession session) {
        List<String> questions = Arrays.asList(
                "Какая модель BMW является самым продаваемым седаном?",
                "Какой Dodge известен как 'Hellcat'?",
                "Какая модель BMW имеет обозначение M3?",
                "Какой Dodge имеет версию 'Demon'?"
        );

        List<List<String>> answers = Arrays.asList(
                Arrays.asList("3 Series", "5 Series", "7 Series", "1 Series"),
                Arrays.asList("Charger", "Challenger", "Durango", "Viper"),
                Arrays.asList("BMW M3", "BMW X3", "BMW Z4", "BMW i8"),
                Arrays.asList("Challenger", "Charger", "Ram", "Durango")
        );

        List<String> correctAnswers = Arrays.asList(
                "3 Series",
                "Charger",
                "BMW M3",
                "Challenger"
        );

        int index = random.nextInt(questions.size());
        session.setCurrentQuizQuestion(questions.get(index));
        session.setCurrentQuizAnswer(correctAnswers.get(index));

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🎮 *Автомобильная викторина!*\n\n" +
                "❓ " + questions.get(index) + "\n\n" +
                "Выберите правильный ответ:");
        message.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<String> answerOptions = answers.get(index);
        for (String answer : answerOptions) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("🚗 " + answer);
            button.setCallbackData("quiz_" + answer);
            row.add(button);
            rows.add(row);
        }

        // Кнопка пропуска
        List<InlineKeyboardButton> skipRow = new ArrayList<>();
        InlineKeyboardButton skipButton = new InlineKeyboardButton();
        skipButton.setText("➡️ Следующий вопрос");
        skipButton.setCallbackData("next_question");
        skipRow.add(skipButton);
        rows.add(skipRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void handleQuizAnswer(Long chatId, Integer messageId, String answer, UserSession session) {
        String correctAnswer = session.getCurrentQuizAnswer();

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);

        if (answer.equals(correctAnswer)) {
            editMessage.setText("✅ *Правильно!*\n\n" +
                    "Вы выбрали правильный ответ: *" + answer + "*\n\n" +
                    "🎉 Поздравляем!");
            session.incrementCorrectAnswers();
        } else {
            editMessage.setText("❌ *Неправильно!*\n\n" +
                    "Ваш ответ: " + answer + "\n" +
                    "Правильный ответ: *" + correctAnswer + "*\n\n" +
                    "Попробуйте еще раз!");
        }

        editMessage.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton newQuizButton = new InlineKeyboardButton();
        newQuizButton.setText("🔄 Новый вопрос");
        newQuizButton.setCallbackData("next_question");
        row.add(newQuizButton);

        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText("🏠 Главное меню");
        menuButton.setCallbackData("main_menu");
        row.add(menuButton);

        rows.add(row);

        markup.setKeyboard(rows);
        editMessage.setReplyMarkup(markup);


        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendErrorMessage(chatId);
        }
    }

    private void sendFavorites(Long chatId, UserSession session) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        Set<String> favorites = session.getFavorites();
        if (favorites.isEmpty()) {
            message.setText("⭐️ *Ваше избранное пусто*\n\n" +
                    "Добавляйте модели в избранное, нажимая на звездочку ⭐️ рядом с моделью.");
        } else {
            StringBuilder text = new StringBuilder("⭐️ *Ваши избранные модели:*\n\n");
            int i = 1;
            for (String model : favorites) {
                Optional<String> brandOpt = MyBotService.getBrandOfModel(model);
                String brandInfo = brandOpt.map(b -> " (" + b + ")").orElse("");
                text.append(i).append(". *").append(model).append("*").append(brandInfo).append("\n");
                i++;
            }
            message.setText(text.toString());
        }

        message.setParseMode("Markdown");

        // Кнопки для управления избранным
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (!favorites.isEmpty()) {
            List<InlineKeyboardButton> clearRow = new ArrayList<>();
            InlineKeyboardButton clearButton = new InlineKeyboardButton();
            clearButton.setText("🗑 Очистить избранное");
            clearButton.setCallbackData("clear_favorites");
            clearRow.add(clearButton);
            rows.add(clearRow);
        }

        List<InlineKeyboardButton> navRow = new ArrayList<>();
        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText("🏠 Главное меню");
        menuButton.setCallbackData("main_menu");
        navRow.add(menuButton);
        rows.add(navRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    private void sendUserStats(Long chatId, UserSession session) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        StringBuilder text = new StringBuilder("📊 *Ваша статистика:*\n\n");
        text.append("🔍 Всего просмотрено марок: ").append(session.getBrandViews().size()).append("\n");
        text.append("✅ Правильных ответов в викторине: ").append(session.getCorrectAnswers()).append("\n");
        text.append("⭐️ Избранных моделей: ").append(session.getFavorites().size()).append("\n\n");

        if (!session.getBrandViews().isEmpty()) {
            text.append("*Популярные марки:*\n");
            session.getBrandViews().entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(3)
                    .forEach(entry ->
                            text.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" раз\n"));
        }

        text.append("\n📈 *Общая статистика бота:*\n");
        Map<String, Integer> modelCounts = MyBotService.getModelCounts();
        modelCounts.forEach((brand, count) ->
                text.append("• ").append(brand).append(": ").append(count).append(" моделей\n"));

        message.setText(text.toString());
        message.setParseMode("Markdown");

        executeMessage(message);
    }

    private void sendSearchPrompt(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🔍 *Поиск моделей*\n\n" +
                "Введите название модели или часть названия для поиска:\n\n" +
                "*Примеры:*\n" +
                "• M3\n" +
                "• Charger\n" +
                "• Series");
        message.setParseMode("Markdown");

        executeMessage(message);
    }


    private void handleSearchQuery(String query, Long chatId, UserSession session) {
        List<String> results = MyBotService.searchModelsPartial(query);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        if (results.isEmpty()) {
            message.setText("🔍 *Результаты поиска для: " + query + "*\n\n" +
                    "⚠️ Модели не найдены.\n" +
                    "Попробуйте другой запрос.");
            session.setMode(UserSession.Mode.NORMAL);
        } else {
            StringBuilder text = new StringBuilder("🔍 *Результаты поиска для: " + query + "*\n\n");

            int limit = Math.min(10, results.size());
            for (int i = 0; i < limit; i++) {
                text.append((i + 1)).append(". *").append(results.get(i)).append("*\n");
            }

            if (results.size() > 10) {
                text.append("\n... и еще ").append(results.size() - 10).append(" моделей");
            }

            message.setText(text.toString());

            // Добавляем кнопки для популярных результатов
            if (!results.isEmpty()) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                // Показываем первые 3 результата как кнопки
                int buttonCount = Math.min(3, results.size());
                for (int i = 0; i < buttonCount; i++) {
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText("🚙 " + results.get(i));

                    Optional<String> brandOpt = MyBotService.getBrandOfModel(results.get(i));
                    if (brandOpt.isPresent()) {
                        button.setCallbackData("model_" + results.get(i));
                    }

                    row.add(button);
                    rows.add(row);
                }

                List<InlineKeyboardButton> navRow = new ArrayList<>();
                InlineKeyboardButton menuButton = new InlineKeyboardButton();
                menuButton.setText("🏠 Главное меню");
                menuButton.setCallbackData("main_menu");
                navRow.add(menuButton);
                rows.add(navRow);

                markup.setKeyboard(rows);
                message.setReplyMarkup(markup);
            }
        }

        message.setParseMode("Markdown");
        session.setMode(UserSession.Mode.NORMAL);
        executeMessage(message);
    }

    private void sendRandomCar(Long chatId) {
        Optional<String> randomModelOpt = MyBotService.getRandomModel();

        if (randomModelOpt.isPresent()) {
            String model = randomModelOpt.get();
            Optional<String> brandOpt = MyBotService.getBrandOfModel(model);

            if (brandOpt.isPresent()) {
                UserSession session = userSessions.computeIfAbsent(chatId, k -> new UserSession());
                sendModelDetails(chatId, brandOpt.get(), model, session);
            } else {
                sendTextMessage(chatId, "🎲 *Случайная модель:*\n\n" + model);
            }
        } else {
            sendTextMessage(chatId, "⚠️ Не удалось выбрать случайную модель. Попробуйте позже.");
        }
    }

    private void sendTopModels(Long chatId) {
        List<String> topModels = MyBotService.getTopModels(10);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        StringBuilder text = new StringBuilder("🏆 *Топ 10 популярных моделей:*\n\n");

        for (int i = 0; i < topModels.size(); i++) {
            String medal = getMedalEmoji(i);
            text.append(medal).append(" *").append(topModels.get(i)).append("*\n");

            Optional<String> brandOpt = MyBotService.getBrandOfModel(topModels.get(i));
            brandOpt.ifPresent(brand -> text.append("   └── ").append(brand).append("\n"));
        }

        message.setText(text.toString());
        message.setParseMode("Markdown");


        executeMessage(message);
    }

    private void sendCategoryStats(Long chatId) {
        Map<String, Long> stats = MyBotService.getCategoryStats();

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        StringBuilder text = new StringBuilder("📈 *Статистика по категориям:*\n\n");

        stats.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry -> {
                    String emoji = getCategoryEmoji(entry.getKey());
                    text.append(emoji).append(" *").append(entry.getKey()).append("*: ")
                            .append(entry.getValue()).append(" моделей\n");
                });

        message.setText(text.toString());
        message.setParseMode("Markdown");

        executeMessage(message);
    }

    private void toggleFavorite(Long chatId, String model, UserSession session) {
        if (session.getFavorites().contains(model)) {
            session.removeFavorite(model);
            sendTextMessage(chatId, "❌ Модель *" + model + "* удалена из избранного");
        } else {
            session.addFavorite(model);
            sendTextMessage(chatId, "✅ Модель *" + model + "* добавлена в избранное!");
        }
    }

    private void sendComparePrompt(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🔄 *Сравнение моделей*\n\n" +
                "Введите две модели для сравнения через запятую:\n\n" +
                "*Пример:*\n" +
                "M3, Charger\n" +
                "5 Series, Durango");
        message.setParseMode("Markdown");

        executeMessage(message);
    }

    private void handleCompareQuery(String query, Long chatId, UserSession session) {
        String[] models = query.split(",");
        if (models.length != 2) {
            sendTextMessage(chatId, "⚠️ Пожалуйста, введите ровно две модели через запятую.");
            return;
        }

        String model1 = models[0].trim();
        String model2 = models[1].trim();

        Optional<String> brand1Opt = MyBotService.getBrandOfModel(model1);
        Optional<String> brand2Opt = MyBotService.getBrandOfModel(model2);

        if (brand1Opt.isEmpty() || brand2Opt.isEmpty()) {
            sendTextMessage(chatId, "⚠️ Одна или обе модели не найдены.");
            session.setMode(UserSession.Mode.NORMAL);
            return;
        }

        // Сравниваем модели
        StringBuilder comparison = new StringBuilder();
        comparison.append("🔄 *Сравнение моделей:*\n\n");

        comparison.append("*").append(model1).append("* vs *").append(model2).append("*\n\n");

        comparison.append("1️⃣ *").append(model1).append("*\n");
        comparison.append("   • Бренд: ").append(brand1Opt.get()).append("\n");
        comparison.append("   • Тип: ").append(MyBotService.getModelDescription(brand1Opt.get(), model1)).append("\n");
        comparison.append("   • Маслкар: ").append(MyBotService.isMuscleCar(model1) ? "Да 🔥" : "Нет").append("\n\n");

        comparison.append("2️⃣ *").append(model2).append("*\n");
        comparison.append("   • Бренд: ").append(brand2Opt.get()).append("\n");
        comparison.append("   • Тип: ").append(MyBotService.getModelDescription(brand2Opt.get(), model2)).append("\n");
        comparison.append("   • Маслкар: ").append(MyBotService.isMuscleCar(model2) ? "Да 🔥" : "Нет").append("\n\n");

        // Простое сравнение
        boolean bothMuscleCars = MyBotService.isMuscleCar(model1) && MyBotService.isMuscleCar(model2);
        boolean sameBrand = brand1Opt.get().equals(brand2Opt.get());

        if (bothMuscleCars) {
            comparison.append("⚡️ *Обе модели являются маслкарами!*\n");
        } else if (MyBotService.isMuscleCar(model1)) {
            comparison.append("⚡️ *" + model1 + " является маслкаром*\n");
        } else if (MyBotService.isMuscleCar(model2)) {
            comparison.append("⚡️ *" + model2 + " является маслкаром*\n");
        }

        if (sameBrand) {
            comparison.append("🏭 *Обе модели одного бренда*\n");
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(comparison.toString());
        message.setParseMode("Markdown");

        session.setMode(UserSession.Mode.NORMAL);
        executeMessage(message);
    }

    private void sendHelpMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("❓ *Помощь по использованию бота*\n\n" +
                "*Основные команды:*\n" +
                "🏁 `/start` - Начать работу с ботом\n" +
                "🔍 `/search` - Поиск моделей по названию\n" +
                "🔄 `/compare` - Сравнить две модели\n" +
                "🎲 `/random` - Показать случайную модель\n" +
                "🎮 `/quiz` - Начать викторину\n" +
                "⭐️ `/favorites` - Показать избранное\n" +
                "📊 `/stats` - Ваша статистика\n" +
                "🚗 `/brands` - Выбрать марку\n\n" +

                "*Основные возможности:*\n" +
                "• Просмотр моделей BMW и Dodge по категориям\n" +
                "• Добавление моделей в избранное\n" +
                "• Автомобильная викторина\n" +
                "• Статистика просмотров\n" +
                "• Поиск моделей\n" +
                "• Сравнение моделей\n\n" +

                "*Как использовать:*\n" +
                "1. Начните с команды `/start`\n" +
                "2. Используйте кнопки для навигации\n" +
                "3. Нажимайте ⭐️ чтобы добавить в избранное\n" +
                "4. Попробуйте викторину для проверки знаний\n\n" +

                "*Советы:*\n" +
                "• Для быстрого поиска используйте команду `/search`\n" +
                "• Добавляйте понравившиеся модели в избранное\n" +
                "• Проверьте свою статистику командой `/stats`");
        message.setParseMode("Markdown");

        executeMessage(message);
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        executeMessage(message);
    }

    private void sendErrorMessage(Long chatId) {
        sendTextMessage(chatId, "⚠️ Произошла ошибка. Пожалуйста, попробуйте еще раз или используйте /start");
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "@islombekcarcollection_bot";
    }

    @Override
    public String getBotToken() {
        return "8433457326:AAE16QSmgNeAWni0X60mqtMxALkGXSxHyy4";
    }

    // Вспомогательные методы
    private String getCategoryEmoji(String category) {
        switch (category) {
            case "седан": return "🚙";
            case "внедорожник": return "🚙";
            case "купе": return "🏎";
            case "пикап": return "🚚";
            case "маслкар": return "🔥";
            default: return "🚗";
        }
    }

    private String getMedalEmoji(int position) {
        switch (position) {
            case 0: return "🥇";
            case 1: return "🥈";
            case 2: return "🥉";
            default: return "🔸";
        }
    }

    // Класс для хранения сессии пользователя
    private static class UserSession {
        enum Mode {
            NORMAL, SEARCH, COMPARE
        }

        private Mode mode = Mode.NORMAL;
        private String selectedBrand;
        private String selectedCategory;
        private final Set<String> favorites = new HashSet<>();
        private final Map<String, Integer> brandViews = new HashMap<>();
        private int correctAnswers = 0;
        private String currentQuizQuestion;
        private String currentQuizAnswer;


        public void reset() {
            mode = Mode.NORMAL;
            selectedBrand = null;
            selectedCategory = null;
        }

        public void addFavorite(String model) {
            favorites.add(model);
        }

        public void removeFavorite(String model) {
            favorites.remove(model);
        }

        public Set<String> getFavorites() {
            return new HashSet<>(favorites);
        }

        public void incrementBrandViews(String brand) {
            brandViews.put(brand, brandViews.getOrDefault(brand, 0) + 1);
        }

        public Map<String, Integer> getBrandViews() {
            return new HashMap<>(brandViews);
        }

        public void incrementCorrectAnswers() {
            correctAnswers++;
        }

        public int getCorrectAnswers() {
            return correctAnswers;
        }

        // Геттеры и сеттеры
        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode; }

        public String getSelectedBrand() { return selectedBrand; }
        public void setSelectedBrand(String brand) { this.selectedBrand = brand; }

        public String getSelectedCategory() { return selectedCategory; }
        public void setSelectedCategory(String category) { this.selectedCategory = category; }

        public String getCurrentQuizQuestion() { return currentQuizQuestion; }
        public void setCurrentQuizQuestion(String question) { this.currentQuizQuestion = question; }

        public String getCurrentQuizAnswer() { return currentQuizAnswer; }
        public void setCurrentQuizAnswer(String answer) { this.currentQuizAnswer = answer; }
    }
}
