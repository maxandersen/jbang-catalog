///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.17.4}@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-rest-client-reactive-jackson
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//SOURCES GPT.java GPTResponse.java
//FILES application.properties

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import picocli.CommandLine.*;
import picocli.CommandLine.Option;

@Command(
	mixinStandardHelpOptions = true, headerHeading = "@|bold,underline Usage|@:%n%n",
	version = "0.1",
synopsisHeading = "%n",
descriptionHeading = "%n@|bold,underline Description|@:%n%n",
parameterListHeading = "%n@|bold,underline Parameters|@:%n",
optionListHeading = "%n@|bold,underline Options|@:%n",
header = "Explain usage of a source file using ChatGPT.",
description = "Queries ChatGPT to explain what a source file does in a Quarkus project.\n\n"+
"Note: Be aware the source code is sent to remote server.")
public class explain implements Runnable {

    @Parameters(index = "0", arity="1", description = "The source file to explain")
    Path sourceFile;

	@Option(names = { "-t", "--token" }, description = "The OpenAI API token. default: OPENAI_API_KEY", required = true, defaultValue = "${OPENAI_API_KEY}")
	String token;

	@Option(names = { "-m", "--model" }, 
			description = "The OpenAI model to use. Valid values: ${COMPLETION-CANDIDATES}", 
			required = true, 
			defaultValue = "gpt-3.5-turbo",
			completionCandidates = ModelCandidates.class)
	String model;

	@Option(names = { "-T", "--temperature" }, 
			description = "The temperature to use. Number between 0 and 1. 0=no variation 1=very creative/random.", 
			required = true, defaultValue = "0.8")
	double temperature;

	//@RestClient
	//GPT gpt;

    @Override
    public void run() {

		System.out.println("Requesting explanation of " + sourceFile + " with model " + model + " and temperature " + temperature + ". Have patience...");

		GPT gpt = RestClientBuilder.newBuilder().baseUri(URI.create("https://api.openai.com")).build(GPT.class);
		
		final List<Map<String, String>> messages = new ArrayList<>();
			messages.add(prompt("system", "You are to advise a software developer on what the following code found in a file at " + sourceFile + " does."));
		try {
			messages.add(prompt("user", Files.readAllLines(sourceFile).stream().collect(Collectors.joining("\n"))));
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + sourceFile, e);	
		}
		
	 var result = gpt.completions(token, Map.of("model", model, "temperature", temperature, "messages", messages));

	 System.out.println(result.choices.stream().map(m->m.message.content).collect(Collectors.joining()));
    }

	Map<String, String> prompt(String role, String content) {
		Map<String, String> m = new HashMap<>();
		m.put("role", role);
		m.put("content", content);
		return m;
	}

	static class ModelCandidates extends ArrayList<String> {
		ModelCandidates() { super(Arrays.asList("gpt-3.5-turbo", "gpt-4")); }
	}
}

