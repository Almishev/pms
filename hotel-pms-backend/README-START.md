# Как да стартирате Backend

## ⚠️ ВАЖНО: Използвайте Command Prompt (cmd.exe) или двойно кликнете start.bat

PowerShell има проблеми с парсирането на batch файлове. За най-добри резултати:

## Решение 1: Двойно кликване (НАЙ-ЛЕСНО) ✅

Двойно кликнете върху `start.bat` файла в `hotel-pms-backend` директорията!

## Решение 2: Command Prompt (cmd.exe)

1. Отворете **Command Prompt** (cmd.exe)
2. Изпълнете:
```cmd
cd C:\Users\Admin\Desktop\lamadev\hotel-pos\hotel-pms-backend
start.bat
```

Или директно:
```cmd
cd C:\Users\Admin\Desktop\lamadev\hotel-pos\hotel-pms-backend
set JAVA_HOME=C:\Program Files\Java\jdk-21
mvnw.cmd spring-boot:run
```

## Решение 3: PowerShell (ако трябва)

```powershell
cd C:\Users\Admin\Desktop\lamadev\hotel-pos\hotel-pms-backend
cmd /c "set JAVA_HOME=C:\Program Files\Java\jdk-21 && mvnw.cmd spring-boot:run"
```

## Преди стартиране:

✅ PostgreSQL е инсталиран и работи  
✅ Базата `hotel_pms` е създадена  
✅ Database credentials в `application.properties` са правилни

## След стартиране:

Backend ще бъде достъпен на: **http://localhost:8085**

Първото стартиране може да отнеме 2-3 минути, защото:
- Maven Wrapper изтегля Maven 3.9.5
- Maven изтегля всички зависимости
- Проектът се компилира
