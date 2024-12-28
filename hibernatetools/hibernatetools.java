///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.hibernate.tool:hibernate-tools-orm:7.0.0.Beta1
//DEPS info.picocli:picocli:4.7.6
////DEPS org.postgresql:postgresql:42.6.0
//JAVA 17+

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.internal.reveng.strategy.TableFilter;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "hibernatetools", mixinStandardHelpOptions = true, version = "Hibernate Tools CLI 0.1", 
description = """
    Hibernate Tools CLI

    This tool is used to generate entities from a database.
    Currently only supports JDBC metadata, but can be extended to support other metadata sources (i.e. existing JPA models) in the future.

    To run it is required you add the necessary JDBC dependencies to the classpath.

    Minimal example on how to run with jbang:
    jbang --deps org.postgresql:postgresql:42.6.0 hibernatetools@maxandersen \\
          --jdbc "jdbc:postgresql://localhost:5432/sakila" 

    Example with more options (commonly user and passwords are required):
    jbang --deps org.postgresql:postgresql:42.6.0 hibernatetools@maxandersen \\
        --jdbc "jdbc:postgresql://localhost:5432/sakila" \\
        --user sakila \\
        --password p_ssW0rd \\
        --schema public \\
        --table-exclude "payment_p.*" \\
        --table-exclude "nicer.*" \\
        --table-exclude "sales_by.*"
        --output src/main/java
    """)
public class hibernatetools implements Runnable {

    @Option(names = {"--jdbc"}, description = "JDBC URL", required = true)
    Optional<String> jdbcUrl;

    @Option(names = {"--user"}, description = "JDBC User")
    Optional<String> user;

    @Option(names = {"--password"}, description = "JDBC Password")
    Optional<String> password;

    @Option(names = {"--schema"}, description = "JDBC Schema to use, default to all schemas available")
    Optional<String> schema;

    @Option(names = {"-D"}, description = "Additional properties (-Dkey=value)", mapFallbackValue = "")
    private Map<String,String> properties = new HashMap<>();

    @Option(names = {"--table-include"}, description = "Table names to match, [<catalog>:][<schema>:]<table>", converter = IncludeTableFilterConverter.class)
    List<TableFilter> tableInclude = new ArrayList<>();

    @Option(names = {"--table-exclude"}, description = "Table names to exclude, [<catalog>:][<schema>:]<table>", converter = ExcludeTableFilterConverter.class)
    List<TableFilter> tableExclude = new ArrayList<>();

    @Option(names = {"--output"}, description = "Output directory, default to src", defaultValue = "src")
    Path output;

    @Override
    public void run() {
        
        //setup reverse engineering strategy (only relevant for jdbc reveng)
        RevengStrategy strategy = new DefaultStrategy();
     
         var overrides = new OverrideRepository();
         File revengxml = Path.of("hibernate.reveng.xml").toFile();
         if(revengxml.exists()) {
             overrides.addFile(revengxml);
         }

         tableExclude.forEach(t -> overrides.addTableFilter(t));
         tableInclude.forEach(t -> overrides.addTableFilter(t));
         strategy = overrides.getReverseEngineeringStrategy(strategy);

         Properties properties = new Properties();
         // properties.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
          
         jdbcUrl.ifPresent(url -> properties.setProperty("hibernate.connection.url", url));
         user.ifPresent(u -> properties.setProperty("hibernate.connection.username", u));
         password.ifPresent(p -> properties.setProperty("hibernate.connection.password", p));
         schema.ifPresent(s -> properties.setProperty("hibernate.default_schema", s));

         //TODO: allow other than just jdbc reveng as source for metadata
         var metadata = MetadataDescriptorFactory
                     .createReverseEngineeringDescriptor(
                         strategy,
                         properties
                     );
     
         var md =metadata.createMetadata();
     
         md.getEntityBindings().forEach(e -> {
             System.out.println("Table: %s Class: %s".formatted(e.getTable().getName(), e.getClassName()));
         });
         
         //TODO: make list of exporters configurable
         configure(properties,metadata, ExporterFactory.createExporter(ExporterType.JAVA)).start();
     
        // configure(properties,metadata, ExporterFactory.createExporter(ExporterType.DOC)).start();
         
    }

    /** configure exporter with common shared properties */
    private Exporter configure(Properties properties, MetadataDescriptor metadata, Exporter exp) {
        exp.getProperties().putAll(properties);
        exp.getProperties().setProperty("ejb3", ""+true);
        exp.getProperties().setProperty("jdk5", ""+true);

        exp.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadata);
        exp.getProperties().put(ExporterConstants.DESTINATION_FOLDER, output.toFile());
        return exp;
    }
    public static void main(String... args) {
       System.exit(new CommandLine(new hibernatetools()).execute(args));
    }


    /** converts <catalog>:][<schema>:]<table> to TableFilter */
    static class TableFilterConverter implements CommandLine.ITypeConverter<TableFilter> {
        @Override
        public TableFilter convert(String value) {
            String[] parts = value.split(":");
            String catalog = ".*";
            String schema = ".*"; 
            String table = ".*";
            
            if (parts.length == 1) {
                table = parts[0];
            } else if (parts.length == 2) {
                schema = parts[0];
                table = parts[1];
            } else if (parts.length == 3) {
                catalog = parts[0];
                schema = parts[1];
                table = parts[2];
            }

            TableFilter filter = new TableFilter();
            filter.setMatchCatalog(catalog);
            filter.setMatchSchema(schema);
            filter.setMatchName(table);
            return filter;
        }
    }

    static class ExcludeTableFilterConverter extends TableFilterConverter {
        @Override
        public TableFilter convert(String value) {
            TableFilter filter = super.convert(value);
            filter.setExclude(true);
            return filter;
        }
    }

    static class IncludeTableFilterConverter extends TableFilterConverter {
        @Override
        public TableFilter convert(String value) {
            TableFilter filter = super.convert(value);
            filter.setExclude(false);
            return filter;
        }
    }
}
