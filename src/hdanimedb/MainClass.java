/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hdanimedb;

/**
 *
 * @author Nam's PC
 */
public class MainClass {
    public static void main(String args[]) {
        GUI gui = new GUI();
        DBConnection dbConnection = new DBConnection();
        Controller controller = new Controller(gui, dbConnection);
    }
}
