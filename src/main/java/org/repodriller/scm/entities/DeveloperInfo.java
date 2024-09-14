package org.repodriller.scm.entities;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.repodriller.util.CommitEntity;
import org.repodriller.util.FileEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeveloperInfo {
    private String id;
    private final String name;
    private final String emailAddress;
    private final List<RevCommit> commits;
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
        this(name, emailAddress, "");
    }

    public DeveloperInfo(CommitEntity commitEntity, RevCommit commit) {
        this.id = commitEntity.getAuthorId();
        this.name = commitEntity.getAuthorName();
        this.emailAddress = commitEntity.getAuthorEmail();
        this.commits = new ArrayList<>();
        this.authorForFiles = new ArrayList<>();
        this.ownerForFiles = new ArrayList<>();
        this.commits.add(commit);
        this.changes = commitEntity.getFileEntity().getChanges();
        this.changesSize = commitEntity.getFileEntity().getChangesSize();
        this.linesAdded = commitEntity.getFileEntity().getLinesAdded();
        this.linesDeleted = commitEntity.getFileEntity().getLinesDeleted();
        this.linesModified = commitEntity.getFileEntity().getLinesModified();
        this.fileAdded = commitEntity.getFileEntity().getFileAdded();
        this.fileDeleted = commitEntity.getFileEntity().getFileDeleted();
        this.fileModified = commitEntity.getFileEntity().getFileModified();
    }

    public DeveloperInfo(String name, String emailAddress, String id) {
        this.id = id;
        this.name = name;
        this.emailAddress = emailAddress;
        this.commits = new ArrayList<>();
        this.authorForFiles = new ArrayList<>();
        this.ownerForFiles = new ArrayList<>();
    }

    public void addCommit(RevCommit commit) {
        commits.add(commit);
    }

    public void updateByCommit(CommitEntity commitEntity, RevCommit commit) {
            this.commits.add(commit);
            this.changes += commitEntity.getFileEntity().getChanges();
            this.changesSize += commitEntity.getFileEntity().getChangesSize();
            this.linesAdded += commitEntity.getFileEntity().getLinesAdded();
            this.linesDeleted += commitEntity.getFileEntity().getLinesDeleted();
            this.linesModified += commitEntity.getFileEntity().getLinesModified();
            this.fileAdded += commitEntity.getFileEntity().getFileAdded();
            this.fileDeleted += commitEntity.getFileEntity().getFileDeleted();
            this.fileModified += commitEntity.getFileEntity().getFileModified();
    }

    public void addAuthoredFile(String filePath) {
        authorForFiles.add(filePath);
    }

    public void addOwnedFile(String filePath) {
        ownerForFiles.add(filePath);
    }

    public void plusLinesAdded(long linesAdded) { this.linesAdded += linesAdded; }

    public void plusLinesDeleted(long linesDeleted) { this.linesDeleted += linesDeleted; }

    public void plusLinesModified(long linesModified) { this.linesModified += linesModified; }

    public void plusFilesAdded(long filesAdded) { this.fileAdded += filesAdded; }

    public void plusFilesDeleted(long filesDeleted) { this.fileDeleted += filesDeleted; }

    public void plusFilesModified(long filesModified) { this.fileModified += filesModified; }

    public void plusChanges(long changes) { this.changes += changes; }

    public void plusChangesSize(long changesSize) { this.changesSize += changesSize; }

    public void processFileEntity(FileEntity fileEntity) {
        this.plusFilesAdded(fileEntity.getLinesAdded());
        this.plusFilesDeleted(fileEntity.getLinesDeleted());
        this.plusFilesModified(fileEntity.getLinesModified());
        this.plusLinesAdded(fileEntity.getLinesAdded());
        this.plusLinesDeleted(fileEntity.getLinesDeleted());
        this.plusLinesModified(fileEntity.getLinesModified());
        this.plusChanges(fileEntity.getChanges());
        this.plusChangesSize(fileEntity.getChangesSize());
    }

    public void processDiff(DiffEntry diff, Repository repository) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
            this.addAuthoredFile(diff.getNewPath());
        }
        switch (diff.getChangeType()) {
            case ADD ->  fileAdded++;
            case DELETE -> fileDeleted++;
            case MODIFY -> fileModified++;
        }
        try (DiffFormatter diffFormatter = new DiffFormatter(out)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);
            diffFormatter.format(diff);

            EditList editList = diffFormatter.toFileHeader(diff).toEditList();
            for (var edit : editList) {
                switch (edit.getType()) {
                    case INSERT -> {
                        linesAdded += edit.getLengthB();
                        changes += edit.getLengthB();
                    }
                    case DELETE -> {
                        linesDeleted += edit.getLengthA();
                        changes += edit.getLengthA();
                    }
                    case REPLACE -> {
                        //TODO getLengthA (removed)  getLengthB (added) - maybe max(A,B) or just B
                        linesModified += edit.getLengthA() + edit.getLengthB();
                        changes += edit.getLengthA() + edit.getLengthB();
                    }
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
