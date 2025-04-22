// FreeDictionary.java
import java.net.HttpURLConnection;
import java.net.URL;

public class FreeDictionary {
    private static final String URL_TEMPLATE =
        "https://api.dictionaryapi.dev/api/v2/entries/en/%s";

    public boolean contains(String word) {
        try {
            String urlStr = String.format(URL_TEMPLATE, word);
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            return (conn.getResponseCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }
}
