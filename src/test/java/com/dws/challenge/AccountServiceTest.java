package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AccountServiceTest {

    private AccountsService accountsService;
    private AccountsRepository accountsRepository;
    @MockBean
   private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        accountsRepository = new AccountsRepositoryInMemory();


        accountsService = new AccountsService(accountsRepository);
       //

        // Setup initial accounts
        Account account1 = new Account("1", new BigDecimal("1200.0"));
        Account account2 = new Account("2", new BigDecimal("500.0"));
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);
    }


    @Test
    void addAccount() {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    void addAccount_failsOnDuplicateId() {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    //Test in a multi threaded scenario where 10 threads are working
    @Test
    public void testConcurrentTransfers() throws InterruptedException {
        // Create a CountDownLatch to synchronize threads
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    // Perform a transfer
                    accountsService.transferAmount(new TransferRequest("1", "2", new BigDecimal("100")));
                } catch (Exception e) {
                    // Handle exceptions as needed
                } finally {
                    latch.countDown(); // Signal that this thread is done
                }
            });
        }

        // Wait for all threads to complete
        latch.await();

        // Verify final balances
        Account account1 = accountsService.getAccount("1");
        Account account2 = accountsService.getAccount("2");

        assertEquals(new BigDecimal("200.0"), account1.getBalance());
        assertEquals(new BigDecimal("1500.0"), account2.getBalance());

        executor.shutdown();
    }
@Test
public void test_transfer_higher_amount_than_available_in_account() {
       assertThrows(InsufficientFundsException.class, () -> accountsService.transferAmount(new TransferRequest("1", "2", new BigDecimal("1500.0"))));
}


}
