package org.example;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class HibernateConfig {

    @Bean
    public SessionFactory sessionFactory() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Account.class);
        configuration.addAnnotatedClass(User.class);


        configuration.setProperty("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        configuration.setProperty("jakarta.persistence.jdbc.url", "jdbc:postgresql://localhost:5436/postgres");
        configuration.setProperty("jakarta.persistence.jdbc.user", "postgres");
        configuration.setProperty("jakarta.persistence.jdbc.password", "root");

        configuration.setProperty("hibernate.show_sql", "true");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");

        return configuration.buildSessionFactory();
    }
}

