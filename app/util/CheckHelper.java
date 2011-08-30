/**
 * 
 */
package util;

/**
 * @author marcus
 */
public class CheckHelper {
	
	public static boolean notNullOrEmpty(String value) {
		return !(value == null || value.equals(""));
	}

}
