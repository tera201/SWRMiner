package org.repodriller.scm.entities;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class DeveloperInfo {
    private final String name;
    private final String emailAddress;
    private List<RevCommit> commits;
    private long changes;
    private long changesSize;
    public long actualLinesOwner;
    public long actualLinesSize;
    private long linesAdded;
    private long linesDeleted;
    private long linesModified;
    private long fileAdded;
    private long fileDeleted;
    private long fileModified;
    private List<String> authorForFiles;
    public List<String> ownerForFiles;

    public DeveloperInfo(String name, String emailAddress) {
        this.name = name;
        this.emailAddress = emailAddress;
        this.commits = new ArrayList<>();
        this.authorForFiles = new ArrayList<>();
        this.ownerForFiles = new ArrayList<>();
    }

    public void addCommit(RevCommit commit) {
        commits.add(commit);
    }

    public void addAuthoredFile(String filePath) {
        authorForFiles.add(filePath);
    }

    public void addOwnedFile(String filePath) {
        ownerForFiles.add(filePath);
    }

    public void processDiff(DiffEntry diff, Repository repository) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
            this.addAuthoredFile(diff.getNewPath());
        }
        switch (diff.getChangeType()) {
            case ADD:
                fileAdded++;
                break;
            case DELETE:
                fileDeleted++;
                break;
            case MODIFY:
                fileModified++;
                break;
        }
        try (DiffFormatter diffFormatter = new DiffFormatter(out)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);
            diffFormatter.format(diff);

            EditList editList = diffFormatter.toFileHeader(diff).toEditList();
            for (var edit : editList) {
                switch (edit.getType()) {
                    case INSERT:
                        linesAdded += edit.getLengthB();
                        changes += edit.getLengthB();
                        break;
                    case DELETE:
                        linesDeleted += edit.getLengthA();
                        changes += edit.getLengthA();
                        break;
                    case REPLACE:
                        //TODO getLengthA (removed)  getLengthB (added) - maybe max(A,B) or just B
                        linesModified += edit.getLengthA() + edit.getLengthB();
                        changes += edit.getLengthA() + edit.getLengthB();
                        break;
                }
            }
        }
        changesSize += out.size();
    }

    public void increaseActualLinesOwner(long linesOwner) {
        actualLinesOwner += linesOwner;
    }

    public void increaseActualLinesSize(long linesSize) {
        actualLinesSize += linesSize;
    }
}
