//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.13.0
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-properties:2.13.0
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.13.0
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.0
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.0
//DEPS com.fasterxml.woodstox:woodstox-core:6.2.5

import com.fasterxml.jackson.databind.SerializationFeature;

com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
com.fasterxml.jackson.databind.ObjectMapper toml = new com.fasterxml.jackson.dataformat.toml.TomlMapper().enable(SerializationFeature.INDENT_OUTPUT);
com.fasterxml.jackson.databind.ObjectMapper csv = new com.fasterxml.jackson.dataformat.csv.CsvMapper().enable(SerializationFeature.INDENT_OUTPUT);
com.fasterxml.jackson.databind.ObjectMapper yaml = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper().enable(SerializationFeature.INDENT_OUTPUT);
com.fasterxml.jackson.databind.ObjectMapper javaprop = new com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper().enable(SerializationFeature.INDENT_OUTPUT);
com.fasterxml.jackson.databind.ObjectMapper xml = new com.fasterxml.jackson.dataformat.xml.XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);


