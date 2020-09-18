//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.awt.Desktop;
import java.util.concurrent.Callable;

@Command(name = "ghview", mixinStandardHelpOptions = true, version = "ghview 0.1",
        description = "ghview made with jbang")
class ghview implements Callable<Integer> {

    @Parameters(index = "0", description = "The thing to open")
    private String resource;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ghview()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        Desktop.getDesktop().browse(new java.net.URI("https://github.com/jbangdev/jbang/issues/" + resource.replace("#","")));
        return 0;
    }
}
