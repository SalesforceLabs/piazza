/**
 * 
 */
package util;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import play.Logger;
import play.libs.OAuth2;
import play.libs.WS;
import play.libs.OAuth2.Response;
import play.libs.WS.HttpResponse;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;
import util.gson.SfdcOAuthResponse;

/**
 * Sfdc is a little picky so it gets its own OAuth class
 * 
 * Modeled after {@link OAuth2}
 * 
 * @author marcus
 */
public class SfdcOAuth {
	
	/* ugly but Play won't dig into child classes for member variables */
	public final String authorizationURL;
	public final String clientid;
	public final String secret;
	public final String accessTokenURL;

	public SfdcOAuth(String authorizationURL, String accessTokenURL,
			String clientid, String secret) {
		this.authorizationURL = authorizationURL;
		this.clientid = clientid;
		this.secret = secret;
		this.accessTokenURL = accessTokenURL;
	}
	
    /**
     * First step of the OAuth2 process: redirects the user to the authorization page
     *
     * @param callbackURL
     */
    public void retrieveVerificationCode(String callbackURL) {
        throw new Redirect(authorizationURL
                + "?client_id=" + clientid
                + "&redirect_uri=" + callbackURL
                + "&response_type=code"
                + "&display=touch");
    }
	
	public SfdcOAuthResponse retrieveSfdcAccessToken(String callbackURL) {
        String accessCode = Params.current().get("code");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("client_id", clientid);
        params.put("client_secret", secret);
        params.put("redirect_uri", callbackURL);
        params.put("code", accessCode);
        params.put("grant_type", "authorization_code");
        HttpResponse response = WS.url(accessTokenURL).params(params).post();
        if (!response.success()) {
        	Logger.error("Error when trying to get access code: " + response.toString());
        }
        Gson gson = new Gson();
        return gson.fromJson(response.getJson(), SfdcOAuthResponse.class);
    }
	
	public SfdcOAuthResponse refreshSfdcAccessToken(String refreshToken) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("client_id", clientid);
        params.put("client_secret", secret);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        HttpResponse response = WS.url(accessTokenURL).params(params).post();
        if (!response.success()) {
        	throw new IllegalStateException("Error when trying to get access code: " + response.getJson());
        }
        Gson gson = new Gson();
        return gson.fromJson(response.getJson(), SfdcOAuthResponse.class);
    }
	
	@Override
	public String toString() {
		return "SfdcOAuth [authorizationURL=" + authorizationURL
				+ ", clientid=" + clientid + ", secret=" + secret
				+ ", accessTokenURL=" + accessTokenURL + "]";
	}
}
