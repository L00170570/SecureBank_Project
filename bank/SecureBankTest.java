package bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecureBankTest {
    private SecureBank bank;

    @BeforeEach
    public void setUp() {
        bank = new SecureBank();
        bank.initDatabase();
    }

    @Test
    public void testIsValidUsername() {
        assertTrue(bank.isValidUsername("john.doe"));
        assertFalse(bank.isValidUsername("john_doe"));
    }

    @Test
    public void testIsValidPassword() {
        assertTrue(bank.isValidPassword("Pass#123"));
        assertFalse(bank.isValidPassword("pass123"));
    }

    @Test
    public void testDepositAndBalance() throws RegistrationException {
        String username = "test.user";
        String password = "Test#123";

        if (!bank.authenticateUser(username, password)) {
            bank.registerUser(username, password);
        }

        double initial = bank.getBalance(username);
        assertTrue(bank.deposit(username, 100.0));
        double newBalance = bank.getBalance(username);
        assertEquals(initial + 100.0, newBalance, 0.01);
    }
}