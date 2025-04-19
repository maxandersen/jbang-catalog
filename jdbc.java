///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.7
//DESC This script launches sqlline or h2 console by just specifying jdbc url.
//DESC `jdbc` will use jbang to download the required driver and launch 
//DESC sqlline (cli) or h2 console (web) or jdbcnav (desktop).
//JAVA 11+ 

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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

    static class LaunchType {
        @Option(names = { "-w", "--web"}, description = "Launch h2 web console")
        boolean web;

        @Option(names = { "-d", "--desktop"}, description = "Launch jdbcnav desktop app")
        boolean desktop;
    }

    @ArgGroup(exclusive = true)
    LaunchType launchType = new LaunchType();

    @Option(names = { "-u", "--user" }, description = "User to connect to database")
    String user;

    @Option(names = { "-p", "--password" }, description = "Password to connect to database")
    String password;

    @Option(names = { "--dg", "--driver-gav" }, description = "List of GAV Driver to use", converter = HeaderConverter.class)
    List<String> driverGAV = new ArrayList<>();

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

        driverDependency = driverGAV.isEmpty() ? drivers.get(scheme) : driverGAV;
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
        if(launchType.web) {
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
        } else if(launchType.desktop) {
            command.add("https://thomasokken.com/jdbcnav/download/jdbcnav.jar");
            if(user != null) {
                command.add("-user=%s".formatted(user));
            }
            if(password != null) {
                command.add("-pass=%s".formatted(password));
            }
           // command.add("-name=%s".formatted(jdbcuri.getHost()));
           // command.add("-driver=%s".formatted(setDriverClasses().get(scheme)));
            command.add("-url=%s".formatted(jdbcurl));
            
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
        drivers.put("postgres", List.of("org.postgresql:postgresql:RELEASE"));
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

    private Map<String, String> setDriverClasses() {
        Map<String, String> drivers = new HashMap<>();
        //https://mariadb.com/kb/en/mariadb-connector-j/
        drivers.put("mariadb", "org.mariadb.jdbc.Driver");
        //https://dev.mysql.com/doc/connector-j/8.0/en/
        drivers.put("mysql", "com.mysql.cj.jdbc.Driver");
        //https://jdbc.postgresql.org/documentation/head/connect.html
        drivers.put("postgresql", "org.postgresql.Driver");
        drivers.put("postgres", "org.postgresql.Driver");
        //https://docs.oracle.com/en/database/oracle/oracle-database/19/jjdbc/JDBC-driver-connection-url-syntax.html#GUID-0A7E1701-2CEC-4608-A498-2D72AEB4013B
        drivers.put("oracle", "oracle.jdbc.OracleDriver");
        //https://docs.microsoft.com/en-us/sql/connect/jdbc/microsoft-jdbc-driver-for-sql-server?view=sql-server-ver15
        drivers.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        //https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/2.0.02/en-US/109397c2206a4ab2a5386d494f4cf75e.html
        drivers.put("sap", "com.sap.db.jdbc.Driver");
        //https://www.ibm.com/docs/en/informix-servers/14.10?topic=SSGU8G_14.1.0/com.ibm.jdbc_pg.doc/ids_jdbc_501.htm
        drivers.put("informix", "com.informix.jdbc.IfxDriver");
        //https://www.firebirdsql.org/file/documentation/drivers_documentation/java/3.0.7/firebird-classic-server.html
        drivers.put("firebird", "org.firebirdsql.jdbc.FBDriver");
        drivers.put("firebirdsql", "org.firebirdsql.jdbc.FBDriver");
        //https://hsqldb.org/doc/2.0/guide/dbproperties-chapt.html
        drivers.put("hsqldb", "org.hsqldb.jdbc.JDBCDriver");
        //https://www.h2database.com/html/features.html#database_url
        drivers.put("h2", "org.h2.Driver");
        //https://db.apache.org/derby/docs/10.8/devguide/cdevdvlp17453.html
        drivers.put("derby", "org.apache.derby.jdbc.EmbeddedDriver");
        drivers.put("sqlite", "org.sqlite.JDBC");
        return drivers;
    }

    static public class HeaderConverter implements CommandLine.ITypeConverter<List<String>> {
       
        @Override
        public List<String> convert(String s) throws Exception {
            var params = s.split(",");
            return Arrays.asList(params);
        }
    }
}
