package models;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Table;
import org.joda.time.DateTime;

import play.Logger;
import play.cache.Cache;
import play.data.validation.Required;
import play.db.jpa.Model;
import play.libs.Crypto;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.templates.JavaExtensions;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;
import util.CheckHelper;
import util.SfdcUtil;
import util.TwitterUtil;
import util.gson.SfdcCreateResponse;
import util.gson.SfdcRecord;
import util.gson.SfdcTask;

import com.google.gson.Gson;


@Entity
@Table(appliesTo="Person",
       indexes = { @Index(name="own_twt_idx", columnNames={"owner_id", "twitterUser"}) })
public class Person extends Model {
    private static final String TWIT_PIC_URL = "http://api.twitter.com/1/users/profile_image/%s?size=normal";

    // Twitter usernames are 15 chars max, just being safe
    @Required
    @Column(length=32)
    public String twitterUser;

    @Required
    public int followers, following;

    public String name;

    public String desc_;
    
    @Temporal(TemporalType.TIMESTAMP)
    public Date lastUpdateTime;
    
    public long twitterId;
    
    @ManyToOne
    @Nonnull
    @Required
    public User owner;
    
    public String location;
    
    /*
     * SFDC Lead Info
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date leadLastActivity;

    public String leadId;
    public String leadUrl;
    public String leadStatus;
    public String leadOwner;

    public String getPicURL() {
        return String.format(TWIT_PIC_URL, JavaExtensions.urlEncode(this.twitterUser));
    }

	private Person(User owner, twitter4j.User u) {
        this.owner = owner;
    	this.twitterId = u.getId();
    	this.twitterUser = u.getScreenName();
    	this.name = u.getName();
    	this.desc_ = u.getDescription();
    	this.followers = u.getFollowersCount();
    	this.following = u.getFriendsCount();
    	Status s = u.getStatus();
    	if (s != null) {
    		this.lastUpdateTime = s.getCreatedAt();
    	}
    	this.location = u.getLocation();
	}
	
	private String getTweetKey() {
	    return String.format("tweets:%s", this.twitterUser.toLowerCase());
	}
	
	public static Person get(User owner, String twitterName) {
	    return Person.find("owner = ? and twitterUser = ?", owner, twitterName).first();
	}

	public static Person getOrCreateSave(User owner, String twitterName) throws TwitterException {
	    Person ret = get(owner, twitterName);
	    if (ret == null) {
		    twitter4j.User twitterUser = TwitterUtil.getTwitter().showUser(twitterName);
		    ret = new Person(owner, twitterUser);
		    if (owner.hasSfdcToken()) {
			    // I don't trust the SFDC integration so I'm going to ignore any errors here
		        try {
		            ret.refreshSFDCInfo();
		        } catch (Exception e) {
		            Logger.error(e, "Error when checking for SFDC info for user: %s", ret.twitterUser);
		        }
		    }
		    ret.save();
	    }
	    return ret;
	}
	
    @Override
    public String toString() {
        return this.twitterUser + "[" + this.id +"]";
    }

    public List<Status> getTweets() {
	    String key = getTweetKey();
	    List<Status> ret = Cache.get(key, List.class);
	    if (ret == null) {
	        try {
	            Logger.info("cache miss: %s", key);
                ret = TwitterUtil.getTwitter().getUserTimeline(this.twitterId, new Paging(1, 10));
                Cache.safeSet(key, ret, "1h");
            } catch (TwitterException e) {
                ret = Collections.emptyList();
                Logger.error(e, "Error while trying to fetch timeline for %s", this.twitterUser);
            }
	    } else {
	        Logger.info("cache hit: %s", key);
	    }
	    return ret;
    }
    
    /**
     * @return true if lead is being tracked in Salesforce
     */
    public boolean isSFDCTracked() {
        return this.leadUrl != null;
    }

    /**
     * Check this lead against SFDC records to see if it's already being tracked.
     */
    public void refreshSFDCInfo() {
        if (! this.owner.hasSfdcToken()) {
            Logger.warn("Attempted to read from SFDC for user without token. User: %s Person: %s", this.owner.id, this.twitterUser);
            return;
        }

    	List<SfdcRecord> leads = SfdcUtil.getRecords(this.owner, "SELECT Id, Status, OwnerId, LastModifiedDate FROM Lead WHERE IsDeleted = false AND Company ='@" + this.twitterUser + "'");
    	if (leads != null && ! leads.isEmpty()) {
			SfdcRecord lead = leads.get(0);
    	    Logger.info("Found person in salesforce updating fields. Person: %s Sfdc id: %s", this.twitterUser, lead.id);
    	    this.leadId = lead.id;
	        this.leadUrl = this.owner.sfdcInstanceUrl + "/" + lead.id;
	        this.leadStatus = lead.status;
	        this.leadLastActivity = SfdcUtil.parseDate(lead.lastActivityDate);
	    	SfdcRecord user = SfdcUtil.getRecords(this.owner, "SELECT Name FROM User WHERE Id = '" + lead.ownerId + "'").get(0);
	    	this.leadOwner = user.name;
    	} else {
    	    Logger.info("Could not find user in salesforce clearing fields. Person: %s", this.twitterUser);
    	    this.leadId = null;
	        this.leadUrl = null;
	        this.leadStatus = null;
	        this.leadLastActivity = null;
	        this.leadOwner = null;
    	}
    }
    
