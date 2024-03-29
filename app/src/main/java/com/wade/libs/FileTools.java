package com.wade.libs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
  FileTools fileTools = new FileTools(Environment.getExternalStorageDirectory()+"/Status.txt");
  fileTools.append("PWR_ON,"+ Calendar.getInstance(timeZone).getTime().toString()+"\n");
 */

public class FileTools {
    private String fileName;
    public FileTools(String fn) {
        fileName = fn;
    }
    public FileTools() {
        fileName = "";
    }
    public void setFileName(String fn) {
        fileName = fn;
    }
    public String load() {
        File file = new File(fileName);
        if (!file.exists()) return "";
        try {
            FileReader f = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(f);
            String text = "";
            String buf="";
            while ((buf = bufferedReader.readLine()) != null) {
                text += buf+"\n";
            }
            bufferedReader.close();
            f.close();
            return text;
        } catch (IOException e) {
        }
        return "";
    }

    public void delete() {
        File file = new File(fileName);
        file.delete();
    }

    private void write(String buf, boolean append) {
        try {
            File file = new File(fileName);
            FileWriter f  = new FileWriter(file, append);
            BufferedWriter bufferedWriter = new BufferedWriter(f);

            bufferedWriter.write(buf);
            bufferedWriter.flush();
            bufferedWriter.close();
            f.close();
        } catch (IOException e) {
        }
    }

    public void save(String buf) {
        write(buf, false);
    }
    public void append(String buf) {
        write(buf, true);
    }
    public void empty() {
        write("", false);
    }
}
