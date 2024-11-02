package org.example.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    private Database database;

    @BeforeEach
    void setUp() throws SQLException {
        database = new Database();
        database.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (database != null) {
            database.closeConnection();
        }
    }

    // ДОБАВЛЕНИЕ ЖИВОТНЫХ
    @Test
    void testAddNewAnimal() throws SQLException, ParseException {
        Date birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-01");
        List<String> skills = new ArrayList<>(Arrays.asList("Sit", "Stay"));
        Animal dog = new Dog("Buddy", skills, birthDate);

        database.addNewAnimal(dog);

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM dogs WHERE name = 'Buddy'")) {

            assertTrue(resultSet.next(), "Животное должно быть добавлено в таблицу.");
            assertEquals("Buddy", resultSet.getString("name"));

            Date dbBirthDate = resultSet.getDate("birth_date");
            assertEquals(birthDate, dbBirthDate, "Дата рождения должна совпадать с заданной");

            // Проверяем навыки в отдельной таблице
            try (ResultSet skillsResultSet = statement.executeQuery("SELECT skill FROM dogs_skills WHERE animal_id = " + resultSet.getInt("id"))) {
                List<String> retrievedSkills = new ArrayList<>();
                while (skillsResultSet.next()) {
                    retrievedSkills.add(skillsResultSet.getString("skill"));
                }
                assertTrue(retrievedSkills.contains("Sit"), "Навыки должны включать 'Sit'.");
                assertTrue(retrievedSkills.contains("Stay"), "Навыки должны включать 'Stay'.");
            }
        } catch (Exception e) {
            fail("Ошибка при выполнении теста: " + e.getMessage());
        }
    }

    @Test
    void testAddDuplicateAnimal() throws SQLException, ParseException {
        Date birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-01");
        List<String> skills = new ArrayList<>(Arrays.asList("Sit", "Stay"));
        Animal dog = new Dog("Buddy", skills, birthDate);

        database.addNewAnimal(dog);

        SQLException thrown = assertThrows(SQLException.class, () -> {
            database.addNewAnimal(dog);
        });

        assertEquals("Duplicate entry 'Buddy' for key 'dogs.name'", thrown.getMessage());
    }

    // ДОБАВЛЕНИЕ КОМАНД
    @Test
    void testAddCommandForAnimal() throws SQLException, ParseException {
        Date birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-01");
        Animal dog = new Dog("Buddy", Arrays.asList("Sit", "Stay"), birthDate);
        database.addNewAnimal(dog);

        String newCommand = "Roll over";
        database.addCommandForAnimal("собака", "Buddy", newCommand);

        try (Connection connection = database.getConnection()) {
            int animalId;

            try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM dogs WHERE name = ?")) {
                statement.setString(1, "Buddy");
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    animalId = resultSet.getInt("id");
                }
            }

            try (PreparedStatement skillStatement = connection.prepareStatement("SELECT skill FROM dogs_skills WHERE animal_id = ?")) {
                skillStatement.setInt(1, animalId);
                try (ResultSet skillResultSet = skillStatement.executeQuery()) {
                    List<String> retrievedSkills = new ArrayList<>();
                    while (skillResultSet.next()) {
                        retrievedSkills.add(skillResultSet.getString("skill"));
                    }
                    assertTrue(retrievedSkills.contains("Roll over"), "Навыки должны включать 'Roll over'.");
                }
            }
        } catch (Exception e) {
            fail("Ошибка при выполнении теста: " + e.getMessage());
        }
    }

    // ОТОБРАЖЕНИЕ ИНФОРМАЦИИ
    @Test
    void testDisplayAllAnimals() throws SQLException, ParseException {
        Date birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-01");
        Animal dog = new Dog("Buddy", Arrays.asList("Sit", "Stay"), birthDate);
        Animal cat = new Cat("Whiskers", Arrays.asList("Meow", "Jump"), birthDate);

        database.addNewAnimal(dog);
        database.addNewAnimal(cat);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM dogs UNION SELECT name FROM cats")) {

            assertTrue(resultSet.next(), "Должно быть хотя бы одно животное в таблице.");
            assertEquals("Buddy", resultSet.getString("name"));

            assertTrue(resultSet.next(), "Должно быть два животных в таблице.");
            assertEquals("Whiskers", resultSet.getString("name"));

            database.displayAllAnimals();

            String output = outputStream.toString();

            assertTrue(output.contains("dogs: Buddy"), "Отсутствует запись о собаке Buddy.");
            assertTrue(output.contains("cats: Whiskers"), "Отсутствует запись о кошке Whiskers.");

            assertTrue(output.contains("Общее количество животных: 2"), "Некорректное общее количество животных.");

        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testDisplayAllAnimalsWhenNoAnimalsInDatabase() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        try {
            database.displayAllAnimals();
            String output = outputStream.toString();
            assertTrue(output.contains("В базе данных нет животных."), "Ожидается сообщение об отсутствии животных в базе данных.");
        } catch (SQLException e) {
            fail("Ошибка при выполнении теста: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
        }
    }


    @Test
    void testDisplayAnimalCommands() throws SQLException, ParseException {
        Date birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-01");
        List<String> skills = new ArrayList<>(Arrays.asList("Sit", "Stay"));
        Animal dog = new Dog("Buddy", skills, birthDate);

        database.addNewAnimal(dog);

        String skillsTableName = "dogs_skills";

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT skill FROM " + skillsTableName +
                     " WHERE animal_id = (SELECT id FROM dogs WHERE name = 'Buddy')")) {

            List<String> retrievedSkills = new ArrayList<>();
            while (resultSet.next()) {
                retrievedSkills.add(resultSet.getString("skill"));
            }

            assertTrue(retrievedSkills.contains("Sit"), "Навыки должны включать 'Sit'.");
            assertTrue(retrievedSkills.contains("Stay"), "Навыки должны включать 'Stay'.");
        } catch (Exception e) {
            fail("Ошибка при выполнении теста: " + e.getMessage());
        }
    }

    @Test
    void testDisplayCommandsForNonExistentAnimal() throws SQLException {
        String nonExistentAnimalName = "NonExistentDog";
        String animalType = "собака";

        try {
            database.displayAnimalCommands(animalType, nonExistentAnimalName);
        } catch (SQLException e) {
            assertEquals("Животное с именем NonExistentDog не найдено.", e.getMessage());
        }
    }
}
