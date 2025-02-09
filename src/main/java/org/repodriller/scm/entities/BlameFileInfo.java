package org.repodriller.scm.entities;

import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class BlameFileInfo {
    private final String fileName;
    private String authorName;
    private String ownerName;
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

    @Override
    public String toString() {
        return String.format("file: %s, LineCount: %d, LineSize: %d, Commits: %s, Authors: [%s]", fileName, lineCount, lineSize, commits, authorInfos.values().toString());
    }
}
