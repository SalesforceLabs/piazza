package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.User;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

@With(RequiresLogin.class)
public class Location extends Controller {
	private static final Map<String, Integer> RANKS = new HashMap<String, Integer>() {{
		   put("neighborhood", 100);
	       put("locality", 90);
           put("street_address", 80);
           put("country", 70);
		}};

	public static class Result {
		public String formatted_address;
		public List<String> types;
		
		public int getRank() {
			List<Integer> ranks = Lists.transform(this.types, new Function<String, Integer>() {
				@Override
				public Integer apply(String str) {
					return RANKS.containsKey(str) ? RANKS.get(str) : -1;
				}
			});
			return Ordering.natural().max(ranks);
		}
	}
	
	private static Result getBestResult(JsonElement e) {
		List<Result> l = new Gson().fromJson(e.getAsJsonObject().getAsJsonArray("results"), new TypeToken<List<Result>>() {}.getType());
		Collections.sort(l, new Comparator<Result>() {
			@Override
			public int compare(Result o1, Result o2) {
				return o2.getRank() - o1.getRank();
			}
		});

		return l.isEmpty() ? null : l.get(0);
	}
	
	public static void resolve(String lat, String long_) {
        checkAuthenticity();
		//now do the google api stuff to get the location
		HttpResponse latResp = WS.url("http://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&sensor=false", lat, long_).get();
		response.status = latResp.getStatus();
		Result r = getBestResult(latResp.getJson());

		User u = RequiresLogin.getActiveUser();
		u.lat = new Double(lat);
		u.long_ = new Double(long_);
		if (r != null) {
			u.address = r.formatted_address;
		} else {
			u.address = null;
		}
		u.save();

		Event.list();
	}
	
	public static void remove(boolean viewEvents) throws Exception {
        checkAuthenticity();
		User u = RequiresLogin.getActiveUser();
		u.address = null;
		u.lat = null;
		u.long_ = null;
		u.conference = null;
		u.save();
		if (viewEvents) {
			Event.list();
		}
		Application.index();
	}
}
