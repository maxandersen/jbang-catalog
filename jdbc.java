///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DESC This script launches sqlline or h2 console by just specifying jdbc url.
//DESC `jdbc` will use jbang to download the required driver and launch sqlline or h2 console.

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

// jdbc urls: https://www.baeldung.com/java-jdbc-url-format
// maven drivers: https://vladmihalcea.com/jdbc-driver-maven-dependency/

@Command(name = "jdbc", mixinStandardHelpOptions = true, version = "jdbc 0.1",
        description = "Launch command to connect a jdbc database using sqlline or h2 web console\n\nExample:\njdbc jdbc:oracle:thin:@myoracle.db.server:1521:my_sid -- -n max\n")
class jdbc implements Callable<Integer> {

    @Parameters(index = "0", description = "JDBC url to connect to")
    private String jdbcurl;

    @Option(names = "-q", description = "Quiet/no launch. Just print command line")
    boolean quiet;

    @Parameters(index = "1..N", description = "Additional args to pass to sqlline or h2 web console")
    List<String> additionalArgs = List.of();

    @Option(names = "-w", description = "Launch h2 web console instead of sqlline")
    boolean web;

    @Option(names = { "-u", "--user" }, description = "User to connect to database")
    String user;

    @Option(names = { "-p", "--password" }, description = "Password to connect to database")
    String password;

    public static void main(String... args) {
        int exitCode = new CommandLine(new jdbc()).execute(args);
        System.exit(exitCode);
    }

    
    @Override
    public Integer call() throws Exception { // your business logic goes here...
        
        List<String> driverDependency;
        URI jdbcuri = null;
        if(!jdbcurl.startsWith("jdbc:")) {
            jdbcurl = "jdbc:" + jdbcurl;
        }  
        jdbcuri = URI.create(jdbcurl.substring("jdbc:".length()));

        String scheme = jdbcuri.getScheme();

        Map<String, List<String>> drivers = setupDrivers();

        driverDependency = drivers.get(scheme);
        if (driverDependency == null) {
            throw new IllegalArgumentException("Unsupported database type: " + scheme);
        }

    
        List<String> command = new ArrayList<>();

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            command.add("jbang.cmd");
        } else {
            command.add("jbang");
        }
        command.add("--deps");
        command.add(String.join(",",driverDependency));
        if(web) {
            command.add("h2@jbanghub/h2");
            if(user != null) {
                command.add("-user");
                command.add(user);
            }
            if(password != null) {
                command.add("-password");
                command.add(password);
            }
            command.add("-url");
            command.add(jdbcurl);
        } else {
            command.add("sqlline:sqlline:RELEASE");
            if(user != null) {
                command.add("-n");
                command.add(user);
            }
            if(password != null) {
                command.add("-p");
                command.add(password);
            }
            command.add("-u");
            command.add(jdbcurl);
        }
        

        if (!additionalArgs.isEmpty()) {
            command.addAll(additionalArgs);
        }

        System.out.println(String.join(" ", command));
        if(!quiet) {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        }
        return 0;
    }


    private Map<String, List<String>> setupDrivers() {
        Map<String, List<String>> drivers = new HashMap<>();
        //https://mariadb.com/kb/en/mariadb-connector-j/
        drivers.put("mariadb", List.of("org.mariadb.jdbc:mariadb-java-client:RELEASE"));
        //https://dev.mysql.com/doc/connector-j/8.0/en/
        drivers.put("mysql", List.of("mysql:mysql-connector-java:RELEASE"));
        //https://jdbc.postgresql.org/documentation/head/connect.html
        drivers.put("postgresql", List.of("org.postgresql:postgresql:RELEASE"));
        //https://docs.oracle.com/en/database/oracle/oracle-database/19/jjdbc/JDBC-driver-connection-url-syntax.html#GUID-0A7E1701-2CEC-4608-A498-2D72AEB4013B
        drivers.put("oracle", List.of("com.oracle.database.jdbc:ojdbc10:RELEASE"));
        //https://docs.microsoft.com/en-us/sql/connect/jdbc/microsoft-jdbc-driver-for-sql-server?view=sql-server-ver15
        drivers.put("sqlserver", List.of("com.microsoft.sqlserver:mssql-jdbc:RELEASE"));
        //https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/2.0.02/en-US/109397c2206a4ab2a5386d494f4cf75e.html
        drivers.put("sap", List.of("com.sapcloud.db.jdbc:ngdbc:RELEASE"));
        //https://www.ibm.com/docs/en/informix-servers/14.10?topic=SSGU8G_14.1.0/com.ibm.jdbc_pg.doc/ids_jdbc_501.htm
        drivers.put("informix", List.of("com.ibm.informix:jdbc:RELEASE"));
        //https://www.firebirdsql.org/file/documentation/drivers_documentation/java/3.0.7/firebird-classic-server.html
        drivers.put("firebird", List.of("org.firebirdsql.jdbc:jaybird:RELEASE"));
        drivers.put("firebirdsql", List.of("org.firebirdsql.jdbc:jaybird:RELEASE"));
        //https://hsqldb.org/doc/2.0/guide/dbproperties-chapt.html
        drivers.put("hsqldb", List.of("org.hsqldb:hsqldb:RELEASE"));
        //https://www.h2database.com/html/features.html#database_url
        drivers.put("h2", List.of("com.h2database:h2:RELEASE"));
        //https://db.apache.org/derby/docs/10.8/devguide/cdevdvlp17453.html
        drivers.put("derby", List.of("org.apache.derby:derby:RELEASE"));
        drivers.put("sqlite", List.of("org.xerial:sqlite-jdbc:RELEASE", "org.slf4j:slf4j-simple:1.7.36"));
        return drivers;
    }
}
