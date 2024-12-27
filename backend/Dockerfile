# Этап 1: Сборка
FROM gradle:8.3-jdk17 AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем весь исходный код в контейнер
COPY . .

# Собираем проект внутри контейнера
RUN gradle build -x test --no-daemon

# Этап 2: Исполняемый контейнер
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR-файл из первого этапа
COPY --from=build /app/build/libs/*.jar app.jar

# Открываем порт для приложения
EXPOSE 8080

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
