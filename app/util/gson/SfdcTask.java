/**
 * 
 */
package util.gson;

import java.util.Date;


import com.google.gson.annotations.SerializedName;

/**
 * @author marcus
 */
public class SfdcTask {

	public SfdcAttributes attributes;
	
	@SerializedName("Id")
	public String id;
	
	@SerializedName("Description")
	public String description;
	
	@SerializedName("ActivityDate")
	public String activityDate;
	
	@SerializedName("Subject")
	public String subject;
	
	/**
	 * Picklist
	 * The type of task, such as Call or Meeting.
	 */
	@SerializedName("Type")
	public String type;
	
	/**
	 * ID of a related Contact or Lead. If the WhoId refers to a lead, then the WhatId field must be empty. Label is Contact/Lead ID.
	 */
	@SerializedName("WhoId")
	public String whoId;
	
	/**
	 * Required. The current status of the task, such as In Progress or Completed. Each predefined Status field implies a value for the IsClosed flag. To obtain picklist values, a client application can invoke the query() call on the TaskStatus object.
	 */
	@SerializedName("Status")
	public String status;
	
	/**
	 * Required. Indicates the importance or urgency of a task, such as high or low.
	 */
	@SerializedName("Priority")
	public String priority;
	
}
