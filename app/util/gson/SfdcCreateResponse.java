/**
 * 
 */
package util.gson;

import com.google.gson.annotations.SerializedName;

/**
 * @author marcus
 */
public class SfdcCreateResponse {
	
	@SerializedName("id")
	public String id;
	
	@SerializedName("success")
	public boolean success;
	
	/*
	 * errors array but lets ignore it
	 */

}
