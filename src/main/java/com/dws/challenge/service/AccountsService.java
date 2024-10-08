package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  @Autowired
  private NotificationService notificationService;

  private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transferAmount(TransferRequest transferRequest) {

      BigDecimal amount = transferRequest.getAmount();

      ReentrantLock lockFrom = locks.computeIfAbsent(transferRequest.getFromAccountId(), id -> new ReentrantLock());
      ReentrantLock lockTo = locks.computeIfAbsent(transferRequest.getToAccountId(), id -> new ReentrantLock());

      // Ensure a consistent ordering of locks to prevent deadlock
      ReentrantLock firstLock = lockFrom.hashCode() < lockTo.hashCode() ? lockFrom : lockTo;
      ReentrantLock secondLock = lockFrom.hashCode() < lockTo.hashCode() ? lockTo : lockFrom;

      boolean lockAcquired = false;

      try {
          firstLock.lock();
          secondLock.lock();
          lockAcquired = true;

          Account fromAccount = getAccount(transferRequest.getFromAccountId());
          Account toAccount = getAccount(transferRequest.getToAccountId());

          if (fromAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
              throw new InsufficientFundsException("Insufficient funds in account: " + fromAccount);
          }

          BigDecimal updatedFromAccountBalance = fromAccount.getBalance().subtract(amount);
          fromAccount.setBalance(updatedFromAccountBalance);
          accountsRepository.updateAccount(fromAccount);

          BigDecimal updatedToAccountBalance = toAccount.getBalance().add(amount);
          toAccount.setBalance(updatedToAccountBalance);
          accountsRepository.updateAccount(toAccount);
          this.notificationService.notifyAboutTransfer(toAccount, "Amount of " + amount + " successfully transferred from " + transferRequest.getFromAccountId());


      } finally {
          if (lockAcquired) {
              secondLock.unlock();
              firstLock.unlock();
          }
      }

  }
}
