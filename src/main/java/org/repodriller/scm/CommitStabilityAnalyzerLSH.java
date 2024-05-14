package org.repodriller.scm;

import org.apache.datasketches.theta.UpdateSketch;
import org.apache.datasketches.theta.UpdateSketchBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommitStabilityAnalyzerLSH {

    public static void analyzeRepository(Git git) throws Exception {
        Iterable<RevCommit> commits = git.log().call();
        List<RevCommit> commitList = new ArrayList<>();
        commits.forEach(commitList::add);

        Map<String, UpdateSketch> fileHunkSketches = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < commitList.size(); i++) {
            fileHunkSketches.clear();
            RevCommit commit = commitList.get(i);
            Date commitDate = commit.getCommitterIdent().getWhen();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(commitDate);
            calendar.add(Calendar.MONTH, 1);
            Date oneMonthLater = calendar.getTime();

            // Получить коммиты в пределах месяца после текущего коммита
            List<RevCommit> nextMonthCommits = findCommitsInNextMonth(commitList, i, oneMonthLater);

            if (!nextMonthCommits.isEmpty()) {
                analyzeCommitChanges(git, commit, fileHunkSketches);

                // Собрать все изменения за следующий месяц в один скетч
                Map<String, UpdateSketch> combinedNextMonthSketches = combineCommitsChanges(git, nextMonthCommits);

                double commitStability = calculateCommitStability(commit, combinedNextMonthSketches, fileHunkSketches);
                Map<String, Double> fileStabilities = calculateFileStabilities(commit, combinedNextMonthSketches, fileHunkSketches);

//                if (commitStability > 0) {
//                    System.out.println("Commit: " + commit.getName() + " Date: " + dateFormat.format(commitDate) + " Stability: " + commitStability);
//                    System.out.println("File Stabilities:");
//                    for (Map.Entry<String, Double> entry : fileStabilities.entrySet()) {
//                        if (entry.getValue() == 0) continue;
//                        System.out.println("  File: " + entry.getKey() + " Stability: " + entry.getValue());
//                    }
//                }
            }
        }
    }

    private static List<RevCommit> findCommitsInNextMonth(List<RevCommit> commitList, int startIndex, Date oneMonthLater) {
        List<RevCommit> nextMonthCommits = new ArrayList<>();
        Date currentCommitDate = commitList.get(startIndex).getCommitterIdent().getWhen();
        for (int i = 0; i < startIndex; i++) {
            RevCommit commit = commitList.get(i);
            Date commitDate = commit.getCommitterIdent().getWhen();
            if (commitDate.after(currentCommitDate) && commitDate.before(oneMonthLater)) {
                nextMonthCommits.add(commit);
            }
        }
        return nextMonthCommits;
    }

    private static void analyzeCommitChanges(Git git, RevCommit commit, Map<String, UpdateSketch> fileHunkSketches) throws Exception {
        RevCommit parent = commit.getParentCount() > 0 ? commit.getParent(0) : null;

        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(git.getRepository());
            List<DiffEntry> diffs = diffFormatter.scan(parent, commit);

            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = diffFormatter.toFileHeader(diff);
                EditList edits = fileHeader.toEditList();

                String filePath = diff.getNewPath();
                fileHunkSketches.putIfAbsent(filePath, new UpdateSketchBuilder().build());

                for (Edit edit : edits) {
                    String hunkContent = getHunkContent(git.getRepository(), commit, filePath, edit);
                    UpdateSketch sketch = fileHunkSketches.get(filePath);
                    sketch.update(hunkContent);
                }
            }
        }
    }

    private static Map<String, UpdateSketch> combineCommitsChanges(Git git, List<RevCommit> commits) throws Exception {
        Map<String, UpdateSketch> combinedSketches = new HashMap<>();

        for (RevCommit commit : commits) {
            RevWalk walk = new RevWalk(git.getRepository());
            RevCommit parent = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0).getId()) : null;

            CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            if (parent != null) {
                oldTreeParser.reset(walk.getObjectReader(), parent.getTree());
            }

            CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
            newTreeParser.reset(walk.getObjectReader(), commit.getTree());

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(parent != null ? oldTreeParser : null)
                    .setNewTree(newTreeParser)
                    .call();

            try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                diffFormatter.setRepository(git.getRepository());

                for (DiffEntry diff : diffs) {
                    FileHeader fileHeader = diffFormatter.toFileHeader(diff);
                    EditList edits = fileHeader.toEditList();

                    String filePath = diff.getNewPath();
                    combinedSketches.putIfAbsent(filePath, new UpdateSketchBuilder().build());
                    UpdateSketch combinedSketch = combinedSketches.get(filePath);

                    for (Edit edit : edits) {
                        String hunkContent = getHunkContent(git.getRepository(), commit, filePath, edit);
                        combinedSketch.update(hunkContent);
                    }
                }
            }
        }

        return combinedSketches;
    }

    private static double calculateCommitStability(RevCommit commit, Map<String, UpdateSketch> combinedNextMonthSketches, Map<String, UpdateSketch> fileHunkSketches) {
        double stabilityScore = 0;

        for (Map.Entry<String, UpdateSketch> entry : fileHunkSketches.entrySet()) {
            String filePath = entry.getKey();
            UpdateSketch sketch = entry.getValue();
            UpdateSketch combinedSketch = combinedNextMonthSketches.get(filePath);

            if (combinedSketch != null) {
                stabilityScore += sketch.getEstimate() * combinedSketch.getEstimate();
            }
        }

        return stabilityScore;
    }

    private static Map<String, Double> calculateFileStabilities(RevCommit commit, Map<String, UpdateSketch> combinedNextMonthSketches, Map<String, UpdateSketch> fileHunkSketches) {
        Map<String, Double> fileStabilities = new HashMap<>();

        for (Map.Entry<String, UpdateSketch> entry : fileHunkSketches.entrySet()) {
            String filePath = entry.getKey();
            UpdateSketch sketch = entry.getValue();
            UpdateSketch combinedSketch = combinedNextMonthSketches.get(filePath);

            if (combinedSketch != null) {
                double fileStabilityScore = sketch.getEstimate() * combinedSketch.getEstimate();
                fileStabilities.put(filePath, fileStabilityScore);
            } else {
                fileStabilities.put(filePath, 0.0);
            }
        }

        return fileStabilities;
    }

    private static String getHunkContent(Repository repository, RevCommit commit, String filePath, Edit edit) throws Exception {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, commit.getTree())) {
            if (treeWalk != null) {
                ObjectId blobId = treeWalk.getObjectId(0);
                byte[] content = repository.open(blobId).getBytes();
                String fileContent = new String(content, "UTF-8");
                return fileContent.substring(edit.getBeginA(), edit.getEndA());
            } else {
                return "";
            }
        }
    }
}