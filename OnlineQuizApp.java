package com.example;

import java.sql.*;
import java.util.Scanner;

public class OnlineQuizApp {

    static class QuizQuestion {
        String question, options[];
        int correctAnswer;

        QuizQuestion(String question, String[] options, int correctAnswer) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }
    }

    static class Quiz {
        QuizQuestion[] questions;

        Quiz(QuizQuestion[] questions) {
            this.questions = questions;
        }

        void startQuiz(String username) {
            Scanner scanner = new Scanner(System.in);
            int score = 0;

            for (int i = 0; i < questions.length; i++) {
                QuizQuestion q = questions[i];
                System.out.println("Question " + (i + 1) + ": " + q.question);
                for (int j = 0; j < q.options.length; j++)
                    System.out.println((j + 1) + ". " + q.options[j]);
                System.out.print("Your answer: ");
                if (scanner.nextInt() == q.correctAnswer) score++;
            }

            System.out.println("Quiz ended. Your score: " + score + "/" + questions.length);
            saveScore(username, score);
        }

        void saveScore(String username, int score) {
            try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/quizdb", "postgres", "187");
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO scores (username, score) VALUES (?, ?)")) {
                pstmt.setString(1, username);
                pstmt.setInt(2, score);
                pstmt.executeUpdate();
                System.out.println("Score saved to the database.");
            } catch (SQLException e) {
                System.err.println("Error saving score: " + e.getMessage());
            }
        }
    }

    static void createTables() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/quizdb", "postgres", "187");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, password VARCHAR(50) NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS scores (id SERIAL PRIMARY KEY, username VARCHAR(50) NOT NULL, score INT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            System.out.println("Tables 'users' and 'scores' created or already exist.");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    static boolean registerUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a username: ");
        String username = scanner.nextLine();
        System.out.print("Enter a password: ");
        String password = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/quizdb", "postgres", "187");
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("Registration successful. You can now log in.");
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    static String loginUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/quizdb", "postgres", "187");
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Login successful. Welcome, " + username + "!");
                return username;
            } else {
                System.out.println("Invalid username or password.");
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Error logging in: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        createTables();
        Scanner scanner = new Scanner(System.in);
        String username = null;

        while (true) {
            System.out.println("1. Register\n2. Login\n3. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    registerUser();
                    break;
                case 2:
                    username = loginUser();
                    if (username != null) {
                        Quiz quiz = new Quiz(new QuizQuestion[]{
                            new QuizQuestion("What is the extension of a Java source file?", new String[]{".java", ".class", ".exe", ".jar"}, 1),
                            new QuizQuestion("Which keyword is used to create an object in Java?", new String[]{"new", "create", "object", "instance"}, 1),
                            new QuizQuestion("What is the entry point of a Java program?", new String[]{"main()", "start()", "run()", "init()"}, 1)
                        });
                        quiz.startQuiz(username);
                    }
                    break;
                case 3:
                    System.out.println("Exiting the application. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}