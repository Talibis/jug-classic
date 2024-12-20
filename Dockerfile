# Используем официальный образ Java как базовый
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию в контейнере
WORKDIR /app

# Копируем собранный JAR-файл в контейнер
COPY build/libs/basa-0.0.1-SNAPSHOT.jar app.jar

# Создаем директории для логов
RUN mkdir -p /app/logs /logs

# Открываем порт, который будет использовать приложение
EXPOSE 8080

# Монтируем тома для логов
VOLUME /app/logs
VOLUME /logs

# Команда для запуска приложения при старте контейнера
ENTRYPOINT ["java","-jar","/app/app.jar"]
