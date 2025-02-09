package org.repodriller.scm.entities;

import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.HashSet;
import java.util.Set;

@Getter
public class BlameAuthorInfo {
    private String author;
    private Set<RevCommit> commits;
    private long lineCount;
    private long lineSize;

    public BlameAuthorInfo(String author) {
        this.author = author;
        this.commits = new HashSet<>();
        this.lineCount = 0;
        this.lineSize = 0;
    }

    public BlameAuthorInfo(String author, Set<RevCommit> commits, long lineCount, long lineSize) {
        this.author = author;
        this.commits = commits;
        this.lineCount = lineCount;
        this.lineSize = lineSize;
    }

    public void add(BlameAuthorInfo other) {
        this.commits.addAll(other.commits);
        this.lineCount += other.lineCount;
        this.lineSize += other.lineSize;
    }

    public void setAuthor(String author) {
        this.author = author;
    };

    @Override
    public String toString() {
        return String.format("Author: %s, LineCount: %d, LineSize: %d, Commits: %s", author, lineCount, lineSize, commits.toString());
    }
}
