/*
 * TerminalPet - a tiny virtual cat that lives in your terminal.
 *
 * Run it (Java 11 or newer, no build, no dependencies):
 *     java TerminalPet.java
 *
 * It remembers you: state is saved to pet.save between runs, and the cat
 * gets hungry and sleepy in real time while you are away.
 *
 * Built-in checks, zero test dependencies:
 *     java TerminalPet.java --self-test
 *
 * Non-interactive tour:
 *     java TerminalPet.java --demo
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;

public class TerminalPet {

    // Emoji as Unicode escapes, so the source is plain ASCII and behaves on
    // any platform charset (Windows terminals included).
    static final String FACE_THRILLED = "\uD83D\uDE3B";
    static final String FACE_CONTENT = "\uD83D\uDE3A";
    static final String FACE_HUNGRY = "\uD83D\uDE3F";
    static final String FACE_SLEEPY = "\uD83D\uDE34";
    static final String FACE_GRUMPY = "\uD83D\uDE40";
    static final String FACE_MISERABLE = "\uD83D\uDE3E";
    static final String FISH = "\uD83D\uDC1F";
    static final String YARN = "\uD83E\uDDF6";
    static final String SLEEP = "\uD83D\uDCA4";
    static final String HEART = "\u2764\uFE0F";
    static final String WAVE = "\uD83D\uDC4B";
    static final String SPARKLES = "\u2728";

    /** How the cat feels, derived from its lowest stat. */
    enum Mood {
        THRILLED(FACE_THRILLED, "Purring at full volume.", "Life is good. You are here."),
        CONTENT(FACE_CONTENT, "Slow blink. That means trust.", "Tail up, world in order."),
        HUNGRY(FACE_HUNGRY, "Staring at the empty bowl.", "A pointed meow at the kitchen."),
        SLEEPY(FACE_SLEEPY, "Eyes half closed mid-sentence.", "Yawning like it is a job."),
        GRUMPY(FACE_GRUMPY, "Tail flicking. Watch it.", "Judging you from the sofa arm."),
        MISERABLE(FACE_MISERABLE, "Curled in a sad little loaf.", "Not okay. Needs you now.");

        final String face;
        final String[] lines;

        Mood(String face, String... lines) {
            this.face = face;
            this.lines = lines;
        }

        String line(Random random) {
            return lines[random.nextInt(lines.length)];
        }
    }

    /** The cat itself: three stats, 0-100, all "higher is better". */
    static class Pet {
        static final int MAX = 100;
        // Real-time decay per hour away. Slow enough to forgive a workday.
        static final int FULLNESS_DECAY_PER_HOUR = 6;
        static final int HAPPINESS_DECAY_PER_HOUR = 5;
        static final int ENERGY_DECAY_PER_HOUR = 3;

        final String name;
        int fullness;
        int happiness;
        int energy;
        Instant lastSeen;

        Pet(String name) {
            this(name, 80, 80, 80, Instant.now());
        }

        Pet(String name, int fullness, int happiness, int energy, Instant lastSeen) {
            this.name = name;
            this.fullness = clamp(fullness);
            this.happiness = clamp(happiness);
            this.energy = clamp(energy);
            this.lastSeen = lastSeen;
        }

        static int clamp(int value) {
            return Math.max(0, Math.min(MAX, value));
        }

        /** Stats drift down with real elapsed time. Returns hours away. */
        long applyDecay(Instant now) {
            long minutes = Math.max(0, Duration.between(lastSeen, now).toMinutes());
            long hours = minutes / 60;
            if (hours > 0) {
                fullness = clamp(fullness - (int) (hours * FULLNESS_DECAY_PER_HOUR));
                happiness = clamp(happiness - (int) (hours * HAPPINESS_DECAY_PER_HOUR));
                energy = clamp(energy - (int) (hours * ENERGY_DECAY_PER_HOUR));
            }
            lastSeen = now;
            return hours;
        }

        String feed() {
            fullness = clamp(fullness + 25);
            happiness = clamp(happiness + 5);
            return name + " demolishes the fish. " + FISH;
        }

        String play() {
            if (energy < 10) {
                return name + " is too tired to chase the yarn. Maybe a nap first.";
            }
            happiness = clamp(happiness + 20);
            energy = clamp(energy - 15);
            fullness = clamp(fullness - 10);
            return name + " murders the yarn ball with great joy. " + YARN;
        }

        String nap() {
            energy = clamp(energy + 30);
            fullness = clamp(fullness - 5);
            return name + " naps in the sun patch. " + SLEEP;
        }

        String cuddle() {
            happiness = clamp(happiness + 10);
            return name + " headbutts your hand. " + HEART;
        }

        Mood mood() {
            int lowest = Math.min(fullness, Math.min(happiness, energy));
            if (lowest < 15) return Mood.MISERABLE;
            if (lowest >= 80) return Mood.THRILLED;
            if (lowest < 40) {
                if (lowest == fullness) return Mood.HUNGRY;
                if (lowest == energy) return Mood.SLEEPY;
                return Mood.GRUMPY;
            }
            return Mood.CONTENT;
        }

        String statusLine(Random random) {
            Mood mood = mood();
            return String.format("%s %s  |  food %d  play %d  rest %d%n   %s",
                    mood.face, name, fullness, happiness, energy, mood.line(random));
        }
    }

    /** Persistence: a plain properties file. Boring on purpose - it never breaks. */
    static class SaveStore {
        private final Path file;

        SaveStore(Path file) {
            this.file = file;
        }

        Pet load() {
            if (!Files.exists(file)) return null;
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
                return new Pet(
                        props.getProperty("name", "Cat"),
                        Integer.parseInt(props.getProperty("fullness", "80")),
                        Integer.parseInt(props.getProperty("happiness", "80")),
                        Integer.parseInt(props.getProperty("energy", "80")),
                        Instant.ofEpochMilli(Long.parseLong(props.getProperty("lastSeen", "0"))));
            } catch (IOException | NumberFormatException corrupt) {
                return null; // a fresh cat, not a crash
            }
        }

        void save(Pet pet) {
            Properties props = new Properties();
            props.setProperty("name", pet.name);
            props.setProperty("fullness", String.valueOf(pet.fullness));
            props.setProperty("happiness", String.valueOf(pet.happiness));
            props.setProperty("energy", String.valueOf(pet.energy));
            props.setProperty("lastSeen", String.valueOf(pet.lastSeen.toEpochMilli()));
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "terminal-pet save");
            } catch (IOException e) {
                System.out.println("(Could not write the save file: " + e.getMessage() + ")");
            }
        }
    }

    static final class Game {
        private final Pet pet;
        private final SaveStore store;
        private final Random random = new Random();

        Game(Pet pet, SaveStore store) {
            this.pet = pet;
            this.store = store;
        }

        String act(String command) {
            switch (command.trim().toLowerCase()) {
                case "feed": return pet.feed();
                case "play": return pet.play();
                case "nap": return pet.nap();
                case "cuddle": return pet.cuddle();
                case "status": return pet.statusLine(random);
                default: return pet.name + " tilts its head. Try feed, play, nap, cuddle, status, quit.";
            }
        }

        void loop(java.util.Scanner scanner) {
            System.out.println();
            System.out.println(pet.statusLine(random));
            System.out.println("Commands: feed | play | nap | cuddle | status | quit");
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine();
                if (line.trim().equalsIgnoreCase("quit")) break;
                System.out.println(act(line));
            }
            store.save(pet);
            System.out.println(pet.name + " watches you go. Saved. " + WAVE);
        }
    }

    static Pet adopt(SaveStore store, String name) {
        Pet pet = new Pet(name);
        store.save(pet);
        System.out.println(SPARKLES + " A small cat pads out of the terminal and into your life.");
        System.out.println("   Say hello to " + name + ".");
        return pet;
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--self-test")) {
            SelfTest.run();
            return;
        }
        SaveStore store = new SaveStore(Paths.get("pet.save"));
        Pet pet = store.load();
        if (pet == null) {
            if (args.length > 0 && args[0].equals("--demo")) {
                pet = adopt(store, "Pixel");
            } else {
                System.out.print("Name your new cat: ");
                java.util.Scanner naming = new java.util.Scanner(System.in);
                String name = naming.hasNextLine() ? naming.nextLine().trim() : "";
                pet = adopt(store, name.isEmpty() ? "Pixel" : name);
            }
        } else {
            long hours = pet.applyDecay(Instant.now());
            if (hours >= 6) {
                System.out.println(pet.name + " missed you. (" + hours + "h away)");
            }
        }
        if (args.length > 0 && args[0].equals("--demo")) {
            Game game = new Game(pet, store);
            System.out.println(pet.statusLine(new Random(42)));
            for (String command : new String[]{"play", "feed", "cuddle", "nap", "status"}) {
                System.out.println("> " + command);
                System.out.println(game.act(command));
            }
            store.save(pet);
            return;
        }
        new Game(pet, store).loop(new java.util.Scanner(System.in));
    }

    /** Zero-dependency checks, living in the same file. Run with --self-test. */
    static final class SelfTest {
        static int passed = 0;

        static void check(String name, boolean condition) {
            if (!condition) throw new AssertionError("FAILED: " + name);
            passed++;
            System.out.println("ok - " + name);
        }

        static void run() {
            Path temp = Paths.get(System.getProperty("java.io.tmpdir"), "terminal-pet-test.save");
            SaveStore store = new SaveStore(temp);

            Pet pet = new Pet("Test");
            check("new cat starts content or thrilled", pet.mood() == Mood.CONTENT || pet.mood() == Mood.THRILLED);

            pet.fullness = 90;
            pet.feed();
            check("feeding clamps fullness at 100", pet.fullness == 100);

            pet.applyDecay(pet.lastSeen.plus(Duration.ofHours(24)));
            check("24h away drains fullness to the floor", pet.fullness == 0);
            check("24h away is miserable", pet.mood() == Mood.MISERABLE);

            Pet tired = new Pet("Tired", 80, 80, 5, Instant.now());
            check("exhausted cat refuses to play", tired.play().contains("too tired"));

            Pet roundTrip = new Pet("Save", 42, 43, 44, Instant.now());
            store.save(roundTrip);
            Pet loaded = store.load();
            check("save and load round-trips every stat",
                    loaded != null && loaded.name.equals("Save")
                            && loaded.fullness == 42 && loaded.happiness == 43 && loaded.energy == 44);

            Pet hungry = new Pet("Hungry", 20, 90, 90, Instant.now());
            check("lowest stat picks the mood", hungry.mood() == Mood.HUNGRY);

            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            System.out.println(passed + " checks passed");
        }
    }
}
