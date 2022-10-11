package aws.devopsguru.partner.opsgenie;

import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpsGenie implements RequestHandler<Map<String, Object>, String> {
	
	enum useCase {
		ALLEVENTS,
		ANOMALIES,
		RECOMMENDATIONS,
		SEVERITYUPGRADED,
		PROACTIVEANDHIGHONLY,
		PROACTIVEANDMEDIUMONLY,
		PROACTIVEANDLOWONLY,
		REACTIVEANDHIGHONLY,
		REACTIVEANDMEDIUMONLY,
		REACTIVEANDLOWONLY,
	}
	
	public String handleRequest(Map<String, Object> event, Context context) {
		
		// Take the object and make a string representation of it
		ObjectMapper objectMapper = new ObjectMapper();
		String temp = "";
		try {
			temp = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			Constants.getLogger().error("Event json passed is incorrect or empty due to: \n", e);
			e.printStackTrace();
		}
		
		// Make a json out of that string
		JsonNode jsonNode = null;
		try {
			jsonNode = objectMapper.readTree(temp);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			Constants.getLogger().error("Failed to make json out of string passed from objectMapper due to: \n", e);
			e.printStackTrace();
		}

		// Change the enum value here for the desired use case. Enum values are listed above.
		// Example: I want only Recommendations so i change the value to useCase.RECOMMENDATIONS
		useCase caseChoice = useCase.ALLEVENTS;
		pickUseCase(caseChoice, jsonNode);
		
		return null;
	}

	public void pickUseCase(useCase caseChoice, JsonNode jsonNode)
	{
		switch(caseChoice)
		{
			case ALLEVENTS:
				AlertType.allFeatures(jsonNode);
				break;
			case ANOMALIES:
				AlertType.InsightOpenAndAnomalies(jsonNode);
				break;
			case RECOMMENDATIONS:
				AlertType.InsightOpenAndRecommendations(jsonNode);
				break;
			case SEVERITYUPGRADED:
				AlertType.InsightOpenAndSeverityUpgraded(jsonNode);
				break;
			case PROACTIVEANDHIGHONLY:
				AlertType.proactiveHighSeverityOnly(jsonNode);
				break;
			case PROACTIVEANDMEDIUMONLY:
				AlertType.proactiveMediumSeverityOnly(jsonNode);
				break;
			case PROACTIVEANDLOWONLY:
				AlertType.proactiveLowSeverityOnly(jsonNode);
				break;
			case REACTIVEANDHIGHONLY:
				AlertType.reactiveHighSeverityOnly(jsonNode);
				break;
			case REACTIVEANDMEDIUMONLY:
				AlertType.reactiveMediumSeverityOnly(jsonNode);
				break;
			case REACTIVEANDLOWONLY:
				AlertType.reactiveLowSeverityOnly(jsonNode);
				break;
		}
	}
}