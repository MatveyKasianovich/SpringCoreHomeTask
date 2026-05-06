package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AccountService {

    private final SessionFactory sessionFactory;
    private final TransactionHelper transactionHelper;
    private final AccountProperties accountProperties;
    private Map<Integer, Account> accountMap = new HashMap<>();
    private final UserService userService;

    private static final BigDecimal MIN_TRANSFER_AMOUNT = new BigDecimal("10.00");

    @Autowired
    public AccountService(AccountProperties accountProperties, @Lazy UserService userService,
                          TransactionHelper transactionHelper, SessionFactory sessionFactory) {
        this.accountProperties = accountProperties;
        this.userService = userService;
        this.transactionHelper = transactionHelper;
        this.sessionFactory = sessionFactory;
    }

    public Account createAccountForUser(int userId) {
        try (Session checkSession = sessionFactory.openSession()) {
            User user = checkSession.get(User.class, userId);
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
        }
        return transactionHelper.executeInTransaction(session ->
                createAccountForUser(session, userId)
        );
    }

    public Account createAccountForUser(Session session, int userId) {
        User user = session.get(User.class, userId);
        Account account = new Account();
        account.setUser(user);

        BigDecimal initialAmount = user.getAccountList().isEmpty() ?
                accountProperties.getDefault_amount() : BigDecimal.ZERO;
        account.setMoneyAmount(initialAmount);

        session.persist(account);
        user.getAccountList().add(account);
        return account;
    }

    public void accountDeposit(int id, BigDecimal deposit) {
        if (deposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit must be positive number");
        }

        try (Session checkSession = sessionFactory.openSession()) {
            Account accountToCheck = checkSession.get(Account.class, id);

            if (accountToCheck == null) {
                throw new IllegalArgumentException("Account not found: " + id);
            }
        }

        transactionHelper.executeTransaction(session -> {
            Account account = session.get(Account.class, id);
            account.setMoneyAmount(account.getMoneyAmount().add(deposit));
            System.out.printf("Deposited %.2f to account %d. New balance: %.2f\n",
                    deposit, id, account.getMoneyAmount());
        });
    }

    public void accountWithDraw(int id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        try (Session checkSession = sessionFactory.openSession()) {
            Account accountToCheck = checkSession.get(Account.class, id);

            if (accountToCheck == null) {
                throw new IllegalArgumentException("Account not found: " + id);
            }
            if (accountToCheck.getMoneyAmount().compareTo(amount) < 0) {
                throw new IllegalArgumentException(String.format(
                        "Error: insufficient funds on account id=%d, moneyAmount=%.2f, attempted withdraw=%.2f",
                        accountToCheck.getId(), accountToCheck.getMoneyAmount(), amount));
            }

            transactionHelper.executeTransaction(session -> {
                Account accountToWithDraw = session.get(Account.class, id);
                accountToWithDraw.setMoneyAmount(accountToWithDraw.getMoneyAmount().subtract(amount));
                System.out.printf("Withdrawn %.2f from account %d. New balance: %.2f\n",
                        amount, id, accountToWithDraw.getMoneyAmount());
            });
        }
    }

    public void accountClose(int accountId) {
        try (Session checkSession = sessionFactory.openSession()) {
            Account accountToClose = checkSession.get(Account.class, accountId);

            if (accountToClose == null) {
                throw new IllegalArgumentException("Account not found: " + accountId);
            }

            User user = accountToClose.getUser();
            List<Account> userAccounts = checkSession.createQuery(
                            "FROM Account WHERE user.id = :userId", Account.class)
                    .setParameter("userId", user.getId())
                    .getResultList();

            if (userAccounts.size() <= 1) {
                throw new IllegalStateException("User has the only account, you cannot close it");
            }
        }

        transactionHelper.executeTransaction(session -> {
            Account account = session.get(Account.class, accountId);
            User user = account.getUser();
            BigDecimal balance = account.getMoneyAmount();

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                List<Account> otherAccounts = session.createQuery(
                                "FROM Account WHERE user.id = :userId AND id != :accountId", Account.class)
                        .setParameter("userId", user.getId())
                        .setParameter("accountId", accountId)
                        .getResultList();

                if (!otherAccounts.isEmpty()) {
                    Account targetAccount = otherAccounts.get(0);
                    targetAccount.setMoneyAmount(targetAccount.getMoneyAmount().add(balance));
                    System.out.printf("Remaining balance %.2f transferred to account %d\n",
                            balance, targetAccount.getId());
                }
            }

            session.remove(account);
            System.out.printf("Account with id=%d was successfully closed\n", accountId);
        });
    }

    public void accountTransfer(int sourceId, int targetId, BigDecimal amount) {
        try (Session checkSession = sessionFactory.openSession()) {
            Account sourceAcc = checkSession.get(Account.class, sourceId);
            Account targetAcc = checkSession.get(Account.class, targetId);

            if (sourceAcc == null || targetAcc == null) {
                throw new IllegalArgumentException("Source or Target acc does not exist");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Amount must be positive");
            }

            if (amount.compareTo(MIN_TRANSFER_AMOUNT) < 0) {
                throw new IllegalStateException(String.format(
                        "Minimum transfer amount is %.2f", MIN_TRANSFER_AMOUNT));
            }

            if (sourceAcc.getMoneyAmount().compareTo(amount) < 0) {
                throw new IllegalStateException(String.format(
                        "Source account has not enough money to transfer amount=%.2f", amount));
            }
        }

        transactionHelper.executeTransaction(session -> {
            Account sourceAcc = session.get(Account.class, sourceId);
            Account targetAcc = session.get(Account.class, targetId);

            if (sourceAcc.getUser().getId() == targetAcc.getUser().getId()) {
                sourceAcc.setMoneyAmount(sourceAcc.getMoneyAmount().subtract(amount));
                targetAcc.setMoneyAmount(targetAcc.getMoneyAmount().add(amount));
            } else {
                BigDecimal commission = accountProperties.getTransfer_commission();
                BigDecimal amountAfterCommission = amount.multiply(BigDecimal.ONE.subtract(commission));
                sourceAcc.setMoneyAmount(sourceAcc.getMoneyAmount().subtract(amount));
                targetAcc.setMoneyAmount(targetAcc.getMoneyAmount().add(amountAfterCommission));
                System.out.printf("Transfer with commission %.2f%%. Amount after commission: %.2f\n",
                        commission.multiply(new BigDecimal("100")), amountAfterCommission);
            }
        });
    }
}
