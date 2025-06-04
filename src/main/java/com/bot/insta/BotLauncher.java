package com.bot.insta;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class BotLauncher {
    public static void main(String[] args) {
        System.out.println("App started!");
        try {
            String botToken = System.getenv("BOT_TOKEN"); // ✅ Environment variable
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new InstagramBot(botToken));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
