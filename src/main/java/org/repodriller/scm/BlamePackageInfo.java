package org.repodriller.scm;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlamePackageInfo {
    private final String packageName;
    private final Set<RevCommit> commits;
    private long lineCount;
    private long lineSize;
    private final Map<String, BlameFileInfo> filesInfo;
    private final Map<String, BlameAuthorInfo> authorInfo;

    public BlamePackageInfo(String packageName) {
        this.packageName = packageName;
        this.commits = new HashSet<>();
        this.lineCount = 0;
        this.lineSize = 0;
        this.filesInfo = new HashMap<>();
        this.authorInfo = new HashMap<>();
    }

    public void add(BlameFileInfo fileInfo, String filePath) {
        this.filesInfo.put(filePath, fileInfo);
        this.lineCount += fileInfo.getLineCount();
        this.lineSize += fileInfo.getLineSize();
        this.commits.addAll(fileInfo.getCommits());
        fileInfo.getAuthorInfos().values().forEach(a ->  authorInfo.computeIfAbsent(a.getAuthor(), k -> new BlameAuthorInfo(a.getAuthor())).add(a));

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

    public String getPackageName() {return packageName;}
    public Set<RevCommit> getCommits() {return commits;}
    public long getLineCount() {return lineCount;}
    public long getLineSize() {return lineSize;}
    public Map<String, BlameFileInfo> getFilesInfo() {return filesInfo;}
    public Map<String, BlameAuthorInfo> getAuthorInfo() {return authorInfo;}

    @Override
    public String toString() {
        return String.format("package: %s, LineCount: %d, LineSize: %d, Commits: %s,", packageName, lineCount, lineSize, commits);
    }
}
