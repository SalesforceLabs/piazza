/**
 * 
 */
package controllers;

import java.util.HashMap;
import java.util.Map;

import models.Person;
import models.User;

import org.joda.time.DateTime;

import play.Play;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;
import util.CheckHelper;
import util.SfdcOAuth;
import util.gson.SfdcCreateResponse;
import util.gson.SfdcOAuthResponse;
import util.gson.SfdcTask;


/**
 * @author marcus
 * <br/>
 * http://wiki.developerforce.com/index.php/Digging_Deeper_into_OAuth_2.0_on_Force.com
 */
@With(RequiresLogin.class)
public class SfdcAuthentication extends Controller {
	public final static SfdcOAuth OAUTH;
	private final static String REDIRECT_URI;
	static {
		String authorizationURL = "https://login.salesforce.com/services/oauth2/authorize";
		String accessTokenURL = "https://login.salesforce.com/services/oauth2/token";
		String clientid = Play.configuration.getProperty("sfdc.consumer_key");
		String secret = Play.configuration.getProperty("sfdc.consumer_secret");
		REDIRECT_URI = Play.configuration.getProperty("sfdc.redirect_uri");
		OAUTH = new SfdcOAuth(authorizationURL, accessTokenURL, clientid, secret);
	}
	
	public static void authenticate() {
		flash.keep();
		OAUTH.retrieveVerificationCode(REDIRECT_URI);
	}
	
	public static void createLead(Long id) {
		checkAuthenticity();
		
		Person person = Person.findById(id);
		if (person != null) {
			person.saveToSFDC();
			People.detail(person.twitterUser);
		}
	}
	
	public static void login(String twitterUser) {
		if (twitterUser != null) {
			flash.put("twitterUser", twitterUser);
		}
		OAUTH.retrieveVerificationCode(REDIRECT_URI);
	}
	
	public static void logout() {
		User user = RequiresLogin.getActiveUser();
		user.clearSfdcInfo();
		user.save();
		Application.config();
	}
	
	public static SfdcOAuthResponse retrieveSfdcAccessToken() {
		return OAUTH.retrieveSfdcAccessToken(REDIRECT_URI);
	}
}
