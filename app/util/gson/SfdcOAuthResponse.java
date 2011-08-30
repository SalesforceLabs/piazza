/**
 * 
 */
package util.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import play.libs.OAuth2.Response;
import play.libs.WS.HttpResponse;

/**
 * Similar to {@link Response}, but SFDC specific
 * 
 * @author marcus
 */
public class SfdcOAuthResponse {
	
	@SerializedName("id")
	public String id;
	
	@SerializedName("issued_at")
	public String issuedAt;
	
	@SerializedName("refresh_token")
	public String refreshToken;
	
	@SerializedName("instance_url")
	public String instanceUrl;
	
	@SerializedName("signature")
	public String signature;
	
	@SerializedName("access_token")
	public String accessToken;

	@Override
	public String toString() {
		return "SfdcOAuthResponse [id=" + id + ", issuedAt=" + issuedAt
				+ ", refreshToken=" + refreshToken + ", instanceUrl="
				+ instanceUrl + ", signature=" + signature + ", accessToken="
				+ accessToken + "]";
	}
}
