package org.example;

import org.example.app.Database;
import org.example.app.Menu;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Database database = new Database();
        Scanner scanner = new Scanner(System.in);
        Menu menu = new Menu(database, scanner);

        menu.displayMenu();
    }
}
