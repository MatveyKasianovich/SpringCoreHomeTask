package org.example;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Scanner;

@Component
public class ConsoleListener {

    @PostConstruct
    public void init() {
        System.out.println("MiniBank Application Started!");
        System.out.println("Welcome to MiniBank!");
        System.out.println("Ready to accept commands.\n");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("MiniBank Application Shutting Down...");
        System.out.println("Goodbye!");
        scanner.close();
    }

    private final UserService userService;
    private final AccountService accountService;
    private final Scanner scanner = new Scanner(System.in);

    @Autowired
    public ConsoleListener(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.println(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Error: Enter a number. You enter: " + input);
            }
        }
    }

    private float readFloat(String prompt) {
        while (true) {
            System.out.println(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Float.parseFloat(input);
            } catch (NumberFormatException e) {
                System.out.println("Error: Enter a number. You enter: " + input);
            }
        }
    }

    public void start() {
        System.out.println("MiniBank started. Type EXIT to stop.");
        System.out.println("Available commands: USER_CREATE, SHOW_ALL_USERS, ACCOUNT_CREATE, " +
                "ACCOUNT_DEPOSIT, ACCOUNT_WITHDRAW, ACCOUNT_TRANSFER, ACCOUNT_CLOSE, EXIT");

        while (true) {
            try {
                System.out.println("\nEnter command:");
                String command = scanner.nextLine().trim().toUpperCase();

                switch (command) {
                    case "USER_CREATE":
                        createUser();
                        break;
                    case "SHOW_ALL_USERS":
                        userService.showAllUsers();
                        break;
                    case "ACCOUNT_CREATE":
                        createAccount();
                        break;
                    case "ACCOUNT_DEPOSIT":
                        deposit();
                        break;
                    case "ACCOUNT_WITHDRAW":
                        withdraw();
                        break;
                    case "ACCOUNT_TRANSFER":
                        transfer();
                        break;
                    case "ACCOUNT_CLOSE":
                        closeAccount();
                        break;
                    case "EXIT":
                        System.out.println("MiniBank stopped.");
                        return;
                    default:
                        System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void createUser() {
        System.out.println("Enter login:");
        String login = scanner.nextLine().trim();
        try {
            userService.createUser(login);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createAccount() {
        int userId = readInt("Enter user id:");
        try {
            Account account = accountService.createNewAccount(userId);
            System.out.println("Account created: " + account);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void deposit() {
        int id = readInt("Enter account id:");
        float amount = readFloat("Enter amount:");
        try {
            accountService.accountDeposit(id, amount);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void withdraw() {
        int id = readInt("Enter account id:");
        float amount = readFloat("Enter amount:");
        try {
            accountService.accountWithDraw(id, amount);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void transfer() {
        int sourceId = readInt("Enter source account id:");
        int targetId = readInt("Enter target account id:");
        float amount = readFloat("Enter amount:");
        try {
            accountService.accountTransfer(sourceId, targetId, amount);
            System.out.println("Transfer completed");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void closeAccount() {
        int accountId = readInt("Enter account id to close:");
        try {
            accountService.accountClose(accountId);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}