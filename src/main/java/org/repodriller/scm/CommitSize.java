package org.repodriller.scm;

import java.util.HashMap;
import java.util.Map;

public class CommitSize {
    private String name;
    private long projectSize;
    private Map<String, Long> fileSize;
    private int date;

    public CommitSize(String name, int date) {
        this.name = name;
        this.projectSize = 0;
        this.fileSize = new HashMap<>();
        this.date = date;
    }

    public CommitSize(String name, long projectSize, Map<String, Long> fileSize, int date) {
        this.name = name;
        this.projectSize = projectSize;
        this.fileSize = fileSize;
        this.date = date;
    }

    public void addFile(String fileName, long fileSize) {
        this.fileSize.put(fileName, fileSize);
        this.projectSize += fileSize;
    }

    public String getName() {return name;}
    public long getProjectSize() {return projectSize;}
    public Map<String, Long> getFileSize() {return fileSize;}
    public int getDate() {return date;}
}
