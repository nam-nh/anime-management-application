/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hdanimedb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.JOptionPane;

/**
 *
 * @author Nam
 */
public class DBConnection {

    private Connection conn;
    private int episodes, status, duration, members, favorites;
    private double score;
    private PreparedStatement tempStmt;

    public DBConnection() {
        try {
            //Get a connection to database
            // TODO change your own database address
            Class.forName("org.gjt.mm.mysql.Driver");
            String dbURL = "jdbc:mysql://103.255.237.145:3306/sungcaos_animeDB?user=sungcaos_ninomax&password=tKP}jOhcw}zB";
            conn = DriverManager.getConnection(dbURL);
            conn.setAutoCommit(false);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            ex.printStackTrace();
            System.exit(0);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public Vector<Vector<Object>> loadAnimes() {
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        int typeId, status;
        try {
            Statement myStmt = conn.createStatement();
            ResultSet myRs = myStmt.executeQuery("select * from animes order by name");
            while (myRs.next()) {
                Vector<Object> row = new Vector<Object>();
                row.add(myRs.getString("id"));
                row.add(myRs.getString("name"));
                row.add(myRs.getString("mal_link"));
                row.add(myRs.getString("anidb_link"));
                typeId = myRs.getInt("type");
                if (typeId == 0) {
                    row.add("Movie");
                } else {
                    row.add("Series");
                }

                row.add(myRs.getInt("episodes"));

                status = myRs.getInt("status");
                switch (status) {
                    case 0:
                        row.add("Finished Airing");
                        break;
                    case 1:
                        row.add("Currently Airing");
                        break;
                    case 2:
                        row.add("Not yet aired");
                        break;
                }

                row.add(myRs.getString("aired"));
                data.add(row);
            }
            conn.commit();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        } finally {
            return data;
        }
    }

    public String addMALLink(String id, HashMap<String, String> info, int sub) {
        try {
            if (!animeExist(id)) {
                PreparedStatement myStmt = conn.prepareStatement("insert into animes values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                myStmt.setString(1, id);
                myStmt.setString(2, info.get("Title"));
                myStmt.setString(3, info.get("MALUrl"));
                myStmt.setString(4, info.get("AnidbUrl"));
                myStmt.setString(5, info.get("English"));
                myStmt.setString(6, info.get("Synonyms"));
                myStmt.setInt(7, sub);
                if (info.get("Episodes").equals("Unknown")) {
                    episodes = 0;
                } else {
                    episodes = Integer.parseInt(info.get("Episodes"));
                }
                myStmt.setInt(9, episodes);

                if (episodes == 1 || info.get("Type").equals("Movie")) {
                    myStmt.setInt(8, 0);
                } else {
                    myStmt.setInt(8, 1);
                }

                if (info.get("Status").equals("Finished Airing")) {
                    status = 0;
                } else if (info.get("Status").equals("Currently Airing")) {
                    status = 1;
                } else {
                    status = 2;
                }
                myStmt.setInt(10, status);
                myStmt.setString(11, info.get("Aired"));
                myStmt.setString(12, info.get("Premiered"));
                myStmt.setString(13, info.get("Broadcast"));
                myStmt.setString(14, info.get("Source"));
                try {
                    String dur = info.get("Duration");
                    if (dur.contains("hr.")) {
                        int hour = Integer.parseInt(dur.split(" hr.")[0]);
                        int min = 0;
                        if (dur.contains("min")) {
                            min = Integer.parseInt(dur.split(" hr. ")[1].split(" min")[0]);
                        }

                        duration = hour * 60 + min;
                    } else {
                        duration = Integer.parseInt(dur.split(" min")[0]);
                    }
                } catch (Exception e) {
                    duration = 0;
                }
                myStmt.setInt(15, duration);
                myStmt.setString(16, info.get("Rating"));

                try {
                    score = Double.parseDouble(info.get("Score"));
                } catch (Exception e) {
                    score = 0;
                }
                myStmt.setDouble(17, score);
                myStmt.setString(18, info.get("Ranked"));
                myStmt.setString(19, info.get("Popularity"));
                try {
                    members = Integer.parseInt(info.get("Members").replace(",", ""));
                } catch (Exception e) {
                    members = 0;
                }
                myStmt.setInt(20, members);
                try {
                    favorites = Integer.parseInt(info.get("Favorites").replace(",", ""));
                } catch (Exception e) {
                    favorites = 0;
                }
                myStmt.setInt(21, favorites);

                myStmt.execute();

                String[] producers = info.get("Producers").split(", ");
                String[] licensors = info.get("Licensors").split(", ");
                String[] studios = info.get("Studios").split(", ");
                String[] genres = info.get("Genres").split(", ");

                for (String producer : producers) {
                    tempStmt = conn.prepareStatement("CALL spAddProducer (?,?)");
                    tempStmt.setString(1, id);
                    tempStmt.setString(2, producer);
                    tempStmt.execute();
                }

                for (String licensor : licensors) {
                    tempStmt = conn.prepareStatement("CALL spAddLicensor (?,?)");
                    tempStmt.setString(1, id);
                    tempStmt.setString(2, licensor);
                    tempStmt.execute();
                }

                for (String studio : studios) {
                    tempStmt = conn.prepareStatement("CALL spAddStudio (?,?)");
                    tempStmt.setString(1, id);
                    tempStmt.setString(2, studio);
                    tempStmt.execute();
                }

                for (String genre : genres) {
                    tempStmt = conn.prepareStatement("CALL spAddGenre (?,?)");
                    tempStmt.setString(1, id);
                    tempStmt.setString(2, genre);
                    tempStmt.execute();
                }

                conn.commit();
                return info.get("Title") + ": Added!";
            } else {
                conn.rollback();
                return info.get("Title") + ": Duplicate!";
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            ex.printStackTrace();
            return "Error!";
        }
    }

    public boolean animeExist(String id) {
        try {
            PreparedStatement myStmt = conn.prepareStatement("select id from animes "
                    + "where id = ?");
            //Set the parameters
            myStmt.setString(1, id);
            //Execute SQL query
            ResultSet myRs = myStmt.executeQuery();
            return myRs.next();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public void removeAnime(String id) {
        try {
            tempStmt = conn.prepareStatement("delete from animes "
                    + "where id = ?");
            //Set the parameters
            tempStmt.setString(1, id);
            //Execute SQL query
            tempStmt.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            ex.printStackTrace();

        }
    }

    public Vector<Vector<Object>> searchAnimes(String id, String name, String malLink, String anidbLink, int type, int audio) {
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        int typeId, status;
        String typeQuery = " and 3 = ?";
        String audioQuery = " and 3 = ?";
        String idQuery = "'' = ?";
        String nameQuery = " and '' = ?";
        String malLinkQuery = " and '' = ?";
        String anidbLinkQuery = " and '' = ?";
        if (type < 3) {
            typeQuery = " and type = ?";
        }

        if (audio < 3) {
            audioQuery = " and audios = ?";
        }

        if (!id.equals("")) {
            if (id.contains("-")) {
                idQuery = "id = ?";
            } else {
                idQuery = "id like ?";
            }
        }

        if (!name.equals("")) {
            nameQuery = " and name like ?";
        }

        if (!malLink.equals("")) {
            malLinkQuery = " and id like ?";
        }

        if (!anidbLink.equals("")) {
            anidbLinkQuery = " and anidb_link like ?";
        }
        String query = "Select * from animes where " + idQuery + nameQuery + malLinkQuery + anidbLinkQuery + typeQuery + audioQuery;
        System.out.println(query);
        try {
            PreparedStatement myStmt = conn.prepareStatement(query);
            if (!id.equals("")) {
                if (id.contains("-")) {
                    myStmt.setString(1, id);
                } else {
                    myStmt.setString(1, id + "-_");
                }
            } else {
                myStmt.setString(1, id);
            }
            
            if(!name.equals("")) {
                myStmt.setString(2, "%" + name + "%");
            } else {
                myStmt.setString(2, name);
            }
            
            if (!malLink.equals("")) {
                myStmt.setString(3, malLink + "-_");
            } else {
                myStmt.setString(3, malLink);
            }
            
            if (!anidbLink.equals("")) {
                myStmt.setString(4, "%" + anidbLink);
            } else {
                myStmt.setString(4, anidbLink);
            }
           
            myStmt.setInt(5, type);
            myStmt.setInt(6, audio);

            ResultSet myRs = myStmt.executeQuery();
            while (myRs.next()) {
                Vector<Object> row = new Vector<Object>();
                row.add(myRs.getString("id"));
                row.add(myRs.getString("name"));
                row.add(myRs.getString("mal_link"));
                row.add(myRs.getString("anidb_link"));
                typeId = myRs.getInt("type");
                if (typeId == 0) {
                    row.add("Movie");
                } else {
                    row.add("Series");
                }

                row.add(myRs.getInt("episodes"));

                status = myRs.getInt("status");
                switch (status) {
                    case 0:
                        row.add("Finished Airing");
                        break;
                    case 1:
                        row.add("Currently Airing");
                        break;
                    case 2:
                        row.add("Not yet aired");
                        break;
                }

                row.add(myRs.getString("aired"));
                data.add(row);
            }
            conn.commit();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        } finally {
            return data;
        }
    }

    public HashMap<String, String> searchAnimesById(String id) {
        HashMap<String, String> info = new HashMap<String, String>();
        try {
            PreparedStatement myStmt = conn.prepareStatement("select * from animes "
                    + "where id = ?");
            myStmt.setString(1, id);
            ResultSet myRs = myStmt.executeQuery();
            while (myRs.next()) {
                info.put("ID", myRs.getString("id"));
                info.put("Title", myRs.getString("name"));
                info.put("MAL Link", myRs.getString("mal_link"));
                info.put("Anidb Link", myRs.getString("anidb_link"));
                info.put("English", myRs.getString("eng"));
                info.put("Synonyms", myRs.getString("synonyms"));
                info.put("Audio", String.valueOf(myRs.getInt("audios")));
                info.put("Type", String.valueOf(myRs.getInt("type")));
                info.put("Episodes", myRs.getString("episodes"));
                info.put("Status", String.valueOf(myRs.getInt("status")));
                info.put("Duration", String.valueOf(myRs.getInt("duration")));
                info.put("Aired", myRs.getString("aired"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        } finally {
            return info;
        }
    }

    public void editAnime(String selectedId, String newTitle, String newMALLink, String newAnidbLink, String newEng, String newSyn, int newType, int newAudio, int newEpisodes, int newDuration, int newStatus, String newAired) {
        try {
            PreparedStatement myStmt = conn.prepareStatement("update animes set name = ?, mal_link = ?, anidb_link = ?, eng = ?, synonyms = ?, audios =?, type = ?, episodes = ?, duration = ?, status = ?, aired = ? "
                    + "where id = ?");
            myStmt.setString(1, newTitle);
            myStmt.setString(2, newMALLink);
            myStmt.setString(3, newAnidbLink);
            myStmt.setString(4, newEng);
            myStmt.setString(5, newSyn);
            myStmt.setInt(6, newType);
            myStmt.setInt(7, newAudio);
            myStmt.setInt(8, newEpisodes);
            myStmt.setInt(9, newDuration);
            myStmt.setInt(10, newStatus);
            myStmt.setString(11, newAired);
            myStmt.setString(12, selectedId);

            myStmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        }
    }

    public Vector<Vector<Object>> loadEpisodes(String animeId) {
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        try {
            PreparedStatement myStmt = conn.prepareStatement("select * from anime_episodes "
                    + "where anime_id = ? order by orderr");
            myStmt.setString(1, animeId);
            ResultSet myRs = myStmt.executeQuery();
            while (myRs.next()) {
                Vector<Object> row = new Vector<Object>();
                row.add(String.valueOf(myRs.getInt("id")));
                row.add(myRs.getString("episode_name"));
                row.add(myRs.getString("backup_link"));
                row.add(myRs.getString("drive_link"));
                row.add(myRs.getString("gg_cloud_link"));
                row.add(myRs.getString("link_3"));
                row.add(myRs.getString("link_4"));
                data.add(row);
            }
            conn.commit();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();

        }

        return data;
    }

    public void removeEpisode(String epId) {
        try {
            tempStmt = conn.prepareStatement("delete from anime_episodes "
                    + "where id = ?");
            //Set the parameters
            tempStmt.setString(1, epId);
            //Execute SQL query
            tempStmt.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            ex.printStackTrace();

        }
    }

    public String addEpisode(String animeId, int order, String episodeName, String backupLink, String driveLink, String ggcloudLink, String link3, String link4) {
        if (!epExist(animeId, episodeName, order)) {
            try {
                PreparedStatement myStmt = conn.prepareStatement("insert into anime_episodes(anime_id, orderr, episode_name, backup_link, drive_link, gg_cloud_link, link_3, link_4) values (?, ?, ?, ?, ?, ?, ?, ?)");
                myStmt.setString(1, animeId);
                myStmt.setInt(2, order);
                myStmt.setString(3, episodeName);
                myStmt.setString(4, backupLink);
                myStmt.setString(5, driveLink);
                myStmt.setString(6, ggcloudLink);
                myStmt.setString(7, link3);
                myStmt.setString(8, link4);

                myStmt.executeUpdate();

                conn.commit();
                return episodeName + " :Added!\n";
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                ex.printStackTrace();
                return "";
            }
        } else {
            return episodeName + " : Duplicated!\n";
        }
    }

    public void editEpisode(int episodeId, int order, String episodeName, String backupLink, String driveLink, String ggcloudLink, String link3, String link4) {
        try {
            PreparedStatement myStmt = conn.prepareStatement("update anime_episodes set"
                    + " orderr = ?,"
                    + " episode_name = ?,"
                    + " backup_link = ?,"
                    + " drive_link = ?,"
                    + " gg_cloud_link = ?,"
                    + " link_3 = ?,"
                    + " link_4 = ?"
                    + " where id = ?");
            myStmt.setInt(1, order);
            myStmt.setString(2, episodeName);
            myStmt.setString(3, backupLink);
            myStmt.setString(4, driveLink);
            myStmt.setString(5, ggcloudLink);
            myStmt.setString(6, link3);
            myStmt.setString(7, link4);
            myStmt.setInt(8, episodeId);

            myStmt.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            ex.printStackTrace();

        }
    }

    public HashMap<String, String> searchEpisodeById(String id) {
        HashMap<String, String> info = new HashMap<String, String>();
        try {
            PreparedStatement myStmt = conn.prepareStatement("select * from anime_episodes "
                    + "where id = ?");
            myStmt.setString(1, id);
            ResultSet myRs = myStmt.executeQuery();
            while (myRs.next()) {
                info.put("order", String.valueOf(myRs.getInt("orderr")));
                info.put("episode_name", myRs.getString("episode_name"));
                info.put("backup_link", myRs.getString("backup_link"));
                info.put("drive_link", myRs.getString("drive_link"));
                info.put("gg_cloud_link", myRs.getString("gg_cloud_link"));
                info.put("link_3", myRs.getString("link_3"));
                info.put("link_4", myRs.getString("link_4"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        } finally {
            return info;
        }
    }

    public boolean epExist(String animeId, String epName, int order) {
        try {
            PreparedStatement myStmt = conn.prepareStatement("select id from anime_episodes "
                    + "where anime_id = ? and (episode_name like ? or [orderr] = ?)");
            //Set the parameters
            myStmt.setString(1, animeId);
            myStmt.setString(2, epName);
            myStmt.setInt(3, order);
            //Execute SQL query
            ResultSet myRs = myStmt.executeQuery();
            return myRs.next();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

}
