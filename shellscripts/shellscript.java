///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS org.slf4j:slf4j-simple:1.7.30
//JAVA 11+
//FILES demo.sh
//DESCRIPTION Simple example of how to wrap and run a shellscript using jbang catalogs.

import static java.lang.System.*;
import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.zeroturnaround.exec.*;

public class shellscript {

    // take the resource /shellscript.java and write it to a temporary file
    Path writeResourceToFile(String resource, String prefix) {
        try {
            Path tempFile = Files.createTempFile(prefix , "");
            try (InputStream in = getClass().getResourceAsStream(resource)) {
                copy(in, tempFile, REPLACE_EXISTING);
                var permissions = getPosixFilePermissions(tempFile);
                permissions.add(OWNER_EXECUTE);
                setPosixFilePermissions(tempFile, permissions);
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void run(List<String> arguments) throws Exception {
 
        //run the script using zt-exec and print standard out and error
        Path script = writeResourceToFile("demo.sh", "demo");
        arguments.add(0, script.toString());
        new ProcessExecutor()
                .command(arguments)
                .redirectOutput(out)
                .redirectError(err)
                .execute();
    }
    public static void main(String... args) throws Exception {
        new shellscript().run(new ArrayList<String>(Arrays.asList(args)));       
    }
}
