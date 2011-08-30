package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.SingletonMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import controllers.RequiresLogin;
import models.Conference;
import models.User;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.Crypto;
import play.libs.OAuth.ServiceInfo;
import play.templates.JavaExtensions;
import twitter4j.GeoLocation;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterUtil {
	public static Twitter getTwitter(User user) {
        AccessToken accessToken = new AccessToken(Crypto.decryptAES(user.token), Crypto.decryptAES(user.secret));
		Twitter t = new TwitterFactory(new ConfigurationBuilder().setOAuthConsumerKey(OAUTH.consumerKey)
																 .setOAuthConsumerSecret(OAUTH.consumerSecret).build()).getInstance(accessToken);
        return t;
	}
	
	public static Twitter getTwitter() {
		return getTwitter(RequiresLogin.getActiveUser());
	}

	public static final ServiceInfo OAUTH = new ServiceInfo(
	        "http://twitter.com/oauth/request_token",
	        "http://twitter.com/oauth/access_token",
	        "http://twitter.com/oauth/authenticate",
	        System.getenv("TWT_OAUTH_KEY"),
	        System.getenv("TWT_OAUTH_SECRET")
	);

	private static Query buildQuery() {
		User ac = RequiresLogin.getActiveUser();

		Set keywordSet = Sets.newTreeSet();
		Conference conference = ac.conference;
		if (conference != null) {
		    // clone so that we don't modify the original user
			keywordSet.add(conference.hashtag);
		}
		
		Set<String> userKeywords = ac.getKeywordSet();
		if (userKeywords != null) {
			keywordSet.addAll(userKeywords);
		}
		
		if (keywordSet.isEmpty()) {
		    return null;
		}
		
		Query q = new Query();
		q.query(JavaExtensions.join(keywordSet, " "));
		return q;
	}
	
	private static String getCacheKey(Query q) {
	    return "search:" + q.getQuery().replace(" ", ",");
	}

	public static void clearResultCacheForCurrentUser() {
	    Query q = buildQuery();
	    if (q != null) {
	        String key = getCacheKey(q);
	        Logger.info("cache clear: %s", key);
		    Cache.safeDelete(key);
	    }
	}

	public static List<Tweet> getSuggestedTweets() throws TwitterException {
		Query q = buildQuery();    		
		if (q == null) {
		    return Collections.emptyList();
		}
		
		String key = getCacheKey(q);
		Twitter t = getTwitter();
		List<Tweet> ret = Cache.get(key, List.class);
		Set<String> seen = Sets.newHashSet();
		if (ret == null) {
		    ret = Lists.newArrayList();
		    Logger.info("cache miss: '%s'", key);
			QueryResult result = t.search(q);
			for (Tweet tweet : result.getTweets()) {
			    if (! seen.contains(tweet.getFromUser())) {
			        Cache.set(getTweetCacheKey(RequiresLogin.getActiveUser(), tweet.getFromUser()), tweet, "3h");
			        seen.add(tweet.getFromUser());
				    ret.add(tweet);
			    }
			}
			Cache.set(key, ret, "10mn");
		} else {
		    Logger.info("cache hit: '%s'", key);
		}
		
		return ret;
	}
	
	public static String getTweetCacheKey(User owner, String twitterUser) {
	    return String.format("relevantTweet:%s:%s", owner.id, twitterUser);
	}
}
