package aws.devopsguru.partner.opsgenie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ifountain.opsgenie.client.OpsGenieClient;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;

public class Constants {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OpsGenie.class);
	private static AlertApi opsGenieClient = new OpsGenieClient().alertV2();
	private static String apiKey = System.getenv("API_KEY");
	private static String teamName = System.getenv("TEAM_NAME");
	private static String email = System.getenv("EMAIL");
	
	public static Logger getLogger()
	{
		return LOGGER;
	}
	
	public static AlertApi getOpsGenieClient()
	{
		// Null/Empty check for API_KEY and sets it to OpsGenieClient
		if (apiKey != null && !apiKey.trim().isEmpty()) {
			opsGenieClient.getApiClient().setApiKey(apiKey);
		} else {
			LOGGER.error("API_KEY has not been set properly check enviornment variable in Lambda");
		}
		return opsGenieClient;
	}
	
	public static String getTeamName()
	{
		return teamName;
	}
	
	public static String getEmail()
	{
		return email;
	}
}