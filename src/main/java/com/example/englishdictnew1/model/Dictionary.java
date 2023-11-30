package com.example.englishdictnew1.model;

import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Component
public class Dictionary {
    private final String[] keys;
    private final String[] values;
    private final int capacity;

    public Dictionary() {
        capacity = 100000;
        keys = new String[capacity];
        values = new String[capacity];
        loadWordsFromDatabase();
    }

    private void loadWordsFromDatabase() {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/english_dictionary", "root", "");
             Statement stmt = connection.createStatement()) {

            String query = "SELECT Word, Meaning FROM word_meaning_examples";
            ResultSet resultSet = stmt.executeQuery(query);

            while (resultSet.next()) {
                String word = resultSet.getString("Word").toLowerCase();
                String meaning = resultSet.getString("Meaning");

                if (meaning != null) {
                    insert(word, meaning);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insert(String word, String meaning) {
        int index = hashFunction(word);

        if (keys[index] == null) {
            keys[index] = word;
            values[index] = meaning;
        } else {
            index = resolveCollision(index);
            keys[index] = word;
            values[index] = meaning;
        }
    }

    private int hashFunction(String word) {
        return Math.abs(word.hashCode() % capacity);
    }

    private int resolveCollision(int index) {
        int count = 1;
        int newIndex = (index + count * count) % capacity;

        while (keys[newIndex] != null) {
            count++;
            newIndex = (index + count * count) % capacity;
        }

        return newIndex;
    }

    public String getMeaning(String word) {
        word = word.toLowerCase();
        int index = findIndex(word);

        if (index != -1) {
            return values[index];
        } else {
            List<String> similarWords = findSimilarWords(word);
            if (!similarWords.isEmpty()) {
                StringBuilder suggestion = new StringBuilder("Did you mean any of these? ");
                for (String similarWord : similarWords) {
                    suggestion.append("'").append(similarWord).append("' ");
                }
                return suggestion.toString();
            }
            return "Word not found.";
        }
    }

    private int findIndex(String word) {
        int index = hashFunction(word);
        int count = 1;
        int newIndex = index;

        while (keys[newIndex] != null && !keys[newIndex].equals(word)) {
            newIndex = (index + count * count) % capacity;
            if (keys[newIndex] != null && keys[newIndex].equals(word)) {
                return newIndex;
            }
            count++;
        }

        return keys[newIndex] != null ? newIndex : -1;
    }

    private List<String> findSimilarWords(String word) {
        List<String> similarWords = new ArrayList<>();
        int maxSuggestions = 4;
        final int[] suggestionsCount = {0};
        Map<String, Integer> wordDistances = new HashMap<>();

        for (int i = 0; i < capacity; i++) {
            if (keys[i] != null) {
                int distance = calculateLevenshteinDistance(word, keys[i]);
                wordDistances.put(keys[i], distance);
            }
        }

        wordDistances.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(maxSuggestions)
                .forEach(entry -> {
                    if (suggestionsCount[0] < maxSuggestions && entry.getValue() <= 2) {
                        similarWords.add(entry.getKey());
                        suggestionsCount[0]++;
                    }
                });

        return similarWords;
    }


    private int calculateLevenshteinDistance(String word1, String word2) {
        int[][] distance = new int[word1.length() + 1][word2.length() + 1];

        for (int i = 0; i <= word1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= word2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= word1.length(); i++) {
            for (int j = 1; j <= word2.length(); j++) {
                int cost = (word1.charAt(i - 1) == word2.charAt(j - 1)) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost
                );
            }
        }

        return distance[word1.length()][word2.length()];
    }
}
