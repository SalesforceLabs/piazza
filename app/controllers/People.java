package controllers;
 
import java.util.Collections;
import java.util.List;

import models.Person;
import models.User;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import twitter4j.Tweet;
import twitter4j.TwitterException;
import util.SfdcUtil;
import util.TwitterUtil;

import com.google.common.collect.Lists;

@With(RequiresLogin.class)
public class People extends Controller {    	
	
    public static void detail(String twitterUser) {
        SfdcUtil.refreshToken();

        User user = RequiresLogin.getActiveUser();

        Person person;
        try {
            person = Person.getOrCreateSave(user, twitterUser);
            Tweet tweet = Cache.get(TwitterUtil.getTweetCacheKey(user, twitterUser), Tweet.class);
        
            List msgs = buildCannedMessages();
            render(person, user, msgs, tweet);
        } catch (TwitterException e) {
            Logger.error(e, "Error when fetching user from twitter: %s", twitterUser);
            notFound(JavaExtensions.escapeHtml(twitterUser) + " user not found");
        }
    }
    
    public static void refreshSfdc(String twitterUser) {
        checkAuthenticity();
        Person person = Person.get(RequiresLogin.getActiveUser(), twitterUser);
        if (person != null){ 
	        Logger.info("User requested refresh for SFDC info for Person %s", person.twitterUser);
	        person.refreshSFDCInfo();
	        person.save();
	        detail(person.twitterUser);
        }
    }
    
    public static void respond(String twitterUser, String msg) {
        render(twitterUser, msg);
    }

    public static void updateStatus(String twitterUser, String msg) throws Exception {
        checkAuthenticity();
        TwitterUtil.getTwitter().updateStatus(msg);
        User user = RequiresLogin.getActiveUser();

        Person person = Person.get(user, twitterUser);
        try {
	        if (person != null && user.hasSfdcToken()) {
	            person.logTweetSave(msg);
	        }
        } catch (Exception e) {
            Logger.error(e, "Error while trying to log SFDC case.");
        }

        detail(twitterUser);
    }
    
	public static List<String> buildCannedMessages() {
		User user = RequiresLogin.getActiveUser();
		if(user.conference != null && user.conference.hashtag != null){
			String msg1 = "I see you're at "+ user.conference.hashtag +", want to get together for a quick chat?";
			String msg2 = "I see you're at "+ user.conference.hashtag +",  want to meet up after this session?";
			return Lists.newArrayList(msg1,msg2);
		}
		return Collections.EMPTY_LIST;
	}
}
