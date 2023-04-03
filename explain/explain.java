///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.0.0.CR1}@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-rest-client-reactive-jackson
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//SOURCES GPT.java GPTResponse.java

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.quarkus.runtime.Quarkus;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command
public class explain implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The source file to explain")
    Path sourceFile;

	@Option(names = { "-t", "--token" }, description = "The OpenAI API token", required = true, defaultValue = "${OPENAI_API_KEY}")
	String token;

	@Option(names = { "-m", "--model" }, description = "The OpenAI model to use", required = true, defaultValue = "gtp-3.5-turbo")
	String model;

	@Option(names = { "-T", "--temperature" }, description = "The temperature to use", required = true, defaultValue = "0.8")
	double temperature;

    @Override
    public void run() {

		System.out.println("Explaining " + sourceFile + " with model " + model + " and temperature " + temperature);
		
		GPT gpt = RestClientBuilder.newBuilder().baseUri(URI.create("https://api.openai.com")).build(GPT.class);
		
		final List<Map<String, String>> messages = new ArrayList<>();
			messages.add(prompt("system", "You are to advise a software developer on what the following code found in a file at " + sourceFile + " does."));
		try {
			messages.add(prompt("user", Files.readAllLines(sourceFile).stream().collect(Collectors.joining("\n"))));
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + sourceFile, e);	// TODO Auto-generated catch block
		}
		
		System.out.println(gpt.completions(token, model, temperature, messages));
    }

	Map<String, String> prompt(String role, String content) {
		Map<String, String> m = new HashMap<>();
		m.put("role", role);
		m.put("content", content);
		return m;
	}

	//public static void main(String[] args) {
	//	Quarkus.run();
	//}
}

