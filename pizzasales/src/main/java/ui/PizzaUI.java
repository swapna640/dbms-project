package ui;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import model.QueryInfo;
import service.PizzaService;

public class PizzaUI {

    private static final Scanner sc = new Scanner(System.in);

    public static void menu() {
        while (true) {
            System.out.println("\n=== Pizza Sales Analytics ===");
            System.out.println("1. Create tables");
            System.out.println("2. Drop tables");
            System.out.println("3. Run a query");
            System.out.println("0. Exit");
            System.out.println("Enter your choice: ");
            String choice = sc.next();

            switch (choice) {
                case "1" -> createTables();
                case "2" -> dropTables();
                case "3" -> runQuery();
                case "0" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice - please try again");
            }
        }
    }

    public static void createTables() {
        try {
            PizzaService.createTables();
            System.out.println("Tables created successfully");
        } catch (Exception e) {
            System.out.println("Could not create tables: " + e.getMessage());
        }
    }

    public static void dropTables() {
        System.out.println("This will delete all data. Type Y to confirm: ");
        char c = sc.next().charAt(0);
        if (c != 'Y' && c != 'y') {
            System.out.println("Cancelled");
            return;
        }
        try {
            PizzaService.dropTables();
            System.out.println("Tables dropped");
        } catch (Exception e) {
            System.out.println("Could not drop tables: " + e.getMessage());
        }
    }

    public static void runQuery() {
        System.out.println("\nAvailable queries:");
        for (QueryInfo q : PizzaService.getQueries()) {
            System.out.println(q.getId() + ". " + q.getTitle());
        }
        System.out.println("Please enter the query number: ");
        String input = sc.next();
        try {
            int id = Integer.parseInt(input);
            print(PizzaService.run(id));
        } catch (IllegalArgumentException e) {
            System.out.println("Perhaps your query number was invalid - please try again");
        } catch (Exception e) {
            System.out.println("Query failed: " + e.getMessage());
        }
    }

    private static void print(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            System.out.println("(no rows)");
            return;
        }
        System.out.println();
        for (String col : rows.get(0).keySet()) {
            System.out.printf("%-28s", col);
        }
        System.out.println();
        System.out.println("-".repeat(28 * rows.get(0).size()));
        for (Map<String, Object> row : rows) {
            for (Object val : row.values()) {
                System.out.printf("%-28s", val);
            }
            System.out.println();
        }
        System.out.println(rows.size() + " row(s)");
    }
}