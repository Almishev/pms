package com.hotel.pms.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatabaseBackupService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
    private static final DateTimeFormatter STATE = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Pattern JDBC = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)");

    private final boolean enabled;
    private final String directoryOverride;
    private final String pgDumpOverride;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Path stateFile;
    private final Object lock = new Object();

    public DatabaseBackupService(
            @Value("${pms.backup.enabled:true}") boolean enabled,
            @Value("${pms.backup.directory:}") String directoryOverride,
            @Value("${pms.backup.pg-dump:}") String pgDumpOverride,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        this.enabled = enabled;
        this.directoryOverride = directoryOverride == null ? "" : directoryOverride.trim();
        this.pgDumpOverride = pgDumpOverride == null ? "" : pgDumpOverride.trim();
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.stateFile = Path.of(System.getProperty("user.home"), ".hotel-pms", "last-backup.txt");
    }

    @Scheduled(cron = "${pms.backup.cron:0 35 23 * * *}")
    public void scheduledBackup() {
        runIfDue("планиран час 23:35");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backupMissedWhilePoweredOff() {
        runIfDue("включване на компютъра");
    }

    void runIfDue(String reason) {
        if (!enabled) {
            return;
        }
        synchronized (lock) {
            try {
                Files.createDirectories(stateFile.getParent());
                try (FileChannel channel = FileChannel.open(stateFile.resolveSibling("backup.lock"),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock fileLock = channel.tryLock()) {
                    if (fileLock == null) {
                        log.info("Бекъпът вече тече.");
                        return;
                    }
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime last = readLastSuccess();
                    if (!BackupSchedule.isDue(last, now)) {
                        return;
                    }
                    Path file = dump(now);
                    writeLastSuccess(now);
                    log.info("Бекъпът е записан ({}): {}", reason, file);
                }
            } catch (Exception ex) {
                log.error("Бекъпът не беше записан ({}): {}", reason, ex.getMessage());
            }
        }
    }

    private Path dump(LocalDateTime now) throws IOException, InterruptedException {
        Matcher matcher = JDBC.matcher(jdbcUrl);
        if (!matcher.find()) {
            throw new IOException("Непознат адрес на базата: " + jdbcUrl);
        }
        String host = matcher.group(1);
        String port = matcher.group(2) == null ? "5432" : matcher.group(2);
        String database = matcher.group(3);

        Path drive = backupDirectory();
        Path folder = drive.resolve("hotel-pms-backups");
        Files.createDirectories(folder);
        Path file = folder.resolve("hotel-pms-" + now.format(STAMP) + ".sql");

        List<String> command = new ArrayList<>();
        command.add(pgDumpCommand());
        command.add("-h");
        command.add(host);
        command.add("-p");
        command.add(port);
        command.add("-U");
        command.add(username);
        command.add("-d");
        command.add(database);
        command.add("--no-owner");
        command.add("--no-acl");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("PGPASSWORD", password);
        builder.environment().put("PGCLIENTENCODING", "UTF8");
        builder.redirectOutput(file.toFile());
        builder.redirectErrorStream(false);
        Process process = builder.start();
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = process.waitFor();
        if (code != 0 || !Files.exists(file) || Files.size(file) == 0) {
            Files.deleteIfExists(file);
            throw new IOException(errors.isBlank() ? "pg_dump завърши с код " + code : errors.trim());
        }
        return file;
    }

    private Path backupDirectory() throws IOException {
        if (!directoryOverride.isBlank()) {
            Path configured = Path.of(directoryOverride);
            if (!Files.isDirectory(configured) || !Files.isWritable(configured)) {
                throw new IOException("Папката за бекъп не е достъпна: " + configured);
            }
            return configured;
        }
        Path detected = RemovableDriveFinder.find();
        if (detected == null) {
            throw new IOException("Няма закачен външен диск. Включете флашката и оставете системата да опита отново.");
        }
        return detected;
    }

    private String pgDumpCommand() throws IOException {
        if (!pgDumpOverride.isBlank()) {
            return pgDumpOverride;
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(java.io.File.pathSeparator)) {
                Path candidate = Path.of(dir, isWindows() ? "pg_dump.exe" : "pg_dump");
                if (Files.isExecutable(candidate)) {
                    return candidate.toString();
                }
            }
        }
        if (isWindows()) {
            Path root = Path.of("C:/Program Files/PostgreSQL");
            if (Files.isDirectory(root)) {
                try (var versions = Files.list(root)) {
                    return versions
                            .map(version -> version.resolve("bin").resolve("pg_dump.exe"))
                            .filter(Files::isRegularFile)
                            .sorted()
                            .reduce((first, second) -> second)
                            .map(Path::toString)
                            .orElseThrow(() -> new IOException("pg_dump не е намерен"));
                }
            }
        }
        throw new IOException("pg_dump не е намерен");
    }

    private LocalDateTime readLastSuccess() {
        try {
            if (!Files.exists(stateFile)) {
                return null;
            }
            String text = Files.readString(stateFile, StandardCharsets.UTF_8).trim();
            return text.isEmpty() ? null : LocalDateTime.parse(text, STATE);
        } catch (Exception ex) {
            log.warn("Не се чете последният бекъп: {}", ex.getMessage());
            return null;
        }
    }

    private void writeLastSuccess(LocalDateTime time) throws IOException {
        Files.createDirectories(stateFile.getParent());
        Files.writeString(stateFile, time.format(STATE), StandardCharsets.UTF_8);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
