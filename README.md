
Требования
Java 24

PostgreSQL 14+

Maven 3.8+

Установка и запуск
1. Настройка базы данных
sql
CREATE DATABASE minibank;
\q
Пользователь: postgres, пароль: root

2. Запуск
cd C:\путь\к\проекту && mvn clean compile exec:java -Dexec.mainClass="org.example.Main"

3. Работа с приложением
      После запуска в консоли появится приглашение к вводу команд.
      Доступные команды: USER_CREATE, SHOW_ALL_USERS, ACCOUNT_CREATE, ACCOUNT_DEPOSIT, ACCOUNT_WITHDRAW, ACCOUNT_TRANSFER, ACCOUNT_CLOSE, EXIT.

Следуйте подсказкам системы для ввода данных.

4.Завершение работы
   Введите команду EXIT для корректного завершения работы приложения.

