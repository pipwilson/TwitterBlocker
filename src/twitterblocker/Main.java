/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package twitterblocker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import twitter4j.Relationship;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.internal.logging.Logger;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.org.json.JSONObject;

/**
 * Twitter4j references:
 * http://twitter4j.org/en/javadoc-latest/index.html
 * http://twitter4j.org/en/code-examples.html
 * @author pip
 */
public class Main {

    private final static String twitterID = "";
    private final static String twitterPassword = "";
    private static List alreadyChecked;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws TwitterException, IOException, InterruptedException, MalformedURLException, JSONException {

        System.getProperties().setProperty("twitter4j.restBaseURL", "http://api.supertweet.net/1/");
        System.getProperties().setProperty("twitter4j.debug", "true");

        alreadyChecked = new ArrayList<String>();

        // The factory instance is re-useable and thread safe.
        Twitter twitter = new TwitterFactory().getInstance(twitterID, twitterPassword);
        //log.debug("Getting mentions");

        List<Status> statuses = twitter.getMentions();
        Thread.sleep(5000);

        for (Status status : statuses) {
            if (!alreadyChecked.contains(status.getUser().getScreenName())) {
                alreadyChecked.add(status.getUser().getScreenName());
                // if not on friends list and location meets criteria, block!
                if (!isFriend(twitter, status.getUser()) && isOnLocationBlacklist(status.getUser().getLocation(), status.getText())) {
                    // block
                    // twitter4j implements http://dev.twitter.com/doc/post/blocks/create
                    // but superweet implements http://apiwiki.twitter.com/Twitter-REST-API-Method:-blocks%C2%A0create
                    // http://www.supertweet.net/about/api
                    System.out.println("Blocking: " + status.getUser().getName() + ": " + status.getUser().getLocation());
                    twitter.createBlock(status.getUser().getId());
                    Thread.sleep(5000);
                }
            }
        }
    }

    /*
     * Provided one person is following the other, assume they are friends
     */
    private static boolean isFriend(Twitter twitter, User user) throws TwitterException, InterruptedException {
        System.out.println("Is " + user.getScreenName() + " a friend?");
        Relationship relationship = twitter.showFriendship(twitterID, user.getScreenName());
        if (relationship.isSourceFollowingTarget() || relationship.isTargetFollowingSource()) {
            System.out.println("Yes! They are a friend");
            return true;
        }
        System.out.println("No, they are not a friend");
        return false;

    }

    // ideally look the place up and see where it is!
    // http://dbpedia.org/page/Jakarta
    // http://www.semanticoverflow.com/questions/1343/query-information-about-a-country-with-sparql
    // http://dbpedia.org/sparql
    private static boolean isOnLocationBlacklist(String location, String tweet) throws MalformedURLException, IOException, JSONException {

        System.out.println("Checking where this non-friend lives");

        // check language of the tweet
        if (isOnLanguageBlacklist(getCountryLanguage(tweet))) {
            return true;
        } else if (location != null && location.trim().length() > 0) {
            // otherwise, block by placename list
            location = location.toLowerCase();
            System.out.println(location);
            if (location.startsWith("üt") || location.contains("jakarta") || location.contains("indonesia")) {
                System.out.println("location name is on blacklist");
                return true;
            }
        }

        System.out.println("Their location is not on the blacklist");
        return false;
    }

    private static String getCountryLanguage(String textToTranslate) throws MalformedURLException, IOException, JSONException {
        String API_KEY = "ABQIAAAAB1uvGAxdfkZKfA0ECaE2FxSWaKMadP2-vZSoxSiwk0VcrUjI-hSVzfWq9OKe_2CT2xZFrRorMvIc3A";
        String translateThis = URLEncoder.encode(textToTranslate, "UTF-8");
        URL url = new URL(
                "http://ajax.googleapis.com/ajax/services/language/detect?v=1.0&"
                + "q=" + translateThis + "&key=" + API_KEY);
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Referer", "http://philwilson.org/");

        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        JSONObject json = new JSONObject(builder.toString());
        String country = json.getJSONObject("responseData").get("language").toString();
        System.out.println("tweet language is: " + country);
        return country;
    }

    // language list: http://code.google.com/apis/ajaxlanguage/documentation/reference.html
    private static boolean isOnLanguageBlacklist(String countryLanguage) {
        if (countryLanguage.equalsIgnoreCase("id") || countryLanguage.equalsIgnoreCase("su")) {
            return true;
        }

        return false;
    }
}
