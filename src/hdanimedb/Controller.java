/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package hdanimedb;

import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;

/**
 *
 * @author Nam's PC
 */
public class Controller {
    
    private final GUI gui;
    private final WebScraper webScraper;
    private final DBConnection dbConnection;
    private EpisodesList episodesListDialog;
    private InputView inputView;
    private EditView editView;
    private EditEpView editEpView;
    private AddEpisodeView addEpView;
    private String malLink, anidbLink, bothUrl, selectedId, selectedEpId,
            selectedTitle, selectedMAL, selectedAnidb, newTitle, newMALLink,
            newAnidbLink, newEng, newSyn, newAired, newEpisodeName,
            newBackupLink, newDriveLink, newGgCloudLink, newLink3, newLink4,
            message = "";
    private int audio, progress = 0, newAudio, newType, newEpisodes,
            newDuration, newStatus, newOrder;
    private Vector<Vector<Object>> animeData, searchResult, episodesData;
    
    public Controller(GUI gui, DBConnection dbConnection) {
        
        // create web scraper instance
        webScraper = new WebScraper();
        
        // create user view
        this.gui = gui;
        
        // create database connection
        this.dbConnection = dbConnection;
        
        gui.setVisible(true);
        
        // load animes from database
        updateTable();
        
        // set add anime bttn listener
        gui.addAddBtnListener((ActionEvent e) -> {
            addAnime();
        });
        
        // set search anime button listener
        gui.addSearchBtnListener((ActionEvent e) -> {
            searchAnime();
        });
        
        // event when choose 1 row on table
        gui.tableListener((ListSelectionEvent event) -> {
            if (!event.getValueIsAdjusting()
                    && gui.getTable().getSelectedRow() != -1) {
                //enable remove and edit button
                gui.setEditBtnEnable(true);
                //get data in selected row
                selectedId = gui.getTable()
                        .getValueAt(gui.getTable().getSelectedRow(), 0)
                        .toString();
                
            }
        });
        
        // edit anime button listener
        gui.addEditBtnListener((ActionEvent e) -> {
            editAnime();
        });
        
        // remove anime button listener
        gui.addRemoveBtnListener((ActionEvent e) -> {
            removeAnime();
        });
        
        //event when double click on row in table
        gui.tableDoubleClickListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2) {
                    System.out.println("ding");
                    // get information of selected anime
                    selectedId = gui.getTable().getValueAt(row, 0).toString();
                    selectedTitle = gui.getTable().getValueAt(row, 1).toString();
                    selectedMAL = gui.getTable().getValueAt(row, 2).toString();
                    selectedAnidb = gui.getTable().getValueAt(row, 3).toString();
                    episodesListDialog = new EpisodesList();
                    
                    // set infomation of selected anime
                    episodesListDialog.setID(selectedId);
                    episodesListDialog.setAnimeName(selectedTitle);
                    episodesListDialog.setMalLink(selectedMAL);
                    episodesListDialog.setAnidbLink(selectedAnidb);
                    
                    String audioType
                            = (selectedId.contains("-0")) ? "SUB" : "DUB";
                    episodesListDialog.setTitle(selectedTitle
                            + " - " + audioType);
                    
                    updateEpTable();
                    
                    episodesListDialog.setVisible(true);
                    episodesListDialog.tableListener((ListSelectionEvent event) -> {
                        if (!event.getValueIsAdjusting()
                                && episodesListDialog.getTable().getSelectedRow() != -1) {
                            //enable remove and edit button
                            episodesListDialog.setEditBtnEnable(true);
                            
                            //get data in selected row
                            selectedEpId = episodesListDialog.getTable()
                                    .getValueAt(episodesListDialog.getTable().getSelectedRow(), 0)
                                    .toString();
                            
                        }
                    });
                    
                    //edit episode button listener
                    episodesListDialog.addEditBtnListener((ActionEvent e) -> {
                        editEpisode();
                    });
                    
                    // remove episode button listener
                    episodesListDialog.addRemoveBtnListener((ActionEvent e) -> {
                        removeEpisode();
                    });
                    
                    episodesListDialog.addAddBtnListener((ActionEvent e) -> {
                        
                        addEpisode();
                        
                    });
                }
            }
        });
        
    }
    
    public void updateTable() {
        animeData = dbConnection.loadAnimes();
        gui.setTable(animeData);
    }
    
    public void searchAnime() {
        // search option: 0. by ID, 1. by Title, 2. by MAL link,
        // 3. by anidb link, 4. by type, 5. by audio
        int searchOption = gui.getSearchOption();
        String textValue = gui.getSearchField();
        
        // get search result
        searchResult = getSearchAnimeResult(searchOption, textValue);
        
        // update gui table
        gui.setTable(searchResult);
    }
    
    public void addAnime() {
        inputView = new InputView();
        inputView.setEnableField(true);
        inputView.setVisible(true);
        inputView.addStart1BtnListener((ActionEvent e1) -> {
            addSingleAnime();
        });
        
        inputView.addStart2BtnListener((ActionEvent e1) -> {
            addMultipleAnimes();
        });
    }
    
    public void addSingleAnime() {
        boolean condition1 = true;
        inputView.setEnableField(false);
        anidbLink = inputView.getAnidbUrl();
        if (anidbLink.length() < 1){
            int result = JOptionPane.showConfirmDialog(null, "Episodes won't be imported if you don't provide anidb link", "Confirmation!", JOptionPane.OK_CANCEL_OPTION);
            if(result == 0) {
                condition1 = true;
            }else {
                inputView.setEnableField(true);
                condition1 = false;
            }
        }
        if(condition1) {
            malLink = inputView.getMalUrl();
            audio = inputView.getAudioType();
            Thread addingThread = new Thread(() -> {
                if (audio != 2) {
                    
                    // add only sub or dub version
                    HashMap<String, String> info
                            = webScraper.getMalInfo(malLink, anidbLink);
                    try {
                        String id = malLink.split("/")[4] + "-"
                                + String.valueOf(audio);
                        dbConnection.addMALLink(id, info, audio);
                        if(audio == 1) {
                            int result1 = JOptionPane.showConfirmDialog(null, "Importing Episode for DUB version?", "Confirmation!", JOptionPane.YES_NO_CANCEL_OPTION);
                            switch (result1) {
                                case 0:
                                    String episodeInfo = info.get("Episode List");
                                    String[] episodeList = episodeInfo.split("~~~");
                                    if(episodeList.length > 1) {
                                        for (String episodeList1 : episodeList) {
                                            int epOrder = Integer.parseInt(episodeList1.split("&&&&&")[1]);
                                            String episodeName = episodeList1.split("&&&&&")[0];
                                            dbConnection.addEpisode(id, epOrder, episodeName, "", "", "", "", "");
                                        }
                                    }   break;
                                case 1:
                                    break;
                                default:
                                    inputView.setEnableField(true);
                                    break;
                            }
                        } else {
                            String episodeInfo = info.get("Episode List");
                            String[] episodeList = episodeInfo.split("~~~");
                            
                            if(episodeList.length > 1) {
                                for (String episodeList1 : episodeList) {
                                    int epOrder = Integer.parseInt(episodeList1.split("&&&&&")[1]);
                                    String episodeName = episodeList1.split("&&&&&")[0];
                                    dbConnection.addEpisode(id, epOrder, episodeName, "", "", "", "", "");
                                }
                            }
                        }
                        
                        
                        updateTable();
                        
                        JOptionPane.showMessageDialog(null,
                                "Added anime: " + info.get("Title") + "!");
                        inputView.dispose();
                    } catch (HeadlessException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Wrong MAL link!");
                        inputView.setEnableField(true);
                    }
                } else {
                    // for add both sub and dub
                    HashMap<String, String> info
                            = webScraper.getMalInfo(malLink, anidbLink);
                    try {
                        String id1 = malLink.split("/")[4] + "-0";
                        String id2 = malLink.split("/")[4] + "-1";
                        
                        dbConnection.addMALLink(id1, info, 0);
                        dbConnection.addMALLink(id2, info, 1);
                        String episodeInfo = info.get("Episode List");
                        String[] episodeList = episodeInfo.split("~~~");
                        
                        int result1 = JOptionPane.showConfirmDialog(null, "Importing Episode for DUB version?", "Confirmation!", JOptionPane.YES_NO_CANCEL_OPTION);
                            switch (result1) {
                                case 0:
                                    if(episodeList.length > 1) {
                                        for (String episodeList1 : episodeList) {
                                            int epOrder = Integer.parseInt(episodeList1.split("&&&&&")[1]);
                                            String episodeName = episodeList1.split("&&&&&")[0];
                                            dbConnection.addEpisode(id1, epOrder, episodeName, "", "", "", "", "");
                                            dbConnection.addEpisode(id2, epOrder, episodeName, "", "", "", "", "");
                                        }
                                    }
                                    
                                    // refresh anime table
                                    updateTable();
                                    
                                    JOptionPane.showMessageDialog(null,
                                            "Added anime: " + info.get("Title") + "!");
                                    inputView.dispose();
                                    break;
                                case 1:
                                    if(episodeList.length > 1) {
                                        for (String episodeList1 : episodeList) {
                                            int epOrder = Integer.parseInt(episodeList1.split("&&&&&")[1]);
                                            String episodeName = episodeList1.split("&&&&&")[0];
                                            dbConnection.addEpisode(id1, epOrder, episodeName, "", "", "", "", "");
                                        }
                                    }
                                    
                                    // refresh anime table
                                    updateTable();
                                    
                                    JOptionPane.showMessageDialog(null,
                                            "Added anime: " + info.get("Title") + "!");
                                    inputView.dispose();
                                    break;
                                default:
                                    inputView.setEnableField(true);
                                    break;
                            }
                        
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Wrong MAL link!");
                        inputView.setEnableField(true);
                    }
                }
            });
            addingThread.start();
        }
    }
    
    public void addMultipleAnimes() {
        Thread addAnimesThread = new Thread(() -> {
            message = "";
            inputView.setEnableField(false);
            bothUrl = inputView.getBothLink();
            audio = inputView.getAudiosType();
            
            //split string in text area
            String[] rows = bothUrl.split("\n");
            Thread[] addingThreads = new Thread[rows.length];
            for (int i = 0; i < rows.length; i++) {
                String tempMalLink = "";
                String tempAnidbLink = "";
                
                // check where are mal link and anidb link
                if (rows[i].split("\t")[0].contains("myanimelist.net") && (rows[i].split("\t")[1].contains("anidb.net") || (rows[i].split("\t")[1].replace(" ", "")).equals(""))) {
                    tempMalLink = rows[i].split("\t")[0];
                    try {
                        tempAnidbLink = rows[i].split("\t")[1];
                    } catch (Exception err) {
                        tempAnidbLink = "";
                    }
                    
                } else if (rows[i].split("\t")[1].contains("myanimelist.net") && (rows[i].split("\t")[0].contains("anidb.net") || (rows[i].split("\t")[0].replace(" ", "")).equals(""))) {
                    tempMalLink = rows[i].split("\t")[1];
                    tempAnidbLink = rows[i].split("\t")[0];
                }
                
                final String malUrlFinal = tempMalLink;
                HashMap<String, String> info = webScraper.getMalInfo(tempMalLink, tempAnidbLink);
                Thread addingThread = new Thread(() -> {
                    if (audio != 2) {
                        
                        // add only sub or dub version
                        try {
                            String id = malUrlFinal.split("/")[4] + "-" + String.valueOf(audio);
                            String m = dbConnection.addMALLink(id, info, audio);
                            /*
                            //get episodes info
                            String episodeInfo = info.get("Episode List");
                            String[] episodeList = episodeInfo.split("~~~");
                            if(episodeList.length > 1) {
                                for (String episodeList1 : episodeList) {
                                    int epOrder = Integer.parseInt(episodeList1.split("&&&&&")[1]);
                                    String episodeName = episodeList1.split("&&&&&")[0];
                                    dbConnection.addEpisode(id, epOrder, episodeName, "", "", "", "", "");
                                }
                            }*/
                            message = message + m + "\n";
                            // refresh anime table
                            updateTable();
                            
                            // update progress bar
                            upProgress((int) 100 / rows.length);
                            inputView.setProgressBar(progress);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        
                        // add both sub and dub versions
                        try {
                            String id1 = malUrlFinal.split("/")[4] + "-0";
                            String id2 = malUrlFinal.split("/")[4] + "-1";
                            
                            String m = dbConnection.addMALLink(id1, info, 0);
                            
                            dbConnection.addMALLink(id2, info, 1);
                            
                            //get episodes info
                            String episodeInfo = info.get("Episode List");
                            String[] episodeList = episodeInfo.split("~~~");
                            if(episodeList.length > 1) {
                                for (String episodeList1 : episodeList) {
                                    int epOrder = Integer.parseInt(episodeList1.split("&&&&&")[1]);
                                    String episodeName = episodeList1.split("&&&&&")[0];
                                    dbConnection.addEpisode(id1, epOrder, episodeName, "", "", "", "", "");
                                    dbConnection.addEpisode(id2, epOrder, episodeName, "", "", "", "", "");
                                }
                            }
                            
                            message = message + m + " - SUB & DUB + \n";
                            //update progress bar
                            upProgress((int) 100 / rows.length);
                            inputView.setProgressBar(progress);
                            
                            //refresh anime table
                            updateTable();
                        } catch (Exception e) {
                            e.printStackTrace();
                            message = message + "Wrong MAL Link!\n";
                            inputView.setEnableField(true);
                        }
                    }
                });
                
                addingThreads[i] = addingThread;
                
                addingThread.start();
                
            }
            
            // waiting for all threads completed
            for (Thread thread : addingThreads) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            // set the progress to 100%
            upProgress(100);
            inputView.setProgressBar(progress);
            
            // update the table
            updateTable();
            
            JOptionPane.showMessageDialog(null, message);
            inputView.dispose();
        });
        addAnimesThread.start();
    }
    
    //update the progress values from multiple threads
    public synchronized void upProgress(int value) {
        if (value != 100) {
            if (value != 0) {
                progress = progress + value;
            } else {
                progress = 0;
            }
        } else {
            progress = 100;
        }
    }
    
    public void editAnime() {
        
        editView = new EditView();
        
        // set the old values on edit view
        HashMap<String, String> selectedAnimeInfo
                = dbConnection.searchAnimesById(selectedId);
        editView.setID(selectedAnimeInfo.get("ID"));
        editView.setAnimeTitle(selectedAnimeInfo.get("Title"));
        editView.setMalLink(selectedAnimeInfo.get("MAL Link"));
        editView.setAnidbLink(selectedAnimeInfo.get("Anidb Link"));
        editView.setEnglish(selectedAnimeInfo.get("English"));
        editView.setSynonyms(selectedAnimeInfo.get("Synonyms"));
        editView.setAnimeType(Integer.parseInt(selectedAnimeInfo.get("Type")));
        editView.setAudio(Integer.parseInt(selectedAnimeInfo.get("Audio")));
        editView.setEpisodes(selectedAnimeInfo.get("Episodes"));
        editView.setDuration(selectedAnimeInfo.get("Duration"));
        editView.setStatus(Integer.parseInt(selectedAnimeInfo.get("Status")));
        editView.setAired(selectedAnimeInfo.get("Aired"));
        
        editView.setVisible(true);
        
        // save changed button listener
        editView.addSaveBtnListener((ActionEvent e) -> {
            saveAnime();
        });
        
        // cancel button listener
        editView.addCancelBtnListener((ActionEvent e) -> {
            editView.dispose();
        });
    }
    
    public void saveAnime() {
        // get new values
        newTitle = editView.getAnimeTitle();
        newMALLink = editView.getMalLink();
        newAnidbLink = editView.getAnidbLink();
        newEng = editView.getEnglish();
        newSyn = editView.getSynonyms();
        newType = editView.getAnimeType();
        newAudio = editView.getAudio();
        newEpisodes = editView.getEpisodes();
        newDuration = editView.getDuration();
        newStatus = editView.getStatus();
        newAired = editView.getAired();
        
        //confirm dialog
        int result = JOptionPane.showConfirmDialog(null, "Save(Y/N)?");
        if (result == JOptionPane.YES_OPTION) {
            dbConnection.editAnime(selectedId, newTitle, newMALLink, newAnidbLink, newEng, newSyn, newType, newAudio, newEpisodes, newDuration, newStatus, newAired);
            editView.dispose();
            
            updateTable();
        } else if (result == JOptionPane.NO_OPTION) {
            editView.dispose();
        }
        
    }
    
    public void removeAnime() {
        
        dbConnection.removeAnime(selectedId);
        updateTable();
        
    }
    
    /**
     * Search animes on database with search option and text Search option: 0.
     * by ID; 1. by Title; 2. by MAL link; 3. by anidb link; 4. by type; 5. by
     * audio.
     *
     * @param searchOption
     * @param textValue
     * @return searchResult
     */
    public Vector<Vector<Object>> getSearchAnimeResult(int searchOption,
            String textValue) {
        switch (searchOption) {
            case 0:
                searchResult = dbConnection.searchAnimes(textValue, "", "", "", 3, 3);
                break;
            case 1:
                searchResult = dbConnection.searchAnimes("", textValue, "", "", 3, 3);
                break;
            case 2:
                
                // try to search by id on mal link
                try {
                    searchResult = dbConnection.searchAnimes("", "", textValue.split("/")[4], "", 3, 3);
                } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    searchResult = dbConnection.searchAnimes("", "", textValue, "", 3, 3);
                }
                break;
            case 3:
                
                //try to search by id on anidb link
                try {
                    searchResult = dbConnection.searchAnimes("", "", "", textValue.split("=")[2], 3, 3);
                } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    searchResult = dbConnection.searchAnimes("", "", "", textValue, 3, 3);
                }
                break;
            case 5:
                
                // notice that only 2 option in search by audio
                if (textValue.toLowerCase().equals("sub")) {
                    searchResult = dbConnection.searchAnimes("", "", "", "", 3, 0);
                } else if (textValue.toLowerCase().equals("dub")) {
                    searchResult = dbConnection.searchAnimes("", "", "", "", 3, 1);
                } else {
                    JOptionPane.showMessageDialog(null, "Audio must be 'sub' or 'dub'");
                    searchResult = animeData;
                }
                break;
            case 4:
                
                // notice that only 2 option in search by type
                if (textValue.toLowerCase().equals("movie")) {
                    searchResult = dbConnection.searchAnimes("", "", "", "", 0, 3);
                } else if (textValue.toLowerCase().equals("series")) {
                    searchResult = dbConnection.searchAnimes("", "", "", "", 1, 3);
                } else {
                    JOptionPane.showMessageDialog(null, "Type must be 'movie' or 'series'");
                    searchResult = animeData;
                }
                break;
            default:
                break;
        }
        
        return searchResult;
    }
    
    public void updateEpTable() {
        episodesData = dbConnection.loadEpisodes(selectedId);
        episodesListDialog.setTable(episodesData);
    }
    
    public void removeEpisode() {
        
        dbConnection.removeEpisode(selectedEpId);
        updateEpTable();
        
    }
    
    public void editEpisode() {
        
        editEpView = new EditEpView();
        
        // set the old values on edit view
        HashMap<String, String> selectedEpInfo
                = dbConnection.searchEpisodeById(selectedEpId);
        editEpView.setId(selectedEpId);
        editEpView.setAnimeTitle(selectedTitle);
        editEpView.setOrder(selectedEpInfo.get("order"));
        editEpView.setEpName(selectedEpInfo.get("episode_name"));
        editEpView.setBackupLink(selectedEpInfo.get("backup_link"));
        editEpView.setDriveLink(selectedEpInfo.get("drive_link"));
        editEpView.setGgCloudLink(selectedEpInfo.get("gg_cloud_link"));
        editEpView.setLink3(selectedEpInfo.get("link_3"));
        editEpView.setLink4(selectedEpInfo.get("link_4"));
        
        editEpView.setVisible(true);
        
        // save changed button listener
        editEpView.addSaveBtnListener((ActionEvent e) -> {
            saveEpisode();
            updateEpTable();
        });
        
        // cancel button listener
        editEpView.addCancelBtnListener((ActionEvent e) -> {
            editEpView.dispose();
            updateEpTable();
        });
    }
    
    public void saveEpisode() {
        // get new values
        try {
            newOrder = Integer.parseInt(editEpView.getOrder());
            newEpisodeName = editEpView.getEpName();
            newBackupLink = editEpView.getBackupLink();
            newDriveLink = editEpView.getDriveLink();
            newGgCloudLink = editEpView.getGgCloudLink();
            newLink3 = editEpView.getLink3();
            newLink4 = editEpView.getLink4();
            
            //confirm dialog
            int result = JOptionPane.showConfirmDialog(null, "Save(Y/N)?");
            if (result == JOptionPane.YES_OPTION) {
                dbConnection.editEpisode(Integer.parseInt(selectedEpId), newOrder,
                        newEpisodeName, newBackupLink, newDriveLink, newGgCloudLink,
                        newLink3, newLink4);
                editEpView.dispose();
                updateTable();
            } else if (result == JOptionPane.NO_OPTION) {
                editEpView.dispose();
            }
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Order must be number!");
        }
        
    }
    
    public void addEpisode() {
        addEpView = new AddEpisodeView();
        addEpView.setEnableField(true);
        addEpView.setVisible(true);
        addEpView.addAddBtn1Listener((ActionEvent e1) -> {
            addSingleEp();
        });
        
        addEpView.addAddBtn2Listener((ActionEvent e1) -> {
            upProgress(0);
            addMultipleEps();
        });
    }
    
    public void addSingleEp() {
        try {
            addEpView.setEnableField(false);
            
            newEpisodeName = addEpView.getEpName();
            newOrder = Integer.parseInt(addEpView.getOrder());
            newBackupLink = addEpView.getBackupLink();
            newDriveLink = addEpView.getDriveLink();
            newGgCloudLink = addEpView.getGgCloudLink();
            newLink3 = addEpView.getLink3();
            newLink4 = addEpView.getLink4();
            
            Thread addingThread = new Thread(() -> {
                String message = dbConnection.addEpisode(selectedId, newOrder, newEpisodeName,
                        newBackupLink, newDriveLink, newGgCloudLink, newLink3,
                        newLink4);
                JOptionPane.showMessageDialog(null, message);
                updateEpTable();
                
                addEpView.dispose();
            });
            addingThread.start();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Order must be a number!!");
            addEpView.setEnableField(true);
        }
    }
    
    public void addMultipleEps() {
        try {
            message = "";
            String data = addEpView.getData();
            String[] rows = data.split("\n");
            Thread[] addingThreads = new Thread[rows.length];
            for (int i = 0; i < rows.length; i++) {
                try {
                    String tempEpisodeName = rows[i].split("\t")[1];
                    int tempOrder = Integer.parseInt(rows[i].split("\t")[0]);
                    String tempBackupLink = "";
                    String tempDriveLink = "";
                    String tempGgCloudLink = "";
                    String tempLink3 = "";
                    String tempLink4 = "";
                    try {
                        tempBackupLink = rows[i].split("\t")[2];
                        tempDriveLink = rows[i].split("\t")[3];
                        tempGgCloudLink = rows[i].split("\t")[4];
                        tempLink3 = rows[i].split("\t")[5];
                        tempLink4 = rows[i].split("\t")[6];
                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("Lack of Links");
                    }
                    final String finalBackupLink = tempBackupLink;
                    final String finalGgCloudLink = tempGgCloudLink;
                    final String finalDriveLink = tempDriveLink;
                    final String finalLink3 = tempLink3;
                    final String finalLink4 = tempLink4;
                    
                    Thread addingThread = new Thread(() -> {
                        String m = dbConnection.addEpisode(selectedId, tempOrder,
                                tempEpisodeName, finalBackupLink, finalDriveLink,
                                finalGgCloudLink, finalLink3, finalLink4);
                        
                        //update the message
                        message = message + m;
                        upProgress((int) 100 / rows.length);
                        addEpView.setProgressBar(progress);
                        updateEpTable();
                        
                    });
                    addingThreads[i] = addingThread;
                    addingThread.start();
                } catch (IndexOutOfBoundsException e) {
                    JOptionPane.showMessageDialog(null, "Invalid form data!");
                }
            }
            
            for (Thread thread : addingThreads) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            // set the progress to 100%
            upProgress(100);
            addEpView.setProgressBar(progress);
            
            JOptionPane.showMessageDialog(null, message);
            // update the episode table
            updateEpTable();
            addEpView.dispose();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Order must be a number!!");
        }
    }
}