    /**
     * Create a lead for this twitter user in Salesforce.
     * If the user is already saved in Salesforce does nothing.
     * If the record is updated, it will be auto-saved in the DB.
     */
    public void saveToSFDC() {
        if (!this.owner.hasSfdcToken() || this.isSFDCTracked()) {
            return;
        }

        refreshSFDCInfo();
        if (this.isSFDCTracked()) {
            return;
        }

        WSRequest createRequest = WS.url(this.owner.sfdcInstanceUrl + SfdcUtil.LEAD_REST_ENDPOINT);
        SfdcUtil.setToken(createRequest, Crypto.decryptAES(this.owner.sfdcAccessToken));
        createRequest.setHeader("Content-Type", "application/json");
        
        SfdcRecord lead = new SfdcRecord();
        /*
         *  It would also be nice if there was a custom field on Lead for twitter handle.
         */
        if (CheckHelper.notNullOrEmpty(this.name.trim())) {
            List<String> names = Arrays.asList(this.name.trim().split("\\s"));
            if (names.size() == 1) {
                lead.lastName = names.get(0);
            } else {
                lead.firstName = JavaExtensions.join(names.subList(0, names.size() - 1), " ");
                lead.lastName = names.get(names.size() - 1);
            }
        } else {
            lead.lastName = this.twitterUser;
        }

        lead.company = "@" + this.twitterUser; // this is where a custom field would be nice
        String leadDescription = "";
        if (CheckHelper.notNullOrEmpty(leadDescription)) {
            leadDescription += this.name + "'s description on Twitter: " + this.desc_ + ".\n";
        }
        if (this.owner.conference != null) {
            leadDescription += "Met " + this.name + " at conference " + this.owner.conference.name + ".\n";
        }
        if (CheckHelper.notNullOrEmpty(location)) {
            leadDescription += this.name + " lives in " + location + ".\n";
        }
        lead.description = leadDescription;
        lead.website = "http://twitter.com/" + this.twitterUser;
        createRequest.body = new Gson().toJson(lead, SfdcRecord.class);
        HttpResponse createResponse = createRequest.post();
        SfdcCreateResponse sfdcCreateResponse = new Gson().fromJson(createResponse.getJson(), SfdcCreateResponse.class);
        if (! createResponse.success() || ! sfdcCreateResponse.success) {
            Logger.error("Failed to insert Lead for person '%s' Response: %s", this.twitterUser, createResponse.getJson());
            return;
        }
        Logger.info("Successfully created lead with id %s in SFDC for Person %s", sfdcCreateResponse.id, this.twitterUser);
        refreshSFDCInfo();
        save();
    }
    
    /**
     * Log the Twitter message sent to the Person in Salesforce.
     * If the Person is not yet tracked in Salesforce it is saved there.
     * If the Person is updated it will be auto-saved to DB.
     */
    public void logTweetSave(String message) {
        if (! this.owner.hasSfdcToken()) {
            Logger.info("Cannot log Tweet for Person %s because User %s does not have an SFDC token.", this.twitterUser, this.owner.id);
            return;
        }

        saveToSFDC();
        if (! this.isSFDCTracked()) {
            Logger.info("Cannot log Tweet for Person %s because saving to SFDC appears to have failed.", this.twitterUser);
            return;
        }

        WSRequest createRequest = WS.url(this.owner.sfdcInstanceUrl + SfdcUtil.TASK_REST_ENDPOINT);
        SfdcUtil.setToken(createRequest, Crypto.decryptAES(this.owner.sfdcAccessToken));
        createRequest.setHeader("Content-Type", "application/json");
        
        SfdcTask task = new SfdcTask();
        task.activityDate = new DateTime().toString();
        task.description = "Sent following message via twitter from " + this.owner.id + ": " + message;
        task.whoId = this.leadId;
        task.priority = "Low";
        task.subject = "Tweet sent: " + message;
        task.status = "Completed";
        createRequest.body = new Gson().toJson(task, SfdcTask.class);
        HttpResponse createResponse = createRequest.post();
        SfdcCreateResponse sfdcCreateResponse = new Gson().fromJson(createResponse.getJson(), SfdcCreateResponse.class);
        if (!createResponse.success() || ! sfdcCreateResponse.success) {
            Logger.error("Failed to insert Lead for person '%s' Response: %s", this.twitterUser, createResponse.getJson());
            return;
        }
        Logger.info("Successfully created task with id %s in SFDC for Lead %id Person %s", sfdcCreateResponse.id, this.leadId, this.twitterUser);
    }
}