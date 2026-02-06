package com.example.telegrambot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyBot());
            System.out.println("✅ Бот успешно запущен!");
            System.out.println("🤖 Бот готов к работе!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("❌ Ошибка запуска бота: " + e.getMessage());
        }
    }
}


