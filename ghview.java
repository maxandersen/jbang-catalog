//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.6
//DEPS org.eclipse.jgit:org.eclipse.jgit:5.9.0.202009080501-r
//DEPS org.slf4j:slf4j-nop:1.7.30

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

@Command(name = "ghview", mixinStandardHelpOptions = true, version = "ghview 0.1", description = "ghview made with jbang")
class ghview implements Callable<Integer> {

    @Parameters(description = "The thing to open", arity = "1..N")
    private List<String> resource;

    @Option(names = "-d", description = "Directory context", defaultValue = ".")
    private File dir;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ghview()).execute(args);
        System.exit(exitCode);
    }

    String getBaseUrl(String repo) throws IOException {

        if(repo==null) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setWorkTree(dir).readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    // .setMustExist(true)
                    .build();

            String url = repository.getConfig().getString("remote", "origin", "url");

            url = url.replaceFirst("^https://github.com/(.*)/(.*).git$", "https://github.com/$1/$2");
            return url;
        } else {
            return "https://github.com/" + repo;
        }
    }
    @Override
    public Integer call() throws Exception { // your business logic goes here...
       // System.out.println("opening " + resource);

        for (String res : resource) {

            Pattern p = Pattern.compile("(?<repo>[a-z0-9A-Z_.-]+/[a-z0-9A-Z_.-]+)?(?<issue>\\\\?#[0-9]+)");

            Matcher m = p.matcher(res);

            if(m.find()) {
                String url = getBaseUrl(m.group("repo"));
                if(m.group("issue")!=null) {
                    url = url + "/issues/" + m.group("issue").replace("#", "").replace("\\","");
                }
                URI uri = new URI(url);
                Desktop.getDesktop().browse(uri);
            }
        }
        return 0;
    }
}
