package org.repodriller.scm.entities;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CommitSize {
    private String name;
    private long projectSize;
    private String authorName;
    private String authorEmail;
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

    public void setAuthor(String authorName, String authorEmail) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
    }

    public void addFile(String fileName, long fileSize) {
        this.fileSize.put(fileName, fileSize);
        this.projectSize += fileSize;
    }

    public void  addFileSize(long fileSize) {
        this.projectSize += fileSize;
    }
}
