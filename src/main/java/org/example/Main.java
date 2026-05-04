package org.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            ConsoleListener consoleListener = context.getBean(ConsoleListener.class);
            consoleListener.start();
        }
    }
}