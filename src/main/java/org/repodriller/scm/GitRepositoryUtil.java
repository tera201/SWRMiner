package org.repodriller.scm;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.repodriller.scm.entities.DeveloperInfo;
import org.repodriller.util.BlameEntity;
import org.repodriller.util.DataBaseUtil;
import org.repodriller.util.FileEntity;

import java.io.IOException;
import java.util.*;


public class GitRepositoryUtil {

    public static void analyzeCommit(RevCommit commit, Git git, DeveloperInfo dev) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            RevCommit parent = (commit.getParentCount() > 0) ? commit.getParent(0) : null;
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            List<DiffEntry> diffs = diffFormatter.scan(parent, commit);
            for (DiffEntry diff : diffs) {
                dev.processDiff(diff, git.getRepository());
            }
        }
    }

    public static Map<String, FileEntity> getCommitsFiles(RevCommit commit, Git git) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            RevCommit parent = (commit.getParentCount() > 0) ? commit.getParent(0) : null;
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            List<DiffEntry> diffs = diffFormatter.scan(parent, commit);
            Map<String, FileEntity> paths = new HashMap();

            int fileAdded = 0, fileDeleted = 0,fileModified = 0, linesAdded = 0, linesDeleted = 0, linesModified = 0, changes  = 0;

            for (DiffEntry diff : diffs) {
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
                paths.putIfAbsent(diff.getNewPath(), new FileEntity(0, 0, 0, 0, 0, 0, 0));
                paths.get(diff.getNewPath()).plus(fileAdded, fileDeleted, fileModified, linesAdded, linesDeleted, linesModified, changes);
            }
            return paths;
        }
    }

    public static long processCommitSize(RevCommit commit, Git git) {
        try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
            long projectSize = 0;
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                ObjectId objectId = treeWalk.getObjectId(0);
                long size = git.getRepository().getObjectDatabase().open(objectId).getSize();
               projectSize += size;
            }
            return projectSize;
        } catch (Exception e) {}
        return 0;
    }

    public static void updateFileOwnerBasedOnBlame(BlameResult blameResult, Map<String, DeveloperInfo> developers) {
        Map<String, Integer> linesOwners = new HashMap<>();
        Map<String, Long> linesSizes = new HashMap<>();
        if (blameResult != null) {
            for (int i = 0; i < blameResult.getResultContents().size(); i++) {
                String authorEmail = blameResult.getSourceAuthor(i).getEmailAddress();
                long lineSize = blameResult.getResultContents().getString(i).getBytes().length;
                linesSizes.merge(authorEmail, lineSize, Long::sum);
                linesOwners.merge(authorEmail, 1, Integer::sum);
            }
            linesOwners.forEach((key, value) -> developers.get(key).increaseActualLinesOwner(value));
            linesSizes.forEach((key, value) -> developers.get(key).increaseActualLinesSize(value));
            Optional<Map.Entry<String, Integer>> owner = linesOwners.entrySet().stream().max(Map.Entry.comparingByValue());
            owner.ifPresent(stringIntegerEntry -> developers.get(stringIntegerEntry.getKey()).addOwnedFile(blameResult.getResultPath()));
        } else System.out.println("Blame for file " + blameResult.getResultPath() + " not found");
    }

    public static void updateFileOwnerBasedOnBlame(BlameResult blameResult, Map<String, Integer> devs, DataBaseUtil dataBaseUtil, Integer projectId, Integer blameFileId) {
        Map<String, BlameEntity> blameEntityMap = new HashMap<>();
        if (blameResult != null) {
            for (int i = 0; i < blameResult.getResultContents().size(); i++) {
                PersonIdent author = blameResult.getSourceAuthor(i);
                String commitHash = blameResult.getSourceCommit(i).getName();
                long lineSize = blameResult.getResultContents().getString(i).getBytes().length;
                BlameEntity blameEntity = blameEntityMap.getOrDefault(blameResult.getSourceAuthor(i).getEmailAddress(), new BlameEntity(projectId, devs.get(author.getEmailAddress()), blameFileId, new ArrayList<>(), new ArrayList<>(), 0));
                blameEntityMap.putIfAbsent(blameResult.getSourceAuthor(i).getEmailAddress(), blameEntity);
                blameEntity.getBlameHashes().add(commitHash);
                blameEntity.getLineIds().add(i);
                blameEntity.setLineSize(blameEntity.getLineSize() + lineSize);
            }
        } else System.out.println("Blame for file " + blameResult.getResultPath() + " not found");
        dataBaseUtil.insertBlame(blameEntityMap.values().stream().toList());
    }
}
