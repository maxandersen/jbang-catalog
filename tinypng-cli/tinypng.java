///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.7
//DEPS com.tinify:tinify:RELEASE

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;


import com.tinify.Tinify;

@Command(name = "tinypng", mixinStandardHelpOptions = true, version = "tinypng 0.1",
        description = "tinypng made with jbang")
class tinypng implements Callable<Integer> {

    @Option(names = {"-t", "--token"}, description = "The tinypng api token", required = true)
    private String token;

    @Parameters(index = "0", arity = "1..*", description = "The files to compress")
    List<Path> files;

    @Option(names = {"-p", "--prefix"}, description = "The prefix for the output files", defaultValue = "tinypng-")
    String prefix = "tinypng-";
    public static void main(String... args) {
        int exitCode = new CommandLine(new tinypng()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
       
        Tinify.setKey(token);

        for (Path file : files) {
            Path target = null;
            if(prefix != null) {
                target = file.getParent() == null ? Path.of(prefix + file.getFileName()) : file.getParent().resolve(prefix + file.getFileName());
            } else {
                target = file;
            }
            long originalSize = file.toFile().length();
            System.out.println("Compressing " + file + " to " + target);
            Tinify.fromFile(file.toString()).toFile(target.toString());
            long compressedSize = target.toFile().length();
            double percentReduced = ((originalSize - compressedSize) / (double)originalSize) * 100;
            System.out.printf("Reduced by %.1f%% (from %d to %d bytes)%n", 
                percentReduced, originalSize, compressedSize);
        }

        return 0;
    }
}
