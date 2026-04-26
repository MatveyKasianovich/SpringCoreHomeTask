package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class UserService {

    private final AccountService accountService;
    private final AtomicInteger userIdGenerator = new AtomicInteger(1);
    private final Map<Integer, User> userMap = new HashMap<>();

    @Autowired
    public UserService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void createUser(String login) {
        boolean exists = userMap.values().stream()
                .anyMatch(user -> login.equals(user.getLogin()));

        if (!exists) {
            int newId = userIdGenerator.getAndIncrement();
            User newUser = new User(newId, login);
            newUser.setAccountList(new ArrayList<>());

            userMap.put(newUser.getId(), newUser);

            accountService.createNewAccount(newUser.getId());


            System.out.println("User created: " + newUser);
        } else {
            throw new IllegalArgumentException(String.format("User with login '%s' already exists", login));
        }
    }

    public void showAllUsers() {
        if (userMap.isEmpty()) {
            System.out.println("No users in the system");
            return;
        }
        userMap.values().forEach(System.out::println);
    }

    public User userExists(int userId) {
        return userMap.get(userId);
    }

}