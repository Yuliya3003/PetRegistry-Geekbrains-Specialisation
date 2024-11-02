package org.example.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuTest {
    private Database mockDatabase;
    private Menu menu;

    @BeforeEach
    void setUp() {
        mockDatabase = Mockito.mock(Database.class);
    }

    @Test
    void testAddAnimalPromptWithValidInput() throws SQLException {
        String input = "собака\nБарсик\nсидеть, лежать\n2023-01-01\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        menu = new Menu(mockDatabase, new Scanner(in));

        menu.addAnimalPrompt();

        ArgumentCaptor<Animal> animalCaptor = ArgumentCaptor.forClass(Animal.class);
        verify(mockDatabase, times(1)).addNewAnimal(animalCaptor.capture());

        Animal animal = animalCaptor.getValue();
        assertEquals("Барсик", animal.getName());
        assertEquals(List.of("сидеть", "лежать"), animal.getSkills());
        assertEquals(parseDate("2023-01-01"), animal.getBirthDate());
    }

    @Test
    void testAddAnimalPromptWithInvalidAnimalType() throws SQLException {
        String input = "птица\nБарсик\nсидеть, лежать\n2023-01-01\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        menu = new Menu(mockDatabase, new Scanner(in));

        menu.addAnimalPrompt();

        verify(mockDatabase, never()).addNewAnimal(any());
    }

    @Test
    void testAddAnimalPromptWithInvalidDate() throws SQLException {
        String input = "собака\nБарсик\nсидеть, лежать\nневерная-дата\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        menu = new Menu(mockDatabase, new Scanner(in));

        menu.addAnimalPrompt();

        verify(mockDatabase, never()).addNewAnimal(any());
    }

    @Test
    void testDisplayAnimalCommandsPromptWithValidInput() throws SQLException {
        String input = "собака\nБарсик\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        menu = new Menu(mockDatabase, new Scanner(in));

        menu.displayAnimalCommandsPrompt();

        verify(mockDatabase, times(1)).displayAnimalCommands("собака", "Барсик");
    }

    @Test
    void testTeachNewCommandPromptWithValidInput() throws SQLException {
        String input = "собака\nБарсик\nголос\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        menu = new Menu(mockDatabase, new Scanner(in));

        menu.teachNewCommandPrompt();

        verify(mockDatabase, times(1)).addCommandForAnimal("собака", "Барсик", "голос");
    }

    private Date parseDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            fail("Невозможно разобрать дату: " + date);
            return null;
        }
    }
}
