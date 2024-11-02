package org.example.app;

import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;

public class Menu {
    private final Database database;
    private final Scanner scanner;

    public Menu(Database database, Scanner scanner) {
        this.database = database;
        this.scanner = scanner;
    }

    public void displayMenu() {
        while (true) {
            try {
                System.out.println("Меню:");
                System.out.println("1. Добавить новое животное");
                System.out.println("2. Показать список всех животных");
                System.out.println("3. Просмотреть список команд животного");
                System.out.println("4. Обучить животное новой команде");
                System.out.println("0. Выход");
                System.out.print("Выберите пункт меню: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1 -> addAnimalPrompt();
                    case 2 -> database.displayAllAnimals();
                    case 3 -> displayAnimalCommandsPrompt();
                    case 4 -> teachNewCommandPrompt();
                    case 0 -> {
                        System.out.println("Программа завершена.");
                        return;
                    }
                    default -> System.out.println("Неверный выбор. Попробуйте снова.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Ошибка: неверный формат ввода. Попробуйте снова.");
                scanner.nextLine();
            } catch (SQLException e) {
                System.out.println("Ошибка работы с базой данных: " + e.getMessage());
            }
        }
    }

    void addAnimalPrompt() throws SQLException {
        System.out.println("Введите информацию о животном.");

        System.out.print("Введите тип животного (собака, кот, хомяк, лошадь, верблюд, осел): ");
        String animalType = scanner.nextLine().trim().toLowerCase();


        if (animalType == null) {
            System.out.println("Неизвестный тип животного: " + animalType);
            return;
        }

        System.out.print("Введите имя животного: ");
        String name = scanner.nextLine();

        System.out.print("Введите навыки животного (через запятую): ");
        String skillsInput = scanner.nextLine();
        List<String> skills = Arrays.asList(skillsInput.split("\\s*,\\s*")); // Разделяем по запятой и убираем пробелы

        System.out.print("Введите дату рождения животного (в формате YYYY-MM-DD): ");
        String birthDateStr = scanner.nextLine();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date birthDate;

        try {
            birthDate = dateFormat.parse(birthDateStr);
        } catch (ParseException e) {
            System.out.println("Ошибка: неверный формат даты. Пожалуйста, используйте формат YYYY-MM-DD.");
            return;
        }

        Animal animal = switch (animalType) {
            case "собака" -> new Dog(name, skills, birthDate);
            case "кот" -> new Cat(name, skills, birthDate);
            case "хомяк" -> new Hamster(name, skills, birthDate);
            case "лошадь" -> new Horse(name, skills, birthDate);
            case "верблюд" -> new Camel(name, skills, birthDate);
            case "осел" -> new Donkey(name, skills, birthDate);
            default -> null;
        };

        if (animal != null) {
            database.addNewAnimal(animal);
        } else {
            System.out.println("Не удалось создать объект животного.");
        }
    }

    void displayAnimalCommandsPrompt() {
        System.out.print("Введите тип животного (лошадь, кот, собака, верблюд, хомяк, осел): ");
        String animalType = scanner.nextLine();

        if (animalType == null) {
            System.out.println("Неизвестный тип животного: " + animalType);
            return;
        }

        System.out.print("Введите имя животного: ");
        String animalName = scanner.nextLine();

        try {
            database.displayAnimalCommands(animalType, animalName);
        } catch (SQLException e) {
            System.out.println("Ошибка при получении команд: " + e.getMessage());
        }
    }

    void teachNewCommandPrompt() {
        System.out.print("Введите тип животного (собака, кот, хомяк, лошадь, верблюд, осел): ");
        String animalType = scanner.nextLine();

        if (animalType == null) {
            System.out.println("Неизвестный тип животного: " + animalType);
            return;
        }

        System.out.print("Введите имя животного: ");
        String animalName = scanner.nextLine();

        System.out.print("Введите команду, которую хотите добавить для " + animalName + ": ");
        String newCommand = scanner.nextLine();

        try {
            database.addCommandForAnimal(animalType, animalName, newCommand);
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении команды: " + e.getMessage());
        }
    }
}

