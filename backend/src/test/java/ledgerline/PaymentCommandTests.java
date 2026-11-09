package ledgerline.payment;

import ledgerline.ledger.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentCommandTests {

    @Test
    void depositShouldSetCorrectAccounts() {
        PaymentCommand command =
                PaymentCommand.deposit("ACC100", new BigDecimal("500.00"));

        assertEquals(TransactionType.DEPOSIT, command.type());
        assertNull(command.fromAccount());
        assertEquals("ACC100", command.toAccount());
        assertEquals("ACC100", command.ownedAccountNumber());
    }

    @Test
    void withdrawalShouldUseFromAccountAsOwnedAccount() {
        PaymentCommand command =
                PaymentCommand.withdrawal("ACC100", new BigDecimal("200.00"));

        assertEquals(TransactionType.WITHDRAWAL, command.type());
        assertEquals("ACC100", command.ownedAccountNumber());
        assertNull(command.toAccount());
    }

    @Test
    void transferShouldIdentifyBothAccounts() {
        PaymentCommand command =
                PaymentCommand.transfer(
                        "ACC100", "ACC200", new BigDecimal("100.00"));

        assertEquals("ACC100", command.ownedAccountNumber());
        assertEquals("ACC200", command.toAccount());
    }

    @Test
    void sameRequestShouldHaveSameSignature() {
        PaymentCommand first =
                PaymentCommand.deposit("ACC100", new BigDecimal("500.00"));

        PaymentCommand second =
                PaymentCommand.deposit("ACC100", new BigDecimal("500.00"));

        assertEquals(first.signature(), second.signature());
    }
}