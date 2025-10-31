# logseq-tag-extractor

A small Kotlin CLI utility that scans Markdown files (e.g., from a Logseq graph) in the current directory and prints the lines that contain a given tag, together with their surrounding context (parents/children derived from indentation).

It supports common Logseq tag syntaxes such as `#tag`, `[[tag]]`.

## Stack
- Language: Kotlin (JVM)
- JDK: Java 21
- Build tool: Maven
- Logging: slf4j + Logback (configured via `src/main/resources/logback.xml`)
- Testing: JUnit 5, AssertJ

## Entry point
- Main class: `com.mg.logseq.extractor.LogseqTagExtractorKt`
- Source: `src/main/kotlin/com/mg/logseq/extractor/LogseqTagExtractor.kt`

## Requirements
- Java 21 (JDK)
- Maven 3.8+ (tested with recent Maven versions)

## Setup
Clone the repository and build it:

```bash
mvn -q -DskipTests package
```

This produces a shaded (fat) JAR in `target/` with the main class configured.

## Running
By default, the tool scans the current working directory for `*.md` files, sorts them by date (see “How files are sorted”), and for each file that contains matches for the tag prints a header with the file name and the matching lines/context.

### Usage
```bash
# From sources using Maven Exec (recommended during development)
mvn -q exec:java -Dexec.args="<tag>"

# From the shaded JAR
java -jar target/logseq-tag-extractor-0.1-SNAPSHOT.jar <tag>
```

Examples:
```bash
mvn -q exec:java -Dexec.args="tag-1"
java -jar target/logseq-tag-extractor-0.1-SNAPSHOT.jar "tag 1"
```

Output format (example):
```
>>>>>>>>>>>>>>>> 2025_01_02.md
# Title 0
  - subtitle 0.2
    - subcontent 0.2.1 [[tag 1]]
      - subcontent 0.2.1.1
>>>>>>>>>>>>>>>> 2025_01_03.md
# Title 1
  - subtitle 1.2
    - subcontent 1.2.1 [[tag 1]]
      - subcontent 1.2.1.1
      - subcontent 1.2.1.2
>>>>>>>>>>>>>>>> 2025_06_06.md
# Title 3 [[tag 1]]
  - content
```

### How files are sorted
Files are collected with a glob of `*.md` in the current directory and sorted by a LocalDate resolved as follows:
- If the filename starts with a date in `yyyy_MM_dd` (before the first `.md`), that date is used.
- Otherwise, the file’s last-modified timestamp is used, converted to the system default timezone.


## Scripts and useful Maven commands
- `mvn clean package` — build the shaded JAR
- `mvn exec:java -Dexec.args="<tag>"` — run from sources
- `mvn test` — run tests

No custom shell scripts are included in this repository.

## Environment variables
There are no required application-specific environment variables.

Optional (logging via Logback):
- `FILE_LOG_PATTERN` — overrides the file log pattern; used by `logback.xml`
- `LOG_BASE_NAME` — base name for log files (default: `logseq-tag-extractor`)

Logs:
- Console appender prints INFO+ level to stdout.
- Rolling file appender writes to `log/logseq-tag-extractor.log` with daily rotation.

## Tests
Run unit tests with:
```bash
mvn test
```

Example test: `LogseqTagExtractorTest` validates that extracting `"tag 1"` returns the expected lines and context.


## Limitations / Notes
- Input scope is limited to Markdown files (`*.md`) in the current working directory.
- Indentation is used to infer hierarchy. Lines starting with spaces are treated as 2-space indents; otherwise leading whitespace is counted literally.
- Tag detection supports `#tag`, `[[tag]]` patterns.
- Output groups results per file and prints only files with at least one match.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

