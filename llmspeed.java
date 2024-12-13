///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//DESCRIPTION Calculate the speed of a Large Language Model using OpenAI model. The speed is measured in tokens per second based on the models own reported data.
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.3
//DEPS info.picocli:picocli:4.7.6

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "llmspeed", mixinStandardHelpOptions = true, 
                    description = """
                        Calculate the speed of a Large Language Model using OpenAI model. 
                        The speed is measured in tokens per second based on the models own reported data.
                        """)
public class llmspeed implements Runnable {

    @Option(names = {"-m", "--model"}, description = "Model to use (default: ${DEFAULT-VALUE})", defaultValue = "llama3.1")
    private String model;

    @Option(names = {"-u", "--url"}, description = "Base URL without /api/generate (default: ${DEFAULT-VALUE})", defaultValue = "http://localhost:11434")
    private String baseUrl;

    @Option(names = {"-p", "--prompt"}, description = "Prompt to use (default: ${DEFAULT-VALUE})", defaultValue = "Why is the sky blue?")
    private String prompt;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output for debugging")
    private boolean verbose;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new llmspeed()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        if (verbose) {
            System.out.println("Model: " + model);
            System.out.println("Base URL: " + baseUrl);
            System.out.println("Prompt: " + prompt);
        }
        try {
            Map<String, Object> responseData = sendRequest();
            double speed = calculateSpeed(responseData);
            
            //System.out.println("url: " + baseUrl); // dont leak possible open url endpoint
            System.out.println("model: " + model);
            System.out.println("tokens_per_second: " + speed);

        } catch(ConnectException ce) {
            System.err.println("Failed to connect to the server at " + baseUrl + ". Make sure the server is running and the URL is correct.");
            if (verbose) {
                ce.printStackTrace();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("An error occurred while processing the request.");
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, Object> sendRequest() throws IOException, InterruptedException {
        String url = baseUrl + "/api/generate";
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        payload.put("stream", false);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(jsonPayload))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (verbose) {
            System.out.println("Response body:\n " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
    }

    public static double calculateSpeed(Map<String, Object> responseData) {
        int evalCount = (int) responseData.getOrDefault("eval_count", 0);
        long evalDuration = (long) responseData.getOrDefault("eval_duration", 1L);  // Prevent division by zero

        // Convert nanoseconds to seconds for eval_duration
        double evalDurationSeconds = evalDuration / 1e9;

        // Calculate tokens per second
        return evalCount / evalDurationSeconds;
    }
}