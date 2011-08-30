/**
 * 
 */
package util.gson;

import java.util.List;


import com.google.gson.annotations.SerializedName;

/**
 * @author marcus
 */
public class SfdcRecord {
	
	public SfdcAttributes attributes;
	
	@SerializedName("Id")
	public String id;
	
	/*
	 * User fields
	 */
	@SerializedName("Username")
	public String username;
	
	@SerializedName("Name")
	public String name;
	
	/*
	 * Lead fields
	 */
	@SerializedName("FirstName")
	public String firstName;
	
	@SerializedName("LastName")
	public String lastName;
	
	@SerializedName("Description")
	public String description;
	
	@SerializedName("Company")
	public String company;
	
	@SerializedName("Website")
	public String website;
	
	@SerializedName("Status")
	public String status;
	
	@SerializedName("OwnerId")
	public String ownerId;
	
	@SerializedName("LastModifiedDate")
	public String lastActivityDate;
	
}
