package com.hotel.pms.backup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RemovableDriveFinder {
    private RemovableDriveFinder() {
    }

    static Path find() throws IOException {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return findWindows();
        }
        return findLinux();
    }

    private static Path findWindows() throws IOException {
        Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                "Get-CimInstance Win32_LogicalDisk | Where-Object { $_.DriveType -eq 2 } | ForEach-Object { $_.DeviceID }"
        ).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            if (process.waitFor() != 0) {
                return null;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
        for (String line : output.split("\\R")) {
            String drive = line.trim();
            if (drive.matches("[A-Za-z]:")) {
                Path root = Path.of(drive + "\\");
                if (Files.isDirectory(root) && Files.isWritable(root)) {
                    return root;
                }
            }
        }
        return null;
    }

    private static Path findLinux() throws IOException {
        Path mounts = Path.of("/proc/mounts");
        if (!Files.exists(mounts)) {
            return null;
        }
        for (String line : Files.readAllLines(mounts)) {
            String[] parts = line.split(" ");
            if (parts.length < 2 || !parts[0].startsWith("/dev/")) {
                continue;
            }
            String mount = unescape(parts[1]);
            if (mount.equals("/") || mount.startsWith("/boot")) {
                continue;
            }
            String block = blockName(Path.of(parts[0]).getFileName().toString());
            Path flag = Path.of("/sys/block", block, "removable");
            if (!Files.exists(flag)) {
                continue;
            }
            String value = Files.readString(flag).trim();
            Path target = Path.of(mount);
            if ("1".equals(value) && Files.isDirectory(target) && Files.isWritable(target)) {
                return target;
            }
        }
        return null;
    }

    static String blockName(String device) {
        Matcher nvme = Pattern.compile("^(nvme\\d+n\\d+)p\\d+$").matcher(device);
        if (nvme.matches()) {
            return nvme.group(1);
        }
        Matcher mmc = Pattern.compile("^(mmcblk\\d+)p\\d+$").matcher(device);
        if (mmc.matches()) {
            return mmc.group(1);
        }
        return device.replaceFirst("\\d+$", "");
    }

    private static String unescape(String mount) {
        return mount.replace("\\040", " ").replace("\\011", "\t").replace("\\012", "\n").replace("\\134", "\\");
    }
}
