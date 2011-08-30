/**
 * 
 */
package util.gson;

import java.util.List;


import com.google.gson.annotations.SerializedName;

/**
 * @author marcus
 */
public class SfdcRestResponse {
	
	@SerializedName("totalSize")
	public int totalSize;
	
	@SerializedName("done")
	public boolean done;
	
	public List<SfdcRecord> records;

	@Override
	public String toString() {
		return "SfdcRestResponse [totalSize=" + totalSize + ", done=" + done
				+ ", records=" + records + "]";
	}
	

}
