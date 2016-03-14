/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Eric
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class ParsedFile {
    
    private ArrayList<String> sens_data = new ArrayList<String>();
    //Parse function: parses a text file and returns an arraylist of those words.
    public ArrayList Parse(String st){
        
        ArrayList words = new ArrayList();
        String fileToParse = st;
        BufferedReader fileReader = null;
      
        //Try statement in case file doesn't exist
        try{
            String line = "";
            fileReader = new BufferedReader(new FileReader(fileToParse));
            
            //Read each of the file and add it to the arraylist of words.
            while ((line = fileReader.readLine()) != null){
                words.add(line);
            }
        }
        
        //catch the error in case the file wasn't found and show the stacktrace.
        catch(Exception e){
            e.printStackTrace();
            }
        return words;
    }

    
}
