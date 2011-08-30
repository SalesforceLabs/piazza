package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.Person;
import models.User;
import play.data.validation.Error;
import play.data.validation.Match;
import play.data.validation.Required;
import play.libs.OAuth2;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.results.Redirect;
import util.SfdcUtil;
import util.TwitterUtil;
import util.gson.SfdcOAuthResponse;

@With(RequiresLogin.class)
public class Application extends Controller {
    public static void refresh() throws Exception {
        TwitterUtil.clearResultCacheForCurrentUser();
    	index();
    }

    public static void index() throws Exception {
    	User user = RequiresLogin.getActiveUser();
    	
    	if(user.getLocationStr() == null){
    		waiting();
    	}
    	
    	SfdcUtil.refreshToken();
    	List tweets = TwitterUtil.getSuggestedTweets();

    	render(tweets, user);
	}

	public static void config() {
		User user = RequiresLogin.getActiveUser();
		if (OAuth2.isCodeResponse()) {
			// if we're here, we've been redirected from SFDC with a code so lets use it
			SfdcOAuthResponse response = SfdcAuthentication.retrieveSfdcAccessToken();
			user.setSfdcInfo(response);
			user.save();
			if (flash.contains("twitterUser")) {
				redirect("/people/" + flash.get("twitterUser"));
			}
			redirect("/config");
		} 

        SfdcUtil.refreshToken();
		render(user);
	}
	
	public static void deleteKeyword(String keyword) {
		checkAuthenticity();
		User u = RequiresLogin.getActiveUser();
		if (u != null) {
			u.deleteKeyword(keyword);
			u.save();
		}
	}
	
	public static void createKeyword(@Match(value="\\w+", message="Keyword can only contain letter and numbers.") @Required(message="Keyword missing.") String keyword) {
		checkAuthenticity();
		if (validation.hasErrors()) {
			StringBuilder ret = new StringBuilder();
			ret.append("Error: ");
			for (Error e: validation.errors()) {
				ret.append(e.message()).append("\n");
			}
			response.status = 400;
			renderText(ret.toString());
		}

		User u = RequiresLogin.getActiveUser();
		if (u != null) {
			if (u.addKeyword(keyword)) {
				u.save();
				response.status = 201;
			} else {
				response.status = 409;
				renderText(String.format("Keyword '%s' already exists.", keyword));
			}
		}
	}
	
	/**
	 * Diplay loading message, and prompt user for Location via JS and
	 * Redirects to the event detail page.
	 */
	public static void waiting(){
		render();
	}
}
