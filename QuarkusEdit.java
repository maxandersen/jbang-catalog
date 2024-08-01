
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.6
//DEPS dev.jbang:jbang-cli:RELEASE

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import dev.jbang.cli.Edit;
import dev.jbang.cli.ExitException;
import dev.jbang.net.*;
import dev.jbang.util.CommandBuffer;
import dev.jbang.util.Util;
import dev.jbang.util.Util.Shell;

import static dev.jbang.util.Util.infoMsg;
import static dev.jbang.util.Util.pathToString;
import static dev.jbang.util.Util.verboseMsg;

/** 
 * most of this code copied from jbang. intent is to make this available in a reusable utility API
 */
@Command(name = "QuarkusEdit", mixinStandardHelpOptions = true, version = "QuarkusEdit 0.1",
        description = "Experimental Quarkus Edit that download and setup vscode IDE for you with proper Quarkus/Java features installed")
class QuarkusEdit implements Callable<Integer> {

        @CommandLine.Option(names = {
        "--editor", "-e" }, arity = "0..1")
        public Optional<String> editor;

        @CommandLine.Parameters(index = "0", arity = "0..N")
        List<Path> additionalFiles = new ArrayList<>();

 private boolean openEditor(String projectPathString, List<String> additionalFiles) throws IOException {
        if (!editor.isPresent() || editor.get().isEmpty()) {
            editor = askEditor();
            if (!editor.isPresent()) {
                return false;
            }
        } else {
            showStartingMsg(editor.get(), false);
        }
        if ("gitpod".equals(editor.get()) && System.getenv("GITPOD_WORKSPACE_URL") != null) {
            infoMsg("Open this url to edit the project in your gitpod session:\n\n"
                    + System.getenv("GITPOD_WORKSPACE_URL") + "#" + projectPathString + "\n\n");
        } else {
            List<String> optionList = new ArrayList<>();
            optionList.add(editor.get());
            optionList.add(projectPathString);
            optionList.addAll(additionalFiles);

            String[] cmd;
            if (Util.getShell() == Shell.bash) {
                final String editorCommand = CommandBuffer.of(optionList).asCommandLine(Shell.bash);
                cmd = new String[] { "sh", "-c", editorCommand };
            } else {
                final String editorCommand = CommandBuffer.of(optionList).asCommandLine(Shell.cmd);
                cmd = new String[] { "cmd", "/c", editorCommand };
            }
            verboseMsg("Running `" + String.join(" ", cmd) + "`");
            new ProcessBuilder(cmd).start();
        }
        return true;
    }

    static String[] knownEditors = { "codium", "code", "cursor", "eclipse", "idea", "netbeans" };

     private static List<String> findEditorsOnPath() {
        return Arrays.stream(knownEditors).filter(e -> Util.searchPath(e) != null).collect(Collectors.toList());
    }


   
    private static Optional<String> askEditor() throws IOException {
        Path editorBinPath = EditorManager.getVSCodiumBinPath();
        Path dataPath = EditorManager.getVSCodiumDataPath();

        if (!Files.exists(editorBinPath)) {
            String question = "You requested to open default editor but no default editor configured.\n" +
                    "\n" +
                    "Quarkus can download and configure a visual studio code (VSCodium) with Java support to use\n" +
                    "See https://vscodium.com for details\n" +
                    "\n" +
                    "Do you want to";

            List<String> options = new ArrayList<>();
            options.add("Download and run VSCodium");

            List<String> pathEditors = findEditorsOnPath();
            for (String ed : pathEditors) {
                options.add("Use '" + ed + "'");
            }

            int result = Util.askInput(question, 30, 0, options.toArray(new String[] {}));
            if (result == 0) {
                return Optional.empty();
            } else if (result == 1) {
                setupEditor(editorBinPath, dataPath);
            } else if (result > 1) {
                String ed = pathEditors.get(result - 2);
                showStartingMsg(ed, true);
                return Optional.of(ed);
            } else {
                throw new ExitException(22,
                        "No default editor configured and no other option accepted.\n Please try again making a correct choice or use an explicit editor, i.e. `jbang edit --open=eclipse xyz.java`");
            }
        }

        return Optional.of(editorBinPath.toAbsolutePath().toString());
    }
    
    private static void showStartingMsg(String ed, boolean showConfig) {
        String msg = "Starting '" + ed + "'.";
    //    if (showConfig) {
    //        msg += "If you want to make this the default, run 'jbang config set edit.open " + ed + "'";
    //    }
        Util.infoMsg(msg);
    }

    // copied from jbang until its available via proper api/dependency
    private static void setupEditor(Path editorBinPath, Path dataPath) throws IOException {
        EditorManager.downloadAndInstallEditor();

        if (!Files.exists(dataPath)) {
            verboseMsg("Making portable data path " + dataPath.toString());
            Files.createDirectories(dataPath);
        }

        Path settingsjson = dataPath.resolve("user-data/User/settings.json");

        if (!Files.exists(settingsjson)) {
            verboseMsg("Setting up some good default settings at " + settingsjson);
            Files.createDirectories(settingsjson.getParent());

            String vscodeSettings = "{\n" +
            // better than breadcrumbs
                    "    \"editor.experimental.stickyScroll.enabled\": true,\n" +
                    // autosave because vscode has it default and it just makes things work smoother
                    "    \"files.autoSave\": \"onFocusChange\",\n" +
                    // use modern java
                    "    \"java.codeGeneration.hashCodeEquals.useJava7Objects\": true,\n" +
                    // instead of `out.println(x);` you get
                    // `out.println(argClosestWithMatchingType)`
                    "    \"java.completion.guessMethodArguments\": true,\n" +
                    // when editing html/xml editing tags updates the matching pair
                    "    \"editor.linkedEditing\": true,\n" +
                    // looks cooler - doesn't hurt
                    "    \"editor.cursorBlinking\": \"phase\",\n" +
                    // making easy to zoom for presentations
                    "    \"editor.mouseWheelZoom\": true\n" +
                    "}";
            Util.writeString(settingsjson, vscodeSettings);
        }

        verboseMsg("Installing Java + Quarkus extensions...");
        ProcessBuilder pb = new ProcessBuilder(editorBinPath.toAbsolutePath().toString(),
                "--install-extension", "redhat.java",
                "--install-extension", "vscjava.vscode-java-debug",
                "--install-extension", "vscjava.vscode-java-test",
                "--install-extension", "vscjava.vscode-java-dependency",
                "--install-extension", "jbangdev.jbang-vscode",
                "--install-extension", "redhat.vscode-quarkus");

        pb.inheritIO();
        Process process = pb.start();
        try {
            int exit = process.waitFor();
            if (exit > 0) {
                throw new ExitException(22,
                                   "Could not install and setup extensions into VSCodium. Aborting.");
            }
        } catch (InterruptedException e) {
            Util.errorMsg("Problems installing VSCodium extensions", e);
        }
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new QuarkusEdit()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
            Path path = null;
            Path location = null;
            if(additionalFiles.size() > 0) {
                location = additionalFiles.get(0);
            }

            if (location == null) {
                path = Edit.locateProjectDir(Paths.get("."));
            } else {
                path = Edit.locateProjectDir(location);
            }
            if (path != null && ((location != null) && !path.equals(location))) {
                additionalFiles.add(0, location);
            }

            openEditor(pathToString(path), additionalFiles.stream().map(p -> pathToString(p)).collect(Collectors.toList()));
        return 0;
    }
}
