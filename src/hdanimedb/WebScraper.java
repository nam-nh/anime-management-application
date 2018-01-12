/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hdanimedb;

import java.io.IOException;
import java.util.HashMap;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author Nam's PC
 */
public class WebScraper {
    private HashMap<String,String> info;
    Connection.Response response = null;
    Document doc;
    Elements tempElement, tempElement1;
    private String title, engTitle, synonymsTitle, type, episodes, status, aired;
    private String premiered, broadcast, producers, licensors, studios, source;
    private String genres, duration, rating, score, ranked, popularity, members, favorites;
    private String tempString, episodesList;
    public HashMap<String,String> getMalInfo(String malUrl, String anidb){
        info = new HashMap<String, String>();
        try {
            response = Jsoup.connect(malUrl)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                    .timeout(30000)
                    .execute();
            doc = response.parse();
            tempElement = doc.select("#contentWrapper > div:nth-child(1) > h1 > span");
            title = tempElement.text();
            System.out.println("Title: " + title);
            info.put("Title", title);

            tempElement = doc.select("#content > table > tbody > tr > td.borderClass > div > div");
            for (int i = 0; i < tempElement.size(); i++){
                tempString = tempElement.get(i).text();
                if (tempString.contains("English:")){
                    engTitle = tempString.split("English: ")[1];
                    System.out.println("English: " + engTitle);
                } else if (tempString.contains("Synonyms:")){
                    synonymsTitle = tempString.split("Synonyms: ")[1];
                    System.out.println("Synonyms: " + synonymsTitle);
                } else if (tempString.contains("Type:")){
                    type = tempString.split("Type: ")[1];
                    System.out.println("Type: " + type);
                } else if (tempString.contains("Episodes:")){
                    episodes = tempString.split("Episodes: ")[1];
                    System.out.println("Episodes: " + episodes);
                } else if (tempString.contains("Status:") && !tempString.contains("WatchingCompletedOn")){
                    status = tempString.split("Status: ")[1];
                } else if (tempString.contains("Aired:")){
                    aired = tempString.split("Aired: ")[1];
                } else if (tempString.contains("Premiered:")){
                    premiered = tempString.split("Premiered: ")[1];
                } else if (tempString.contains("Broadcast:")){
                    broadcast = tempString.split("Broadcast: ")[1];
                } else if (tempString.contains("Producers:")){
                    if(tempString.contains("None found")){
                        producers = "None found";
                    } else {
                        producers = tempString.split("Producers: ")[1];
                    }
                } else if (tempString.contains("Licensors:")){
                    if(tempString.contains("None found")){
                        licensors = "None found";
                    } else {
                        licensors = tempString.split("Licensors: ")[1];
                    }
                } else if (tempString.contains("Studios:")){
                    studios = tempString.split("Studios: ")[1];
                } else if (tempString.contains("Source:")){
                    source = tempString.split("Source: ")[1];
                } else if (tempString.contains("Genres:")){
                    genres = tempString.split("Genres: ")[1];
                } else if (tempString.contains("Duration:")){
                    duration = tempString.split("Duration: ")[1];
                } else if (tempString.contains("Rating:")){
                    rating = tempString.split("Rating: ")[1];
                } else if (tempString.contains("Score:") && !tempString.contains("Masterpiece")){
                    score = tempString.split("Score: ")[1].split(" ")[0];
                } else if (tempString.contains("Ranked:")){
                    ranked = tempString.split("Ranked: ")[1].split(" ")[0];
                } else if (tempString.contains("Popularity:")){
                    popularity = tempString.split("Popularity: ")[1];
                } else if (tempString.contains("Members:")){
                    members = tempString.split("Members: ")[1];
                } else if (tempString.contains("Favorites:")){
                    favorites = tempString.split("Favorites: ")[1];
                }
                
                
                
                
            }
            
            //get episodes infomation
            episodesList = "";
            if(anidb.length() > 2) {
                response = Jsoup.connect(anidb)
                        .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                        .timeout(30000)
                        .execute();
                doc = response.parse();
                tempElement = doc.select("#eplist > tbody");
                tempElement = tempElement.select("tr");
                
                for (int i=0; i<tempElement.size(); i++) {
                    tempElement1 = tempElement.get(i).select("td");

                    try{
                        int orderEp = Integer.parseInt(tempElement1.get(0).text());
                        if(orderEp <10){
                            episodesList += "Episode 0" + tempElement1.get(0).text() + ": " + tempElement1.get(1).text() + "&&&&&" + tempElement1.get(0).text() + "~~~";
                        } else {
                            episodesList += "Episode " + tempElement1.get(0).text() + ": " + tempElement1.get(1).text() + "&&&&&" + tempElement1.get(0).text() + "~~~";
                        }
                    } catch(NumberFormatException e){
                        System.out.println("Episode Special not added!");
                    }
                };
            }
            
            // put info to hashmap
            info.put("MALUrl", malUrl);
            info.put("AnidbUrl", anidb);
            info.put("Title",title);
            info.put("English",engTitle);
            info.put("Synonyms",synonymsTitle);
            info.put("Type",type);
            info.put("Episodes",episodes);
            info.put("Status",status);
            info.put("Aired",aired);
            info.put("Premiered",premiered);
            info.put("Broadcast",broadcast);
            info.put("Producers",producers);
            info.put("Licensors",licensors);
            info.put("Studios",studios);
            info.put("Source",source);
            info.put("Genres",genres);
            info.put("Duration",duration);
            info.put("Rating",rating);
            info.put("Score",score);
            info.put("Ranked",ranked);
            info.put("Popularity",popularity);
            info.put("Members",members);
            info.put("Favorites",favorites);
            info.put("Episode List", episodesList);
            
        } catch (IOException e) {
            System.out.println("io - "+e);
        }
                
        return info;
    }
}
