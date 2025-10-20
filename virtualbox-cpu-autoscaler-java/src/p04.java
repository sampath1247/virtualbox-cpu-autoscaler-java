import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class p04 {
    // Constants
    private static final String VM_NAME = "cicc2_group06";
    private static final int MAX_CPUS = 4;
    private static final int MIN_CPUS = 1;
    private static final int CHECK_INTERVAL_MS = 10_000;
    private static final int HIGH_IDLE_THRESHOLD_COUNT = 2;
    private static final double LOW_IDLE_THRESHOLD = 75.0;
    private static final double HIGH_IDLE_THRESHOLD = 85.0;
    private static final String MONITOR_FILE = "monitor.dat";

    // Tracking variables
    private static int currentCpus = 1;
    private static int highIdleCount = 0;

    public static void main(String[] args) {
        // Initial setup: Enable metrics collection
        executeCommand("VBoxManage metrics setup --period 2 " + VM_NAME + " Guest/CPU/Load/Idle");
        System.out.println("Starting CPU monitoring for " + VM_NAME);

        while (true) {
            try {
                // 1) Update the monitor.dat file
                updateMonitorDat();

                // 2) Read the latest idle percentage from monitor.dat
                double idle = getLatestIdle();
                System.out.printf("Idle: %.2f%%, CPUs: %d%n", idle, currentCpus);

                // 3) Decision logic (unchanged)
                if (idle < LOW_IDLE_THRESHOLD && currentCpus < MAX_CPUS) {
                    System.out.println("CPU usage high, plugging CPU " + currentCpus);
                    executeCommand("VBoxManage controlvm " + VM_NAME + " plugcpu " + currentCpus);
                    currentCpus++;
                    highIdleCount = 0;
                } else if (idle > HIGH_IDLE_THRESHOLD && currentCpus > MIN_CPUS) {
                    highIdleCount++;
                    if (highIdleCount >= HIGH_IDLE_THRESHOLD_COUNT) {
                        int cpuToUnplug = currentCpus - 1;
                        System.out.println("CPU idle for threshold, unplugging CPU " + cpuToUnplug);
                        executeCommand("VBoxManage controlvm " + VM_NAME + " unplugcpu " + cpuToUnplug);
                        currentCpus--;
                        highIdleCount = 0;
                    }
                } else {
                    highIdleCount = 0;
                }

                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    /** Runs the VBoxManage metrics query and writes its full output to monitor.dat */
    private static void updateMonitorDat() throws IOException, InterruptedException {
        List<String> cmd = Arrays.asList(
            "VBoxManage", "metrics", "query",
            VM_NAME, "Guest/CPU/Load/Idle"
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedWriter out = new BufferedWriter(new FileWriter(MONITOR_FILE))
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                out.write(line);
                out.newLine();
            }
        }

        int exit = proc.waitFor();
        if (exit != 0) {
            throw new IOException("VBoxManage returned exit code " + exit);
        }
    }

    /** Reads monitor.dat and returns the last “Guest/CPU/Load/Idle” percentage */
    private static double getLatestIdle() {
        String lastLine = null;
        try (BufferedReader br = new BufferedReader(new FileReader(MONITOR_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Guest/CPU/Load/Idle")) {
                    lastLine = line.trim();
                }
            }
            if (lastLine == null) {
                throw new IOException("No Guest/CPU/Load/Idle entries found");
            }
            // split on whitespace and parse the last token (percentage)
            String[] parts = lastLine.split("\\s+");
            String token = parts[parts.length - 1];
            if (token.endsWith("%")) {
                token = token.substring(0, token.length() - 1);
            }
            return Double.parseDouble(token);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading " + MONITOR_FILE + ": " + e.getMessage());
            return -1;
        }
    }

    /** Utility for running arbitrary VBoxManage commands if needed elsewhere */
    private static String executeCommand(String command) {
        try {
            // simple split on spaces—if your commands grow more complex, consider a List<String>
            Process p = Runtime.getRuntime().exec(command);
            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(p.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = in.readLine()) != null) {
                    sb.append(l).append("\n");
                }
                p.waitFor();
                return sb.toString();
            }
        } catch (Exception e) {
            System.err.println("Command execution failed: " + e.getMessage());
            return null;
        }
    }
}
