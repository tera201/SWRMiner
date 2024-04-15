package org.repodriller.scm;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlameFileInfo {
    private final String fileName;
    private final Set<RevCommit> commits;
    private long lineCount;
    private long lineSize;
    private final Map<String, BlameAuthorInfo> authorInfos;

    public BlameFileInfo(String fileName) {
        this.fileName = fileName;
        this.commits = new HashSet<>();
        this.lineCount = 0;
        this.lineSize = 0;
        this.authorInfos = new HashMap<>();
    }

    public void add(BlameAuthorInfo authorInfo) {
        this.authorInfos.computeIfAbsent(authorInfo.getAuthor(), k -> new BlameAuthorInfo(authorInfo.getAuthor())).add(authorInfo);
        this.lineCount += authorInfo.getLineCount();
        this.lineSize += authorInfo.getLineSize();
        this.commits.addAll(authorInfo.getCommits());
    }

    public RevCommit findLatestCommit() {
        RevCommit latestCommit = null;
        int latestTime = 0;

        for (RevCommit commit : this.commits) {
            if (commit.getCommitTime() > latestTime) {
                latestCommit = commit;
                latestTime = commit.getCommitTime();
            }
        }

        return latestCommit;
    }

    public String getFileName() {return fileName;}
    public Set<RevCommit> getCommits() {return commits;}
    public long getLineCount() {return lineCount;}
    public long getLineSize() {return lineSize;}
    public Map<String, BlameAuthorInfo> getAuthorInfos() {return authorInfos;}

    @Override
    public String toString() {
        return String.format("file: %s, LineCount: %d, LineSize: %d, Commits: %s, Authors: [%s]", fileName, lineCount, lineSize, commits, authorInfos.values().toString());
    }
}
