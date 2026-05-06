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


        configuration.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:postgresql://localhost:5436/postgres");
        configuration.setProperty("hibernate.connection.username", "postgres");
        configuration.setProperty("hibernate.connection.password", "root");


        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");


        configuration.setProperty("hibernate.hbm2ddl.auto", "update");


        configuration.setProperty("hibernate.current_session_context_class", "thread");


        configuration.setProperty("hibernate.show_sql", "true");
        configuration.setProperty("hibernate.format_sql", "true");

        return configuration.buildSessionFactory();
    }
}