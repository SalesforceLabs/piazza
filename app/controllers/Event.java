package controllers;

import java.util.List;

import models.Conference;
import models.User;
import models.User.GroupedConferences;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Event manager.
 * 
 * @author mpaksoy
 */
@With(RequiresLogin.class)
public class Event extends Controller {
	public static void list() {
		User user = RequiresLogin.getActiveUser();
		GroupedConferences conf = user.getGroupedConferences();
		render(conf, user);
	}
	
	public static void set(long id) throws Exception { 
        checkAuthenticity();
		Conference c = Conference.findById(id);
		if (c == null) {
			badRequest();
		} else {
			User u = RequiresLogin.getActiveUser();
			if (! c.equals(u.conference)) {
				u.conference = c;
				u.save();
			}
		}
		Application.index();
	}
}
