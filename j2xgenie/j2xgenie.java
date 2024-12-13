///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.17.4}@pom
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-openai:0.19.0.CR3
//DEPS io.quarkus:quarkus-picocli
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
// Q:CONFIG quarkus.log.category."io.quarkiverse.langchain4j".level=DEBUG
//Q:CONFIG quarkus.langchain4j.openai.api-key=${OPENAI_KEY}
//Q:CONFIG quarkus.langchain4j.openai.timeout=60s
//Q:CONFIG quarkus.langchain4j.openai.log-requests=true
//Q:CONFIG quarkus.langchain4j.openai.log-responses=true

//JAVAC_OPTIONS -parameters

import static java.lang.System.out;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "j2xgenie", description = "Generate code from (json) data using genAI.")
@ActivateRequestContext
public class j2xgenie implements Runnable {

    @Option(names = "--force", description = "Overwrite existing files", defaultValue = "false")
    boolean force;

    @Option(names = "--types", description = "The type of structure to generate, e.g., Java records, C# classes, etc.", defaultValue = "Java records")
    String typeOfStruct;

    @Option(names = "--outpath", description = "The path to output the generated files", defaultValue = "src")
    Path outputPath;

    @Parameters(index = "0", description = "The source data to generate code from, use '-' for stdin")
    String source;

    @RegisterAiService
    public interface jaidataAI {

        @SystemMessage("""
                    IDENTITY

                    You are a superintelligent AI that finds all mentions of data structures within an input and you
                    output properly formatted code that perfectly represents what's in the input.

                    STEPS

                    * Read the whole input and understand the context of everything.
                    * Find all mention of data structures, e.g., projects, teams, budgets, metrics, KPIs, etc., and think about the name of those fields and the data in each field.
                    * Generate {typeOfStruct}
                    * The code need to be complete, no "//Getters and Setters" or similar comments that tell users to do something. They should work as is generated.
                    * Make sure that if the programming language used requires the declaration to be in individual files it should be generated as such.
                    * Ensure the code is compilable.
                    * In the documentation comments include sample of the mapped data.

                """)
        @UserMessage("""
                    {data}
                """)
        FileInfos generate(String data, String typeOfStruct);
    }

    record FileInfos(List<FileInfo> files) {
    };

    record FileInfo(String filename, String content) {
    };

    @Inject
    jaidataAI ai;

    @Override
    public void run() {
        String input = null;

        if (source.equals("-")) {
            StringBuilder inputBuilder = new StringBuilder();
            try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    inputBuilder.append(scanner.nextLine()).append("\n");
                }
                input = inputBuilder.toString().trim();
            }
        } else if (source.startsWith("http://") || source.startsWith("https://")) {
            try {
                URL url = new URL(source);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    StringBuilder inputBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        inputBuilder.append(line).append("\n");
                    }
                    input = inputBuilder.toString().trim();
                }
            } catch (IOException e) {
                System.err.println("Error reading from URL: " + e.getMessage());
                return;
            }
        } else {
            Path inputPath = Paths.get(source);
            if (!exists(inputPath)) {
                System.err.println("Error: File does not exist: " + inputPath);
                return;
            }
            try {
                input = Files.readString(inputPath);
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if(input == null) {
            System.err.println("Error: No input provided");
            return;
        }

        var files = ai.generate(input, typeOfStruct);

        for (FileInfo file : files.files()) {
            try {
                Path filePath = outputPath.resolve(file.filename()).normalize();
                if (!filePath.startsWith(outputPath)) {
                    System.err.println("Error: File path resolves outside of output directory: %s".formatted(file.filename()));
                    continue;
                }
                createDirectories(filePath.getParent());
                if (!exists(filePath) || force) {
                    writeString(filePath, file.content());
                    out.println("Generated file: %s".formatted(filePath));
                } else {
                    out.println("File already exists, skipping: %s. Use --force to overwrite.".formatted(filePath));
                }
            } catch (IOException e) {
                System.err.println("Error writing file %s: %s".formatted(file.filename(), e.getMessage()));
            }
        }
    }

}
