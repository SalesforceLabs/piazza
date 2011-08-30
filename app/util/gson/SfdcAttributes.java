package util.gson;

import com.google.gson.annotations.SerializedName;

/**
 * @author marcus
 */
public class SfdcAttributes {
	
	@SerializedName("type")
	public String type;
	
	@SerializedName("url")
	public String url;

	@Override
	public String toString() {
		return "SfdcAttribute [type=" + type + ", url=" + url + "]";
	}
}
