package com.mcmirror;

import com.mcmirror.config.MirrorConfig;
import com.mcmirror.service.MirrorService;
import com.mcmirror.service.VersionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Minecraft Mirror — mirrors Minecraft core files, version JSONs, and asset indexes.
 *
 * <p>Usage:
 * <pre>
 *   java -jar mcmirror.jar              # interactive mode
 *   java -jar mcmirror.jar update       # run mirror
 *   java -jar mcmirror.jar list         # list available versions
 *   java -jar mcmirror.jar status       # check local mirror status
 * </pre>
 */
@Command(
        name = "mcmirror",
        version = "Minecraft Mirror 2.0.0",
        mixinStandardHelpOptions = true,
        subcommands = {
                Main.UpdateCommand.class,
                Main.ListCommand.class,
                Main.StatusCommand.class
        },
        description = "Mirrors Minecraft core files from Mojang servers"
)
public class Main implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String NAME = "Minecraft Mirror";
    private static final String VERSION = "2.0.0";

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        runInteractive();
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    // ── Subcommands ────────────────────────────────────────────────

    @Command(name = "update", description = "Download/update all mirrored files")
    static class UpdateCommand implements Callable<Integer> {

        @CommandLine.Option(names = {"-A", "--with-assets"},
                description = "Download full asset files (not just indexes)")
        private boolean withAssets;

        @Override
        public Integer call() {
            if (withAssets) {
                System.setProperty("mcmirror.withAssets", "true");
            }
            MirrorConfig config = new MirrorConfig();
            MirrorService service = new MirrorService(config);
            service.execute();
            return 0;
        }
    }

    @Command(name = "list", description = "List available Minecraft versions from remote manifest")
    static class ListCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            MirrorConfig config = new MirrorConfig();
            VersionService versionService = new VersionService(config);
            versionService.listVersions();
            return 0;
        }
    }

    @Command(name = "status", description = "Check local mirror status (which versions are downloaded)")
    static class StatusCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            MirrorConfig config = new MirrorConfig();
            MirrorService service = new MirrorService(config);
            service.status();
            return 0;
        }
    }

    // ── Interactive mode ────────────────────────────────────────────

    private static void runInteractive() {
        printBanner();

        MirrorConfig config = new MirrorConfig();
        MirrorService service = new MirrorService(config);

        try (Scanner scanner = new Scanner(System.in, "UTF-8")) {
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                switch (input.toLowerCase()) {
                    case "update":
                        service.execute();
                        break;
                    case "status":
                        service.status();
                        break;
                    case "help":
                    case "?":
                        printHelp();
                        break;
                    case "quit":
                    case "exit":
                    case "q":
                        System.out.println("Goodbye.");
                        return;
                    default:
                        if (!input.isEmpty()) {
                            System.out.println("Unknown command. Type 'help' for available commands.");
                        }
                }
            }
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  " + NAME + " v" + VERSION);
        System.out.println("  Type 'update' to start mirroring, 'help' for commands, 'quit' to exit.");
        System.out.println();
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  update    Download/update all mirrored files");
        System.out.println("  status    Check local mirror completeness");
        System.out.println("  help      Show this help");
        System.out.println("  quit      Exit the program");
        System.out.println();
        System.out.println("Configuration (system properties or -D flags):");
        System.out.println("  mcmirror.threads=N            Thread count for parallel downloads (default: 4)");
        System.out.println("  mcmirror.maxRetries=N         Max retry count (default: 3)");
        System.out.println("  mcmirror.verifyHash=BOOL      Verify SHA-1 after download (default: true)");
        System.out.println("  mcmirror.connectTimeoutMs=N   Connection timeout in ms (default: 10000)");
        System.out.println("  mcmirror.readTimeoutMs=N      Read timeout in ms (default: 30000)");
        System.out.println("  mcmirror.withAssets=BOOL      Download full asset files (default: false)");
        System.out.println();
    }
}
