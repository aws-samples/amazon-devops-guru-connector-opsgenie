package aws.devopsguru.partner.opsgenie;

import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpsGenie implements RequestHandler<Map<String, Object>, String> {
	
	private static final Logger LOGGER = LogManager.getLogger(OpsGenie .class);

	public String handleRequest(Map<String, Object> event, Context context) {
		
		// Take the object and make a string representation of it
		ObjectMapper objectMapper = new ObjectMapper();
		String temp = "";
		try {
			temp = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Event json passed is incorrect or empty due to: \n", e);
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
			LOGGER.error("Failed to make json out of string passed from objectMapper due to: \n", e);
			e.printStackTrace();
		}
	
		AlertType.createAlert(jsonNode, LOGGER);
		
		return null;
	}

}