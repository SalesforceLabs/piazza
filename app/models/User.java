package models;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;

import play.data.validation.Required;
import play.db.jpa.GenericModel;
import play.libs.Crypto;
import play.templates.JavaExtensions;
import util.SfdcUtil;
import util.gson.SfdcOAuthResponse;
import util.gson.SfdcRecord;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;


@Entity(name="users")
public class User extends GenericModel {
	// Id is Twitter username
    // Twitter username length limit is 15 so we use 32 just to be safe
    @Required @Id
    @Column(length=32)
    public String id;

    // Twitter credentials
    @Column(length=1024)
    public String token;
    @Column(length=1024)
    public String secret;

    public String keywords;
    
    // true iff the user can access staging environment
    private Boolean staging;

    // SFDC
    @Column(length=1024)
    public String sfdcAccessToken;
    @Column(length=1024)
    public String sfdcRefreshToken;
    public String sfdcInstanceUrl;
    public String sfdcId;
    public Long sfdcIssuedAt;
    @Column(length=1024)
    public String sfdcSignature;
    public String sfdcUsername;

    @ManyToOne
    @Index(name="conf_idx")
    public Conference conference;
    
    // Geo
    public Double lat;
    public Double long_;
    public String address;
    
    public static final Set<String> ADMINS = Collections.unmodifiableSet(Sets.newHashSet("djmojorisin", "anzadev", "mustpax"));

    private User(String id) {
        this.id = id;
        this.keywords = "";
        this.staging = isAdmin();
    }

    @Override
    public Object _key() {
        return this.id;
    }

    public void setStaging(boolean staging) {
        this.staging = staging;
    }

    public boolean isStaging() {
        if (this.staging == null) {
            return false;
        }

        return this.staging;
    }

    public static User get(String id) {
        return User.findById(id);
    }

    public static User getOrCreate(String id) {
        User user = get(id);
        if (user == null) {
            user = new User(id);
        }
        return user;
    }

    public boolean isAdmin() {
        return ADMINS.contains(this.id.toLowerCase());
    }
    
    public Set<String> getKeywordSet() {
        if (this.keywords == null || this.keywords.trim().length() == 0) {
            return null;
        }
    
        return Sets.newHashSet(this.keywords.split(","));
    }

    public void deleteKeyword(String keyword) {
        Set<String> keywords = getKeywordSet();
        keywords.remove(keyword);
        setKeywords(keywords);
    }

    public void setKeywords(Set<String> keywords) {
        String str = JavaExtensions.join(keywords, ",");
        this.keywords = str;
    }
    
    /**
     * @return false iff the keyword provided already exists, true if new keyword was added
     */
    public boolean addKeyword(String keyword) {
        if (keyword == null || keyword.indexOf(",") >= 0) {
            throw new IllegalArgumentException("Missing or invalid keyword. Keywords can't contain commas.");
        }

        Set<String> keywords = getKeywordSet();
        if (keywords == null) {
            keywords = Sets.newHashSet();
        }

        boolean ret = keywords.add(keyword);
        if (ret) {
            setKeywords(keywords);
        }
        return ret;
    }
    
    /**
     * Caller should commit
     * @param response - sfdc oauth response
     */
    public void setSfdcInfo(SfdcOAuthResponse response) {
    	this.sfdcAccessToken = Crypto.encryptAES(response.accessToken);
    	this.sfdcInstanceUrl = response.instanceUrl;
    	if (response.refreshToken != null) {
    		// refresh token is only returned in the original login response, not future refresh requests
    		this.sfdcRefreshToken = Crypto.encryptAES(response.refreshToken);
    	}
    	String userId = response.id.substring(response.id.lastIndexOf('/') + 1, response.id.length() - 3);
    	this.sfdcId = userId;
    	this.sfdcSignature = Crypto.encryptAES(response.signature);
    	this.sfdcIssuedAt = Long.valueOf(response.issuedAt);
    	// now lets get the username
    	List<SfdcRecord> users = SfdcUtil.getRecords(this, "SELECT Username FROM User WHERE Id='" + this.sfdcId + "'");
    	this.sfdcUsername = users.get(0).username;
    }
    
    public void clearSfdcInfo() {
    	this.sfdcAccessToken = null;
    	this.sfdcId = null;
    	this.sfdcInstanceUrl = null;
    	this.sfdcIssuedAt = null;
    	this.sfdcRefreshToken = null;
    	this.sfdcSignature = null;
    	this.sfdcUsername = null;
    }
    
    public boolean hasSfdcToken() {
    	return this.sfdcAccessToken != null;
    }

    /**
     * Orders conferences based on <em>increasing</em> distance to given user.
     * If user has no location, just sort conferences alphabetically.
     * 
     * @author mpaksoy
     */
    public static class ConferenceDistanceComparator implements Comparator<Conference> {
		private final LatLng userLoc;

		public ConferenceDistanceComparator(User u) {
			if (u.hasLocation()) {
		    	this.userLoc = new LatLng(u.lat, u.long_);
			} else {
				this.userLoc = null;
			}
    	}

		@Override
		public int compare(Conference o1, Conference o2) {
			if (o1 == o2 || o1.equals(o2)) {
				return 0;
			}
			
			if(o1.isHappeningNow() != o2.isHappeningNow()){
				return o1.isHappeningNow() ? -1 :1;
			}
			
			// If user has no location just alphasort
			if (this.userLoc == null) {
				return o1.name.compareTo(o2.name);
			}

    		double c1dist = LatLngTool.distance(this.userLoc, 
    											new LatLng(o1.confLat, o1.confLong_),
    											LengthUnit.MILE);
    		double c2dist = LatLngTool.distance(this.userLoc, 
    											new LatLng(o2.confLat, o2.confLong_),
    											LengthUnit.MILE);
    		if (c1dist == c2dist) {
				return o1.name.compareTo(o2.name);
    		}

    		
    		return c1dist > c2dist ? 1 : -1;
		}
    }
    
    public static class GroupedConferences {
    	public List<Conference> present;
    	public List<Conference> future;
		public List<Conference> past;

    	public GroupedConferences() {
			this.present = Lists.newArrayList();
			this.future = Lists.newArrayList();
			this.past = Lists.newArrayList();
		}
    }
    
    private List<Conference> getSortedConferces() {
    	List<Conference> listOfConferences = Conference.all().fetch();
    	return Ordering.from(new ConferenceDistanceComparator(this)).sortedCopy(listOfConferences);
    }

    /**
     * @return conference suggestions for this user grouped into three buckets, past, present, future.
     */
    public GroupedConferences getGroupedConferences() {
    	List<Conference> sorted = getSortedConferces();
    	GroupedConferences ret = new GroupedConferences();
    	
    	for (Conference c: sorted) {
    		if (c.isHappeningNow()) {
    			ret.present.add(c);
    		} else if (c.isUpcoming()) {
    			ret.future.add(c);
    		} else {
    			ret.past.add(c);
    		}
    	}

    	return ret;
    }
    
    /**
     * @return String representing user's current location. If possible we try
     *         to return the current conference name, otherwise approximate
     *         location string is displayed.
     */
    public String getLocationStr() {
        if (this.conference == null) {
        	return null;
        }
        return this.conference.hashtag;
    }
    
    public boolean hasLocation() {
    	return this.lat != null && this.long_ != null;
    }
    
    @Override
    public String toString() {
        return "User" + "[" + this.id +"]";
    }
}
