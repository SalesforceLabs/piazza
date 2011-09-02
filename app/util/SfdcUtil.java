package util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import models.User;

import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import play.Logger;
import play.jobs.Job;
import play.libs.Crypto;
import play.libs.WS;
import play.libs.WS.WSRequest;
import util.gson.SfdcOAuthResponse;
import util.gson.SfdcRecord;
import util.gson.SfdcRestResponse;
import controllers.RequiresLogin;
import controllers.SfdcAuthentication;

public class SfdcUtil {
    public final static String QUERY_REST_ENDPOINT = "/services/data/v22.0/query/";
    public final static String LEAD_REST_ENDPOINT = "/services/data/v22.0/sobjects/Lead/"; // we can do upsert later
    public final static String TASK_REST_ENDPOINT = "/services/data/v22.0/sobjects/Task/"; // we can do upsert later

    /**
     * Refreshes token and updates current user
     */
    public static void refreshToken() {
        // new SfdcTokenRefresher(RequiresLogin.getActiveUser().id).now();
    }
    
    private static class SfdcTokenRefresher extends Job {
        private final String username;
        
        public SfdcTokenRefresher(String username) {
            this.username = username;
        }
        
        @Override
        public void doJob() {
            User user = User.get(this.username);
            Logger.info("Starting: SFDC token refresh job: %s", user.id);
	    	if (user.hasSfdcToken()) {
	    		long oneHourAgo = new DateTime().minusHours(1).getMillis();
	    		long issuedAt = user.sfdcIssuedAt;
	    		if (issuedAt < oneHourAgo) {
	    		    Logger.info("Refreshing SFDC token for user %s", user.id);
	    			SfdcOAuthResponse sfdcOAuthResponse;
	    			try {
	    				sfdcOAuthResponse = SfdcAuthentication.OAUTH.refreshSfdcAccessToken(user.sfdcRefreshToken);
	    				// setSfdcInfo is a bit heavy, but lets use it anyway because I am lazy
	    				user.setSfdcInfo(sfdcOAuthResponse);
	    			}
	    			catch (Exception e) {
	    				Logger.warn(e, "SFDC session expired, or something else happened");
	    				user.clearSfdcInfo();
	    			}
	    			user.save();
	    		}
	    	}
            Logger.info("Finished: SFDC token refresh: %s", user.id);
        }
    }

    public static Date parseDate(String dateStr) {
        Date d;
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
            d = df.parse(dateStr);
        } catch (ParseException e) {
            Logger.warn("Cannot parse date string: %s", dateStr);
            d = null;
        }
        return d;
    }

    public static void setToken(WSRequest request, String accessToken) {
        request.headers.put("Authorization", "OAuth " + accessToken);
    }

    public static List<SfdcRecord> getRecords(User user, String query) {
    	WSRequest request = WS.url(user.sfdcInstanceUrl + SfdcUtil.QUERY_REST_ENDPOINT);
    	setToken(request, Crypto.decryptAES(user.sfdcAccessToken));
    	request.parameters.put("q", query);
    	JsonElement jsonElement = request.get().getJson();
    	Logger.info("Read from SFDC. Query: '%s' Result: %s", query, jsonElement);
    	SfdcRestResponse restResponse = new Gson().fromJson(jsonElement, SfdcRestResponse.class);
    	return restResponse.records;
    }
}
