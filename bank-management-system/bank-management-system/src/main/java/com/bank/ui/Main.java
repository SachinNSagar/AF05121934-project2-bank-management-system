package com.bank.ui;

/**
 * Application entry point for the Bank Management System.
 *
 * Run with:
 *   javac -d out -cp lib/mysql-connector-j.jar $(find src/main/java -name "*.java")
 *   cp -r src/main/resources/* out/
 *   java  -cp out:lib/mysql-connector-j.jar com.bank.ui.Main
 */
public class Main {
    public static void main(String[] args) {
        new ConsoleUI().start();
    }
}
