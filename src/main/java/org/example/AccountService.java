package org.example;


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AccountService {

    private final SessionFactory sessionFactory;
    private final TransactionHelper transactionHelper;
    private final AccountProperties accountProperties;
    private  Map<Integer,Account>accountMap=new HashMap<>();
    private final UserService userService;

    @Autowired
    public AccountService(AccountProperties accountProperties, @Lazy UserService userService, TransactionHelper transactionHelper, SessionFactory sessionFactory) {
        this.accountProperties = accountProperties;
        this.userService = userService;
        this.transactionHelper=transactionHelper;
        this.sessionFactory = sessionFactory;
    }




    // Для внешних вызовов (из UI)
    public Account createAccountForUser(int userId) {
        return transactionHelper.executeInTransaction(session ->
                createAccountForUser(session, userId)
        );
    }

    // Для внутренних вызовов (из других сервисов)
    public Account createAccountForUser(Session session, int userId) {
        User user = session.get(User.class, userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Account account = new Account();
        account.setUser(user);
        account.setMoneyAmount(user.getAccountList().isEmpty() ?
                accountProperties.getDefault_amount() : 0);

        session.persist(account);
        return account;
    }


    public void accountDeposit(int id, float deposit) {
        if (!accountMap.containsKey(id)) {
            throw new IllegalArgumentException(String.format("Account with id: %d does not exist", id));
        }
        if (deposit <= 0) {
            throw new IllegalArgumentException("Deposit must be positive number");
        }

        Account account = accountMap.get(id);
        account.setMoneyAmount(account.getMoneyAmount() + deposit);

        System.out.printf("Deposited %.2f to account %d. New balance: %.2f\n", deposit, id, account.getMoneyAmount());
    }

    public void accountWithDraw(int id, float amount) {
        if (!accountMap.containsKey(id)) {
            throw new IllegalArgumentException(String.format("Account with id: %d does not exist", id));
        }
        Account account = accountMap.get(id);
        if (account.getMoneyAmount() < amount) {
            throw new IllegalArgumentException(String.format("Error: insufficient funds on account id=%d, moneyAmount=%.2f, attempted withdraw=%.2f",
                    account.getId(), account.getMoneyAmount(), amount));
        }
        account.setMoneyAmount(account.getMoneyAmount() - amount);

        System.out.printf("Withdrawn %.2f from account %d. New balance: %.2f\n", amount, id, account.getMoneyAmount());
    }

    public void accountClose(int accountId) {
        // Проверка существования аккаунта ДО транзакции
        Account accountToClose;
        try (Session checkSession = sessionFactory.openSession()) {
            accountToClose = checkSession.get(Account.class, accountId);

            if (accountToClose == null) {
                throw new IllegalArgumentException("Account not found: " + accountId);
            }

            // Проверяем, не единственный ли это аккаунт
            User user = accountToClose.getUser();
            List<Account> userAccounts = checkSession.createQuery(
                            "FROM Account WHERE user.id = :userId", Account.class)
                    .setParameter("userId", user.getId())
                    .getResultList();

            if (userAccounts.size() <= 1) {
                throw new IllegalStateException("User has the only account, you cannot close it");
            }
        }

        // Выполняем закрытие в транзакции
        transactionHelper.executeTransaction(session -> {
            // Загружаем аккаунт заново (уже в транзакции)
            Account account = session.get(Account.class, accountId);
            User user = account.getUser();
            float balance = account.getMoneyAmount();

            // Если есть деньги - переводим на другой аккаунт
            if (balance > 0) {
                // Находим любой другой аккаунт пользователя
                List<Account> otherAccounts = session.createQuery(
                                "FROM Account WHERE user.id = :userId AND id != :accountId", Account.class)
                        .setParameter("userId", user.getId())
                        .setParameter("accountId", accountId)
                        .getResultList();

                if (!otherAccounts.isEmpty()) {
                    Account targetAccount = otherAccounts.get(0);
                    targetAccount.setMoneyAmount(targetAccount.getMoneyAmount() + balance);
                    System.out.printf("Remaining balance %.2f transferred to account %d\n",
                            balance, targetAccount.getId());
                }
            }

            // Удаляем аккаунт
            session.remove(account);
            System.out.printf("Account with id=%d was successfully closed\n", accountId);
        });
    }

    public void accountTransfer(int sourceId,int targetId,float amount){

        Account sourceAcc=accountMap.get(sourceId);
        Account targetAcc=accountMap.get(targetId);

        if (sourceAcc==null||targetAcc==null){
            throw new IllegalArgumentException("Source or Target acc does not exist");
        }

        if(sourceAcc.getMoneyAmount()<amount){
            throw new IllegalStateException(String.format("Source account has not enough money to transfer amount=%d",amount));
        }else if(sourceAcc.getMoneyAmount()<0){
            throw new IllegalStateException("Source account has not enough money to transfer negative ammount");
        }

        if(sourceAcc.getUser().getId()==targetAcc.getUser().getId()){
            sourceAcc.setMoneyAmount(sourceAcc.getMoneyAmount()-amount);
            targetAcc.setMoneyAmount(targetAcc.getMoneyAmount()+amount);
        }else {
            sourceAcc.setMoneyAmount(sourceAcc.getMoneyAmount()-amount);
            targetAcc.setMoneyAmount(targetAcc.getMoneyAmount()+(amount*(1-accountProperties.getTransfer_commission())));
        }

    }

}
