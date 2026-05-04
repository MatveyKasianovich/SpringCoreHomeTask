package org.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserService {

    private final SessionFactory sessionFactory;
    private final TransactionHelper transactionHelper;
    private final AccountService accountService;

    @Autowired
    public UserService(SessionFactory sessionFactory,
                       TransactionHelper transactionHelper,
                       AccountService accountService) {
        this.sessionFactory = sessionFactory;
        this.transactionHelper = transactionHelper;
        this.accountService = accountService;
    }


    public void createUser(String login) {

        try (Session checkSession = sessionFactory.openSession()) {
            Long count = checkSession.createNativeQuery(
                            "SELECT COUNT(*) FROM users WHERE login = :login", Long.class)
                    .setParameter("login", login)
                    .getSingleResult();

            if (count > 0) {
                throw new IllegalArgumentException(
                        String.format("User with login '%s' already exists", login));
            }
        }


        transactionHelper.executeInTransaction(session -> {
            User user = new User(login);
            session.persist(user);
            session.flush();

            accountService.createAccountForUser(session, user.getId());

            System.out.println("User created: " + user);
            return user;
        });
    }


    public void showAllUsers() {
        try (Session session = sessionFactory.openSession()) {
            List<User> users = session.createQuery(
                            "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.accountList",
                            User.class)
                    .getResultList();

            if (users.isEmpty()) {
                System.out.println("No users in the system");
            } else {
                users.forEach(System.out::println);
            }
        }
    }


    public Optional<User> findUserById(int userId) {
        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            return Optional.ofNullable(user);
        }
    }

    // Проверка существования (boolean)
    public boolean userExists(int userId) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, userId) != null;
        }
    }
}