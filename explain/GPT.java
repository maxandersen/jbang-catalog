
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.ComputedParamContext;
import io.quarkus.rest.client.reactive.NotBody;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/v1/chat")
//@RegisterRestClient
public interface GPT  {

	@POST
	@Path("completions")
	@ClientHeaderParam(name="Authorization", value="{calctoken}")
	GPTResponse completions(@NotBody String token, Map<String, Object> params);

	default String calctoken(ComputedParamContext ctx) {
		return "Bearer " + ctx.methodParameters().get(0).value();
	}
	
}