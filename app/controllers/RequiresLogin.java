package controllers;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import models.User;
import play.Logger;
import play.Play;
import play.libs.Crypto;
import play.libs.OAuth;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.templates.JavaExtensions;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import util.TwitterUtil;

/**
 * Make given controller or controller methods require login.
 * Usage:
 * @With(RequiresLogin.class)
 * 
 * Based on {@link Secure}.
 *
 * @author mpaksoy
 */
public class RequiresLogin extends Controller {
    private final static boolean isStaging = "true".equals(Play.configuration.getProperty("staging", "false").toLowerCase());

    @Before(priority=10)
    static void https() {
    	if (request.headers.containsKey("x-forwarded-for")) {
    		Header h = request.headers.get("x-forwarded-for");
    		request.remoteAddress = h.value();
    	}

    	if (request.headers.containsKey("x-forwarded-proto")) {
    		Header h = request.headers.get("x-forwarded-proto");
    		request.secure = "https".equals(h.value());
    	}
    }
	
    @Before
    static void log() {
    	Logger.info(JavaExtensions.join(Arrays.asList(request.remoteAddress,
    												  request.method,
    												  request.secure ? "http" : "ssl",
    												  isStaging ? "staging" : "public",
    												  session.get("username"),
    												  request.url), ":"));
    }

    @Before(unless={"login", "auth", "logout"})
    static void checkAccess() throws Throwable {
    	User u = getActiveUser();
        if (u == null){
            if ("GET".equals(request.method)) {
            	flash.put("url", request.url);
            }
            
            if (Play.mode.isProd()) {
	            redirect("https://" + request.domain + "/login");
            }
            
            redirect("https://" + request.domain + ":" + Play.configuration.getProperty("https.port") + "/login");
        }

        // If we're on a staging environment but user doesn't have staging perms disallow
        if (isStaging && ! u.isStaging()) {
	        Logger.warn("Unauthorized staging access attempt. Headers: %s", request.headers);
            forbidden("User doesn't have permission to view staging environment.");
        }

        // Checks
        Check check = getActionAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
        check = getControllerInheritedAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
    }
    
	private static void check(Check check) {
        for(String profile : check.value()) {
            boolean hasProfile = hasProfile(profile);
            if (! hasProfile) {
            	Logger.info("User %s failed access check %s", getActiveUser() == null ? null : getActiveUser().id, profile);
            	forbidden("Forbidden. User missing profile: " + profile);
            }
        }
	}

	private static boolean hasProfile(String profile) {
		User u = getActiveUser();
		if (u != null && "isAdmin".equals(profile)) {
			return u.isAdmin();
		}
		
		return false;
	}

	public static void login() {
		User u = getActiveUser();
		if (u != null) {
			Logger.info("User visited login url, but already logged in. Username: %s", u.id);
			redirectToOriginalURL();
		} else {
			flash.keep("url");
			render();
		}
	}

    public static void auth() throws Exception {
    	flash.keep();
        if (OAuth.isVerifierResponse()) {
            // We got the verifier; now get the access token, store it and back to index

        	String token = session.get("token");
        	String secret = session.get("secret");
        	if (token == null || secret == null) {
        		Logger.error("Invalid session, got callback from Twitter but missing token or session. " +
        					 "Token: %s Secret: %s", token, secret);
        		throw new IllegalStateException();
        	}
            OAuth.Response oauthResponse = OAuth.service(TwitterUtil.OAUTH).retrieveAccessToken(token, secret);
            if (oauthResponse.error == null) {
                token = oauthResponse.token;
                secret = oauthResponse.secret;
                Configuration c = new ConfigurationBuilder().setOAuthConsumerKey(TwitterUtil.OAUTH.consumerKey)
                										    .setOAuthConsumerSecret(TwitterUtil.OAUTH.consumerSecret).build();
                Twitter t = new TwitterFactory(c).getInstance(new AccessToken(token, secret));
                twitter4j.User u = t.verifyCredentials();
                if (u != null) {
	            	User user = User.getOrCreate(u.getScreenName());
	                user.token = Crypto.encryptAES(token);
	                user.secret = Crypto.encryptAES(secret);
		        	session.remove("secret", "token");
		        	session.put("username", user.id);
	                Logger.info("Twitter auth successful. User: %s", user.id);
                	user.save();
	            } else {
	                Logger.error("Could not verify credentials with Twitter.");
	            }
            } else {
                Logger.error("Error connecting to twitter: " + oauthResponse.error);
            }
        } else {
	        OAuth twitt = OAuth.service(TwitterUtil.OAUTH);
	        OAuth.Response oauthResponse = twitt.retrieveRequestToken();
	        if (oauthResponse.error == null) {
	        	session.put("token", oauthResponse.token);
	        	session.put("secret", oauthResponse.secret);
	            redirect(twitt.redirectUrl(oauthResponse.token));
	        } else {
	            Logger.error("Error connecting to twitter: " + oauthResponse.error);
	        }
        }
        redirectToOriginalURL();
    }
    
    public static void logout() {
    	session.remove("username");
    	redirect("/");
    }
    
    static void redirectToOriginalURL() {
        String url = flash.get("url");
        if(url == null) {
            url = "https://" + request.domain + "/";
        }
        redirect(url);
    }
    
    public static User getActiveUser() {
    	String username = session.get("username");
    	if (username == null) {
    		return null;
    	}
    	
    	return User.get(username);
    }
}
