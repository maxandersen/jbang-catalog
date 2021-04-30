
//DEPS org.testcontainers:testcontainers:1.15.3
//DEPS org.slf4j:slf4j-simple:1.7.25
//FILES Dockerfile

import java.nio.file.Paths;

import java.awt.Desktop;
import java.net.URI;

import org.testcontainers.containers.*;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class dashbuilder {
    
    public static void main(String[] args) throws Exception {
        GenericContainer dashbuilder = new GenericContainer(
    new ImageFromDockerfile("dashbuilder-dev", false)
            .withFileFromClasspath("Dockerfile", "Dockerfile"))
            .withFileSystemBind(Paths.get("./dashboards").toAbsolutePath().toString(),
                                 "/tmp/dashbuilder/models", BindMode.READ_WRITE)
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/"));
            
        dashbuilder.start();
        
        int port =  dashbuilder.getMappedPort(8080);

        String url = "http://localhost:"+port;
        System.out.println("Opening browser at "+url);
        Desktop.getDesktop().browse(new URI(url));

        System.out.println("Press ENTER to exit");
        System.in.read();

    };
}
