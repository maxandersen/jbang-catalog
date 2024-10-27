//DEPS info.picocli:picocli:4.7.6

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "imgcat", mixinStandardHelpOptions = true, description = "Display images inline in the terminal.")
public class imgcat implements Callable<Integer> {

    @Option(names = {"-p", "--print"}, description = "Enable printing of filename or URL after each image")
    private boolean printFilename;

    @Option(names = {"-W", "--width"}, description = "Set image width to N character cells, pixels or percent")
    private String width;

    @Option(names = {"-H", "--height"}, description = "Set image height to N character cells, pixels or percent")
    private String height;

    @Option(names = {"-r", "--preserve-aspect-ratio"}, description = "Preserve aspect ratio when scaling image")
    private boolean preserveAspectRatio;

    @Parameters(arity = "1..*", paramLabel = "FILE", description = "Image file(s) to display")
    private File[] files;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new imgcat()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        for (File file : files) {
            if (file.exists() && file.isFile()) {
                String base64Content = encodeFileToBase64(file);
                printImage(file.getName(), base64Content);
                if (printFilename) {
                    System.out.println(file.getName());
                }
            } else {
                System.err.println("ERROR: " + file.getPath() + " does not exist or is not a file.");
                return 1;
            }
        }
        return 0;
    }

    private String encodeFileToBase64(File file) throws Exception {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(bytes);
            if (bytesRead != bytes.length) {
                throw new IOException("Could not read entire file");
            }
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private void printImage(String filename, String base64Content) {
        StringBuilder sb = new StringBuilder();
        if (isTmux()) {
            sb.append("\u001BPtmux;\u001B\u001B]");
        } else {
            sb.append("\u001B]");
        }

        sb.append("1337;File=inline=1");
        sb.append(";size=").append(base64Content.length());
        if (filename != null && !filename.isEmpty()) {
            String encodedFilename = Base64.getEncoder().encodeToString(filename.getBytes(StandardCharsets.UTF_8));
            sb.append(";name=").append(encodedFilename);
        }
        if (width != null) {
            sb.append(";width=").append(width);
        }
        if (height != null) {
            sb.append(";height=").append(height);
        }
        if (preserveAspectRatio) {
            sb.append(";preserveAspectRatio=1");
        }
        sb.append(":").append(base64Content);

        if (isTmux()) {
            sb.append("\u0007\u001B\\");
        } else {
            sb.append("\u0007");
        }

        System.out.print(sb.toString());
        System.out.flush();
    }

    private boolean isTmux() {
        String term = System.getenv("TERM");
        return term != null && (term.startsWith("screen") || term.startsWith("tmux"));
    }
}