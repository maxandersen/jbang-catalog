///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.3


import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static java.lang.String.format;
import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;

@Command(name = "javajupyter", mixinStandardHelpOptions = true, version = "javajupyter 0.1",
        description = "javajupyter made with jbang", showDefaultValues=true)
class javajupyter implements Callable<Integer> {

    private static final String SEPARATOR = "/";
    private static final String CONNECTION_FILE_MARKER = "{connection_file}";
    private static final String LANGUAGE = "java";
    private static final String INTERRUPT_MODE = "message";

        @Option(names = "--name", defaultValue = "Java (JBang Rapaio)")
        String name;

        @Option(names = "--kernel-dir", defaultValue = "jbang-rapaio")
        String kernelDir;

        @Option(names="--timeout", defaultValue = "-1")
        long timeout;

        @Option(names="--compiler-options", defaultValue = "")
        String compilerOptions;

        private enum OSName {
                LINUX,
                WINDOWS,
                MAC,
                SOLARIS
            }
        
            private OSName findOSName() {
                String os = System.getProperty("os.name").toLowerCase();
        
                if (os.contains("win")) {
                    return OSName.WINDOWS;
                } else if (os.contains("mac")) {
                    return OSName.MAC;
                } else if (os.contains("sunos")) {
                    return OSName.SOLARIS;
                } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    return OSName.LINUX;
                }
                return null;
            }
            
            private String getUserHome() {
                String home = System.getProperty("user.home");
                if (home != null && home.endsWith("/")) {
                    home = home.substring(0, home.length() - 1);
                }
                return home;
            }

        private List<String> getInstallationPaths(OSName os) {

        return switch (os) {
            case LINUX, SOLARIS -> List.of(
                    getUserHome() + "/.local/share/jupyter/kernels",
                    "/usr/local/share/jupyter/kernels",
                    "/usr/share/jupyter/kernels"
            );
            case MAC -> List.of(
                    getUserHome() + "/Library/Jupyter/kernels",
                    "/usr/local/share/jupyter/kernels",
                    "/usr/share/jupyter/kernels"
            );
            case WINDOWS -> List.of(
                    System.getenv("APPDATA") + "/jupyter/kernels",
                    System.getenv("PROGRAMDATA") + "/jupyter/kernels"
            );
        };
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new javajupyter()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { 
        
        OSName os = findOSName();
        if (os == null) {
            throw new RuntimeException("Operating system is not recognized. Installation failed.");
        }

        List<String> installationPath = getInstallationPaths(findOSName());

        System.out.println("Considering " + String.join(",", installationPath));
        
        Path command = null;
        String[] paths = System.getenv("PATH").split(File.pathSeparator);
        for (String path : paths) {
            if (os.equals(OSName.WINDOWS)) {
                command = Path.of(path, "jbang.cmd");
                if (Files.exists(command) && Files.isExecutable(command)) {
                    break;
                } 
                command = Path.of(path, "jbang.ps1");
                if (Files.exists(command) && Files.isExecutable(command)) {
                    break;
                } 
            }
            command = Path.of(path, "jbang");
            if (Files.exists(command) && Files.isExecutable(command)) {
                break;
            }
            command = null;
        }
        if (command == null) {
            throw new IllegalStateException("jbang executable not found in PATH. Please ensure it is available before running javajupyter.");
        }
        KernelJson json = new KernelJson(List.of(command.toAbsolutePath().toString(), 
                                "--java", "21",
                                "-R--add-modules",
                                "-Rjava.base,jdk.incubator.vector",
                                "io.github.padreati:rapaio-jupyter-kernel:RELEASE",
                                CONNECTION_FILE_MARKER), 
                                name, 
                                LANGUAGE, 
                                INTERRUPT_MODE, 
                                Map.of(
                                    "RJK_COMPILER_OPTIONS",compilerOptions,
                                    "RJK_INIT_SCRIPT", "",
                                    "RJK_TIMEOUT_MILLIS", ""+timeout));


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        objectMapper.setDefaultPrettyPrinter(prettyPrinter);

        String jsonString = objectMapper.writeValueAsString(json);

        var output = Paths.get(installationPath.get(0), kernelDir, "kernel.json");

        out.println(format("Writing: %s\nto %s", jsonString, output));
        try {
            if (!Files.exists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }
            Files.write(output, jsonString.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        

        return 0;
    }

    

public record KernelJson(
         List<String> argv,
        String displayName,
         String language,
         String interruptMode,
        Map<String, String> env) {
}
}
