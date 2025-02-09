package org.repodriller.scm;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.repodriller.scm.entities.DeveloperInfo;
import org.repodriller.util.BlameEntity;
import org.repodriller.util.DataBaseUtil;
import org.repodriller.util.FileEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class GitRepositoryUtil {

    private static final Map<String, Long> fileSizeCache = new ConcurrentHashMap<>();

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

    public static Map<String, FileEntity> getCommitsFiles(RevCommit commit, Git git, DeveloperInfo dev) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DiffFormatter diffFormatter = new DiffFormatter(out)) {
            RevCommit parent = (commit.getParentCount() > 0) ? commit.getParent(0) : null;
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            List<DiffEntry> diffs = diffFormatter.scan(parent, commit);
            Map<String, FileEntity> paths = new HashMap<>();

            int fileAdded = 0, fileDeleted = 0,fileModified = 0, linesAdded = 0, linesDeleted = 0, linesModified = 0, changes  = 0;

            for (DiffEntry diff : diffs) {
                switch (diff.getChangeType()) {
                    case ADD -> fileAdded++;
                    case DELETE -> fileDeleted++;
                    case MODIFY -> fileModified++;
                }

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
                paths.putIfAbsent(diff.getNewPath(), new FileEntity(0, 0, 0, 0, 0, 0, 0, 0));
                paths.get(diff.getNewPath()).plus(fileAdded, fileDeleted, fileModified, linesAdded, linesDeleted, linesModified, changes, out.size());
                dev.processFileEntity(paths.get(diff.getNewPath()));
            }
            return paths;
        }
    }

    public static long processCommitSize(RevCommit commit, Git git) {
        long projectSize = 0;
        ObjectReader reader = null;
        try {
            reader = git.getRepository().newObjectReader();
            // TreeWalk для обхода файлов в дереве коммита
            try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);  // Рекурсивный обход всех файлов

                while (treeWalk.next()) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    String objectHash = objectId.getName();
                    Long size = fileSizeCache.getOrDefault(objectHash, null);
                    if (size == null) {
                        // Открываем объект и проверяем его тип
                        ObjectLoader loader = reader.open(objectId);
                        if (loader.getType() == Constants.OBJ_BLOB) {
                            // Если это blob (файл), добавляем его размер
                            projectSize += loader.getSize();
                            fileSizeCache.put(objectHash, loader.getSize());
                        }
                    } else projectSize += size;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return projectSize;
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

    public static void updateFileOwnerBasedOnBlame(BlameResult blameResult, Map<String, String> devs, DataBaseUtil dataBaseUtil, Integer projectId, Integer blameFileId, String headHash) {
        Map<String, BlameEntity> blameEntityMap = new HashMap<>();
        if (blameResult != null) {
            for (int i = 0; i < blameResult.getResultContents().size(); i++) {
                PersonIdent author = blameResult.getSourceAuthor(i);
                String commitHash = blameResult.getSourceCommit(i).getName();
                long lineSize = blameResult.getResultContents().getString(i).getBytes().length;
                BlameEntity blameEntity = blameEntityMap.computeIfAbsent(author.getEmailAddress(), key -> new BlameEntity(projectId, devs.get(key), blameFileId, new ArrayList<>(), new ArrayList<>(), 0));
                blameEntity.getBlameHashes().add(headHash);
                blameEntity.getLineIds().add(i);
                blameEntity.setLineSize(blameEntity.getLineSize() + lineSize);
            }
        } else System.out.println("Blame for file " + blameResult.getResultPath() + " not found");
        dataBaseUtil.insertBlame(blameEntityMap.values().stream().toList());
    }
}
