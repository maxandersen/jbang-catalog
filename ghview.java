//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.eclipse.jgit:org.eclipse.jgit:5.9.0.202009080501-r
//DEPS org.slf4j:slf4j-nop:1.7.30

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

@Command(name = "ghview", mixinStandardHelpOptions = true, version = "ghview 0.1", description = "ghview made with jbang")
class ghview implements Callable<Integer> {

    @Parameters(description = "The thing to open", arity = "1..N")
    private List<String> resource;

    @Option(names = "-d", description = "Directory context", defaultValue = ".")
    private File dir;

    public static void main(String... args) {
        System.err.println("args:");
        for (String string : args) {
            System.err.println(string);
        }
        int exitCode = new CommandLine(new ghview()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setWorkTree(dir).readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                // .setMustExist(true)
                .build();

        String url = repository.getConfig().getString("remote", "origin", "url");

        if(url==null) {
            System.err.println("Could not find git repo in " + dir);
            return ExitCode.USAGE;
        }
        url = url.replaceFirst("^https://github.com/(.*)/(.*).git$", "https://github.com/$1/$2");

        for (String res : resource) {
            try {
                Integer.parseInt(res);
                java.net.URI uri = new java.net.URI(url + "/issues/" + res.replace("#", ""));

                    System.err.println("open: " + uri);

                Desktop.getDesktop().browse(uri);

            } catch (NumberFormatException nfe) {
                // bad number skip
            }

        }
        return 0;
    }
}
