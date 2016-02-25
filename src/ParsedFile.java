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

public class ParsedFile {
    
    public ArrayList Parse(){
        ArrayList words = new ArrayList();
        String fileToParse = "Words.txt";
        BufferedReader fileReader = null;
        
        try{
            String line = "";
            fileReader = new BufferedReader(new FileReader(fileToParse));
            while ((line = fileReader.readLine()) != null){
                words.add(line);
            }
        }
        catch(Exception e){
            e.printStackTrace();
            }
        return words;
    }
    
    public static void main(String[] args){
        ParsedFile f = new ParsedFile();
        
        System.out.println(f.Parse());
    }
    
}
