package aws.devopsguru.partner.opsgenie;

import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpsGenie implements RequestHandler<Map<String, Object>, String> {
	
	enum UseCase {
		ALLEVENTS,
		ALLEVENTSREACTIVE,
		ALLEVENTSPROACTIVE,
		ANOMALIES,
		RECOMMENDATIONS,
		SEVERITYUPGRADED,
		RECOMMENDATIONSANDANOMALIES,
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
		UseCase caseChoice = UseCase.ALLEVENTS;
		pickUseCase(caseChoice, jsonNode);
		
		return null;
	}

	public void pickUseCase(UseCase caseChoice, JsonNode jsonNode)
	{
		switch(caseChoice)
		{
			case ALLEVENTS:
				AlertType.allFeatures(jsonNode);
				break;
			case ALLEVENTSREACTIVE:
				AlertType.allFeaturesReactiveInsights(jsonNode);
				break;
			case ALLEVENTSPROACTIVE:
				AlertType.allFeaturesProactiveInsights(jsonNode);
				break;
			case ANOMALIES:
				AlertType.insightOpenAndAnomalies(jsonNode);
				break;
			case RECOMMENDATIONS:
				AlertType.insightOpenAndRecommendations(jsonNode);
				break;
			case SEVERITYUPGRADED:
				AlertType.insightOpenAndSeverityUpgraded(jsonNode);
				break;
			case RECOMMENDATIONSANDANOMALIES: 
				AlertType.insightOpenAnomaliesAndRecommendations(jsonNode);
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