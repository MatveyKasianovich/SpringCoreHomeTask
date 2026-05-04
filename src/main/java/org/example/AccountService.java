package org.example;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AccountService {

    private final AccountProperties accountProperties;
    private  Map<Integer,Account>accountMap=new HashMap<>();
    private final UserService userService;

    @Autowired
    public AccountService(AccountProperties accountProperties, @Lazy UserService userService) {
        this.accountProperties = accountProperties;
        this.userService = userService;
    }

    private final AtomicInteger accountIdGenerator = new AtomicInteger(1);


    public Account createNewAccount(int userId) {
        User user = userService.userExists(userId);
        if (user == null) {
            throw new IllegalArgumentException("User with id " + userId + " does not exist");
        }

        boolean exists = accountMap.values().stream()
                .anyMatch(account -> userId == account.getUserId());

        Account newAccount;
        if (!exists) {
            newAccount = new Account(accountIdGenerator.getAndIncrement(), accountProperties.getDefault_amount(), userId);
        } else {
            newAccount = new Account(accountIdGenerator.getAndIncrement(), 0, userId);
        }

        accountMap.put(newAccount.getId(), newAccount);
        user.getAccountList().add(newAccount);

        return newAccount;
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
        Account account = accountMap.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }

        int accountUserId = account.getUserId();

        boolean hasOtherAccounts = accountMap.values().stream()
                .anyMatch(acc -> acc.getUserId() == accountUserId && acc.getId() != accountId);

        if (!hasOtherAccounts) {
            throw new IllegalStateException("User has the only account, you cannot close it");
        }

        float moneyAmountSourceAcc = account.getMoneyAmount();

        Account firstExistingAccount = accountMap.values().stream()
                .filter(acc -> acc.getUserId() == accountUserId && acc.getId() != accountId)
                .findFirst()
                .orElse(null);


        if (moneyAmountSourceAcc > 0 && firstExistingAccount != null) {
            accountTransfer(accountId, firstExistingAccount.getId(), moneyAmountSourceAcc);
            System.out.printf("Remaining balance %.2f transferred to account %d\n",
                    moneyAmountSourceAcc, firstExistingAccount.getId());
        }

        accountMap.remove(accountId);

        User user = userService.userExists(accountUserId);
        if (user != null) {
            user.getAccountList().remove(account);
        }

        System.out.printf("Account with id=%d was successfully closed\n", accountId);
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

        if(sourceAcc.getUserId()==targetAcc.getUserId()){
            sourceAcc.setMoneyAmount(sourceAcc.getMoneyAmount()-amount);
            targetAcc.setMoneyAmount(targetAcc.getMoneyAmount()+amount);
        }else {
            sourceAcc.setMoneyAmount(sourceAcc.getMoneyAmount()-amount);
            targetAcc.setMoneyAmount(targetAcc.getMoneyAmount()+(amount*(1-accountProperties.getTransfer_commission())));
        }

    }

}
