package com.bot.insta;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.util.*;

public class InstagramBot extends TelegramLongPollingBot {

    private final String botToken;

    public InstagramBot(String botToken) {
        this.botToken = botToken;
        System.out.println("✅ InstagramBot initialized with token.");
    }

    @Override
    public String getBotUsername() {
        return "NonFollowersFinderBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private final Map<Long, File> followersMap = new HashMap<>();
    private final Map<Long, File> followingMap = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            Message message = update.getMessage();
            Document document = message.getDocument();
            String fileName = document.getFileName().toLowerCase();
            Long userId = message.getChatId();

            System.out.println("📩 Received file from userId " + userId + ": " + fileName);

            try {
                File downloaded = downloadFile(document);
                System.out.println("📥 File downloaded to: " + downloaded.getAbsolutePath());

                if (fileName.contains("followers")) {
                    followersMap.put(userId, downloaded);
                    sendText(userId, "✅ Followers file received.");
                    System.out.println("📌 Stored followers file for userId " + userId);
                } else if (fileName.contains("following")) {
                    followingMap.put(userId, downloaded);
                    sendText(userId, "✅ Following file received.");
                    System.out.println("📌 Stored following file for userId " + userId);
                }

                if (followersMap.containsKey(userId) && followingMap.containsKey(userId)) {
                    System.out.println("Both files received. Starting PDF generation for userId ");
                    sendText(userId, "🚀 Both files received. Starting Non-Followers report generation...");
                    generateAndSendPdf(userId);
                    System.out.println("📤 PDF sent to userId " + userId);

                    // Clear maps after processing
                    followersMap.remove(userId);
                    followingMap.remove(userId);
                    System.out.println("🧹 Cleared stored files for userId " + userId);
                }

            } catch (Exception e) {
                sendText(userId, "⚠️ Error: " + e.getMessage());
                System.err.println("❌ Error handling file for userId " + userId);
                e.printStackTrace();
            }
        }
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
            System.out.println("💬 Sent message to userId " + chatId + ": " + text);
        } catch (TelegramApiException e) {
            System.err.println("❌ Failed to send message to userId " + chatId);
            e.printStackTrace();
        }
    }

    private File downloadFile(Document document) throws Exception {
        GetFile getFileRequest = new GetFile(document.getFileId());
        org.telegram.telegrambots.meta.api.objects.File file = execute(getFileRequest);

        File downloaded = File.createTempFile("insta_", ".json");
        System.out.println("🔽 Starting download from Telegram servers...");

        try (
                BufferedInputStream in = new BufferedInputStream(
                        new URL(file.getFileUrl(getBotToken())).openStream());
                FileOutputStream out = new FileOutputStream(downloaded)
        ) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        System.out.println("✅ File download complete: " + downloaded.getAbsolutePath());
        return downloaded;
    }

    private void generateAndSendPdf(Long chatId) throws Exception {
        File followersFile = followersMap.get(chatId);
        File followingFile = followingMap.get(chatId);

        System.out.println("🔍 Extracting followers for userId " + chatId);
        List<String> followers = InstagramUsernameExtractor.extractUsernames(followersFile.getPath());

        System.out.println("🔍 Extracting following for userId " + chatId);
        List<String> following = InstagramUsernameExtractor.extractUsernames(followingFile.getPath());

        Set<String> nonFollowers = new HashSet<>(following);
        nonFollowers.removeAll(followers);
        System.out.println("📊 Non-followers calculated: " + nonFollowers.size());

        // 🔄 Standardized file name
        String fileName = "NonFollowersReport"+ ".pdf";
        File pdf = new File(System.getProperty("java.io.tmpdir"), fileName);

        InstagramUsernameExtractor.createStyledPdf(new ArrayList<>(nonFollowers), pdf.getAbsolutePath());
        System.out.println("📄 PDF generated: " + pdf.getAbsolutePath());

        SendDocument sendDoc = new SendDocument();
        sendDoc.setChatId(chatId.toString());
        sendDoc.setDocument(new InputFile(pdf));
        sendDoc.setCaption("Non-Followers Report");
        execute(sendDoc);

        String summaryMessage = String.format(
                "\uD83D\uDCCA *Instagram Report Summary*\n\n" +
                        "\uD83D\uDC65 *Total Followers:* %d\n" +
                        "\u2795 *Total Following:* %d\n" +
                        "\u274C *Total Non-Followers:* %d\n\n" +
                        "\uD83D\uDCC4 Your non-follower report has been generated and sent as a PDF.",
                followers.size(),
                following.size(),
                nonFollowers.size()
        );

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(summaryMessage);
        message.setParseMode("Markdown");

        try {
            execute(message);
            System.out.println("📬 Summary message sent to userId " + chatId);
        } catch (TelegramApiException e) {
            System.err.println("❌ Failed to send summary message to userId " + chatId);
            e.printStackTrace();
        }
    }


}
