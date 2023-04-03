import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.jboss.resteasy.reactive.RestForm;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/v1/chat")
public interface GPT  {

	@POST
	@Path("completions")
	@ClientHeaderParam(name="Authorization", value="Bearer {token}")
	GPTResponse completions(String token, @RestForm String model, @RestForm double temperature, @RestForm List<Map<String, String>> messages);
}