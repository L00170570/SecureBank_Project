package bank;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecureBankTester {
    private static final Logger logger = Logger.getLogger(SecureBankTester.class.getName());
    private static final String OPTION_REGISTER = "1";
    private static final String OPTION_LOGIN = "2";

    public static void main(String[] args) {
        // Open scanner and initialize the database
        try (Scanner scanner = new Scanner(System.in)) {
            SecureBank bank = new SecureBank();
            bank.initDatabase();

            // Display initial options
            printMenu("1. Register", "2. Login");
            String choice = scanner.nextLine();

            if (OPTION_REGISTER.equals(choice)) {
                registerUser(scanner, bank);  // handle registration
            } else if (OPTION_LOGIN.equals(choice)) {
                login(scanner, bank, 3);  // handle login with 3 attempts
            } else {
                logger.info("Invalid choice.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error occurred", e);
        }
    }

    // Handles user registration with input validation
    private static void registerUser(Scanner scanner, SecureBank bank) {
        logger.info("Enter username: ");
        String username = scanner.nextLine();
        logger.info("Enter password: ");
        String password = scanner.nextLine();
        logger.info("Enter email: ");
        String email = scanner.nextLine();

        // Validate inputs before registering
        if (!bank.isValidUsername(username)) {
            logger.info("Invalid username format.");
            return;
        }
        if (!bank.isValidPassword(password)) {
            logger.info("Invalid password format.");
            return;
        }
        if (!bank.isValidEmail(email)) {
            logger.info("Invalid email format.");
            return;
        }

        // Try to register the user
        try {
            bank.registerUser(username, password);
            logger.info("Registered successfully!");
        } catch (Exception e) {
            logger.info("Registration failed: " + e.getMessage());
        }
    }

    // Handles user login and OTP verification
    private static void login(Scanner scanner, SecureBank bank, int attempts) {
        boolean loggedIn = false;

        while (attempts > 0 && !loggedIn) {
            String username = getInput(scanner, "Username: ");
            String password = getInput(scanner, "Password: ");

            // Validate input formats first
            if (!isValidCredentials(bank, username, password)) {
                logger.log(Level.INFO, "Invalid credentials format. Attempts left: {0}", --attempts);
            }
            // If input is valid and authentication succeeds
            else if (bank.authenticateUser(username, password)) {
                if (verifyOtp(scanner)) {
                    logger.info("Login successful!");
                    loggedIn = true;
                    bankingMenu(scanner, bank, username); // show banking options
                } else {
                    logger.info("Wrong OTP. Access denied.");
                    loggedIn = true; // still exit loop even with incorrect OTP
                }
            }
            // Credentials are invalid
            else {
                logger.log(Level.INFO, "Incorrect credentials. Attempts left: {0}", --attempts);
            }

            if (attempts == 0) {
                logger.info("Too many failed attempts. Locked out.");
            }
        }
    }

    // Displays the banking menu after login
    private static void bankingMenu(Scanner scanner, SecureBank bank, String username) {
        boolean active = true;
        while (active) {
            printMenu("1. Check Balance", "2. Deposit", "3. Withdraw", "4. Logout");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    double balance = bank.getBalance(username);
                    logger.log(Level.INFO, "Current Balance: ${0}", balance);
                    break;
                case "2":
                    logger.info("Enter amount to deposit:");
                    try {
                        double depositAmount = Double.parseDouble(scanner.nextLine());
                        if (bank.deposit(username, depositAmount)) {
                            logger.info("Deposit successful.");
                        } else {
                            logger.info("Invalid deposit amount.");
                        }
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid input: Please enter a numeric value.");
                    }
                    break;
                case "3":
                    logger.info("Enter amount to withdraw:");
                    try {
                        double withdrawAmount = Double.parseDouble(scanner.nextLine());
                        if (bank.withdraw(username, withdrawAmount)) {
                            logger.info("Withdrawal successful.");
                        } else {
                            logger.info("Insufficient funds or invalid amount.");
                        }
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid input: Please enter a numeric value.");
                    }
                    break;
                case "4":
                    logger.info("Logging out...");
                    active = false;
                    break;
                default:
                    logger.info("Invalid option.");
            }
        }
    }

    // Checks if both username and password are valid formats
    private static boolean isValidCredentials(SecureBank bank, String username, String password) {
        boolean valid = true;
        if (!bank.isValidUsername(username)) {
            logger.info("Invalid username format.");
            valid = false;
        }
        if (!bank.isValidPassword(password)) {
            logger.info("Invalid password format.");
            valid = false;
        }
        return valid;
    }

    // Prompts the user and returns their input
    private static String getInput(Scanner scanner, String prompt) {
        logger.info(prompt);
        return scanner.nextLine();
    }

    // Simulates OTP verification
    private static boolean verifyOtp(Scanner scanner) {
        logger.info("OTP sent: 123456");
        logger.info("Enter OTP: ");
        String otp = scanner.nextLine();
        return "123456".equals(otp);
    }

    // Prints a formatted menu with given options
    private static void printMenu(String... options) {
        StringBuilder sb = new StringBuilder("\n--- Menu ---");
        for (String option : options) sb.append("\n").append(option);
        logger.info(sb.toString());
    }
}
