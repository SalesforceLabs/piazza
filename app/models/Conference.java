package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.DateTime;

import play.Logger;
import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class Conference extends Model {
    @Required
	public String name;
	public String hashtag;
	
	@Temporal(TemporalType.DATE)
	@As("yyyy-MM-dd")
    public Date startDate;
	
	@Temporal(TemporalType.DATE)
	@As("yyyy-MM-dd")
    public Date endDate;
	
	public String address; 
	public String city;
	public Double confLat;
	public Double confLong_;
	
    @Override
    public String toString() {
        return this.name + "[" + this.id +"]";
    }
    
    /**
     * We assume the conference is in session on the last day. So if a conference ends on, say,
     * the 21st of September, the confernece is counted as being in session for all of 21st of 
     * September.
     *  
     * @return true if conference is currently happening
     */
    public boolean isHappeningNow() {
    	Date now = new Date();
    	// We assume the conference is in session on the last day, so we just push the end date
    	// to include the whole day
    	Date dayAfter = new DateTime(endDate).plusDays(1).toDate();
    	return startDate.before(now) && dayAfter.after(now);
    }
    
    /**
     * @return true if conference occurs in the future
     */
    public boolean isUpcoming() {
    	return startDate.after(new Date());
    }
}
