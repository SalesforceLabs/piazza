package controllers;

import java.util.List;

import models.Conference;
import models.User;
import play.mvc.With;
import play.test.Fixtures;

@With(RequiresLogin.class)
@Check("isAdmin")
public class Conferences extends ProtectedCRUD {
    public static void reset() {
    	List<Conference> conferences = Conference.all().fetch();
    	for (Conference c: conferences) {
    		List<User> users = User.find("conference = ?", c).fetch();
    		for (User user: users) {
    			user.conference = null;
    			user.save();
    		}
    	}
    		
    	Fixtures.delete(Conference.class);
    	Fixtures.loadModels("data.yml");
    	renderText("Loaded data from data.yml");
    }

}
