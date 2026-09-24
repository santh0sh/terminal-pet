# terminal-pet

A tiny virtual cat that lives in your terminal. Feed it, play with it, come back tomorrow and it missed you. One Java file, no build, no dependencies.

```
✨ A small cat pads out of the terminal and into your life.
   Say hello to Pixel.
😻 Pixel  |  food 80  play 80  rest 80
   Life is good. You are here.
> play
Pixel murders the yarn ball with great joy. 🧶
> nap
Pixel naps in the sun patch. 💤
```

## Run it

You need Java 11 or newer. That is all.

```bash
java TerminalPet.java
```

First run asks you to name the cat. It saves to `pet.save` when you quit, and its stats decay in real time while you are away - leave it a day and it will be hungry, grumpy, or asleep when you return.

Commands: `feed` `play` `nap` `cuddle` `status` `quit`

Extras:

```bash
java TerminalPet.java --demo       # non-interactive tour
java TerminalPet.java --self-test  # 7 built-in checks, zero test dependencies
```

## Why one file

It is deliberately a single `.java` file launched straight from source (`java File.java`, Java 11+). No Maven, no Gradle, no folders - easy to read top to bottom, and easy to upload to GitHub from any device, including a phone.

Small engineering choices worth noticing:

- Emoji are written as `\uXXXX` escapes, so the source is plain ASCII and never breaks on a non-UTF-8 platform charset.
- Persistence is a `java.util.Properties` file - boring, dependency-free, and it never corrupts your cat.
- Mood is derived from the lowest stat, so the cat's face always tells you what it needs.
- The self-test lives inside the same file: `--self-test` runs real assertions (decay math, clamping, save/load round-trip, mood rules) without any test framework.

Windows note: use Windows Terminal (or any UTF-8 terminal) to see the emoji properly. Old cmd.exe may show `?` instead - the cat is still there.

## Upload steps from a phone (2 minutes)

1. Create a public repo named exactly `terminal-pet` on github.com (do not tick "Add a README")
2. Unzip this package in your Files app
3. On the repo page: Add file > Upload files > select all 4 files > Commit

Part of a public build series. Java 11+, MIT licensed.
