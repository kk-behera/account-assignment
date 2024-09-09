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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AccountServiceTest {

    private AccountsService accountService;
    private AccountsRepository accountsRepository;
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        accountsRepository = new AccountsRepositoryInMemory();
        accountService = new AccountsService(accountsRepository, notificationService);

        // Setup initial accounts
        Account account1 = new Account("1", new BigDecimal("1200.0"));
        Account account2 = new Account("2", new BigDecimal("500.0"));
        accountService.createAccount(account1);
        accountService.createAccount(account2);
    }

    @Autowired
    private AccountsService accountsService;

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
                    accountService.transferAmount(new TransferRequest("1", "2", new BigDecimal("100")));
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
        Account account1 = accountService.getAccount("1");
        Account account2 = accountService.getAccount("2");

        assertEquals(new BigDecimal("200.0"), account1.getBalance());
        assertEquals(new BigDecimal("1500.0"), account2.getBalance());

        executor.shutdown();
    }




}
