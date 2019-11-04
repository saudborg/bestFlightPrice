package borges;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Main {

	public static final String KEY = "RmRxpyRk9VmshEWEI9wedVZFqjBUp1DsO27jsnXJcMb8Rw15NI";

	public static void main(String[] args) throws UnirestException, ParseException {

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		String from = "Sao%20Paulo";
		String when = "2019-12-25";
		String withInbound = "2019-12-6";
		int gap = 9;
		String placeIdFrom = getPlaceId(from);

		List<String> listTo = Arrays.asList("Amsterdam", "Porto", "Madrid");
		//List<String> listTo = Arrays.asList("Hanoi", "Bankok","Philippines");

		Double min = Double.MAX_VALUE;
		JSONObject best = null;
		String bestFromCode = null;
		String bestToCode = null;
		String bestDate = null;

		for (String toX : listTo) {
			String placeIdTo = getPlaceId(toX);

			Calendar originalMore = Calendar.getInstance();
			originalMore.setTime(formatter.parse(when));
			originalMore.add(Calendar.DAY_OF_MONTH, gap);

			Calendar originalLess = Calendar.getInstance();
			originalLess.setTime(formatter.parse(when));
			originalLess.add(Calendar.DAY_OF_MONTH, -gap);

			JSONArray quotes = getQuotes(when, placeIdFrom, placeIdTo, withInbound);

			Iterator<Object> iterator = quotes.iterator();

			JSONObject currentBest = getCheaperOption(formatter, originalMore, originalLess, iterator, min, best);
			if (best != null) {
				Double minBest = (Double) best.get("MinPrice");
				Double minCurrent = (Double) currentBest.get("MinPrice");
				if (minCurrent < minBest) {
					best = currentBest;
					bestFromCode = placeIdFrom;
					bestToCode = placeIdTo;
					bestDate = ((JSONObject) best.get("OutboundLeg")).get("DepartureDate").toString();
				}
			} else {
				best = currentBest;
				bestFromCode = placeIdFrom;
				bestToCode = placeIdTo;
				bestDate = ((JSONObject) best.get("OutboundLeg")).get("DepartureDate").toString();
			}
		}

		System.out.println(best.get("OutboundLeg") + " for " + best.get("MinPrice") + " to " + bestToCode);
		bestDate = bestDate.substring(0, 10);

		HttpResponse<JsonNode> response = Unirest.get(
				"https://skyscanner-skyscanner-flight-search-v1.p.rapidapi.com/apiservices/browsequotes/v1.0/US/EUR/en-US/"
						+ bestFromCode + "/" + bestToCode + "/" + bestDate)
				.header("x-rapidapi-host", "skyscanner-skyscanner-flight-search-v1.p.rapidapi.com")
				.header("x-rapidapi-key", KEY).asJson();

		System.out.println(response);

		JSONArray places = (JSONArray) response.getBody().getObject().get("Places");

	}

	private static JSONObject getCheaperOption(SimpleDateFormat formatter, Calendar originalMore, Calendar originalLess,
			Iterator<Object> iterator, Double min, JSONObject best) throws ParseException {
		while (iterator.hasNext()) {
			JSONObject next = (JSONObject) iterator.next();
			JSONObject outbound = (JSONObject) next.get("OutboundLeg");
			Object departureDate = outbound.get("DepartureDate");
			Date c = formatter.parse(departureDate.toString());
			Calendar current = Calendar.getInstance();
			current.setTime(c);
			if (c.after(originalLess.getTime()) && c.before(originalMore.getTime())) {
				Double minPrice = (Double) next.get("MinPrice");
				if (minPrice < min) {
					min = minPrice;
					best = next;
				}
			}
		}
		return best;
	}

	private static JSONArray getQuotes(String when, String placeIdFrom, String placeIdTo, final String withInbound)
			throws UnirestException {
		String inbound = "";
		if (withInbound != null)
			inbound = "?inboundpartialdate=" + withInbound;
		HttpResponse<JsonNode> response = Unirest.get(
				"https://skyscanner-skyscanner-flight-search-v1.p.rapidapi.com/apiservices/browseroutes/v1.0/US/EUR/en-US/"
						+ placeIdFrom + "/" + placeIdTo + "/" + when + inbound)
				.header("x-rapidapi-host", "skyscanner-skyscanner-flight-search-v1.p.rapidapi.com")
				.header("x-rapidapi-key", KEY).asJson();
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return (JSONArray) response.getBody().getObject().get("Quotes");
	}

	private static String getPlaceId(String from) throws UnirestException {
		HttpResponse<JsonNode> getRequest = Unirest.get(
				"https://skyscanner-skyscanner-flight-search-v1.p.rapidapi.com/apiservices/autosuggest/v1.0/UK/EUR/en-GB/?query="
						+ from).header("x-rapidapi-host", "skyscanner-skyscanner-flight-search-v1.p.rapidapi.com")
				.header("x-rapidapi-key", KEY).asJson();

		System.out.println(getRequest);
		JSONArray places = (JSONArray) getRequest.getBody().getObject().get("Places");
		JSONObject place = (JSONObject) places.get(0);
		return (String) place.get("PlaceId");
	}
}
