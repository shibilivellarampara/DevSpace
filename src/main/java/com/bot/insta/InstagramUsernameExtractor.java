package com.bot.insta;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public class InstagramUsernameExtractor {

    /**
     * Extract Instagram usernames from the JSON file.
     */
    public static List<String> extractUsernames(String jsonPath) throws Exception {
        System.out.println("📥 Reading JSON file from: " + jsonPath);

        JSONParser parser = new JSONParser();
        Object parsed = parser.parse(new FileReader(jsonPath));

        List<String> usernames = new ArrayList<>();

        if (parsed instanceof JSONArray) {
            System.out.println("🔍 Detected JSON array format (likely followers.json)");
            JSONArray rootArray = (JSONArray) parsed;
            for (Object item : rootArray) {
                if (item instanceof JSONObject) {
                    extractUsernameFromUserObject((JSONObject) item, usernames);
                }
            }
        } else if (parsed instanceof JSONObject) {
            JSONObject rootObj = (JSONObject) parsed;

            JSONArray followersArray = (JSONArray) rootObj.get("relationships_followers");
            JSONArray followingArray = (JSONArray) rootObj.get("relationships_following");

            if (followersArray != null) {
                System.out.println("🔍 Detected 'relationships_followers' key.");
                for (Object item : followersArray) {
                    if (item instanceof JSONObject) {
                        extractUsernameFromUserObject((JSONObject) item, usernames);
                    }
                }
            }

            if (followingArray != null) {
                System.out.println("🔍 Detected 'relationships_following' key.");
                for (Object item : followingArray) {
                    if (item instanceof JSONObject) {
                        extractUsernameFromUserObject((JSONObject) item, usernames);
                    }
                }
            }

            if (followersArray == null && followingArray == null) {
                System.err.println("⚠️ Unsupported File. Please upload only 'followers.json' or 'following.json' exported from Instagram.");
                throw new Exception("Unsupported File. Only 'followers' or 'following' are supported.");
            }
        } else {
            System.err.println("⚠️ Unsupported file structure. Only 'followers' or 'following' are supported.");
            throw new Exception("Unsupported root structure in JSON.");
        }

        System.out.println("✅ Extracted " + usernames.size() + " usernames.");
        return usernames;
    }

    /**
     * Extract usernames safely from each user object.
     */
    private static void extractUsernameFromUserObject(JSONObject userObj, List<String> usernames) {
        JSONArray stringListData = (JSONArray) userObj.get("string_list_data");

        if (stringListData != null && !stringListData.isEmpty()) {
            Object firstData = stringListData.get(0);
            if (firstData instanceof JSONObject) {
                JSONObject data = (JSONObject) firstData;
                Object rawValue = data.get("value");
                if (rawValue != null) {
                    String username = rawValue.toString().trim();
                    if (!username.isEmpty()) {
                        usernames.add(username);
                       // System.out.println("   ➕ Found username: " + username);
                    }
                }
            }
        }
    }

    /**
     * Create a styled and grouped PDF listing usernames with clickable links.
     */
    public static void createStyledPdf(List<String> usernames, String outputPath) throws Exception {
        System.out.println("📄 Starting PDF generation to: " + outputPath);

        // Normalize and sort usernames
        usernames.sort(Comparator.comparing(s -> s.replaceAll("[_.]", "").toLowerCase()));

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Font groupFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
        Font linkFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.UNDERLINE, BaseColor.BLUE);

        document.add(new Paragraph("📃 Instagram Username Report", titleFont));
        document.add(Chunk.NEWLINE);

        char currentGroup = 0;
        int serial = 1;

        for (String username : usernames) {
            String normalized = username.replaceAll("[_.]", "").toLowerCase();
            if (normalized.isEmpty()) continue;

            char firstChar = Character.toUpperCase(normalized.charAt(0));

            if (firstChar != currentGroup) {
                currentGroup = firstChar;
                document.add(new Paragraph("\n" + currentGroup, groupFont));
                document.add(Chunk.NEWLINE);
            }

            Paragraph entry = new Paragraph();
            entry.add(new Chunk(serial + ". ", FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK)));
            Anchor link = new Anchor(username, linkFont);
            link.setReference("https://instagram.com/" + username);
            entry.add(link);

            document.add(entry);
            serial++;
        }

        document.close();
        System.out.println("✅ PDF generation complete. " + serial + " usernames added.");
    }
}
