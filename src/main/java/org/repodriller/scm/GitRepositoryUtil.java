package org.repodriller.scm;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.repodriller.scm.entities.DeveloperInfo;

import java.io.IOException;
import java.util.*;


public class GitRepositoryUtil {

    public static void analyzeCommit(RevCommit commit, Git git, DeveloperInfo dev) throws IOException {
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            if (commit.getParentCount() > 0) {
                List<DiffEntry> diffs = diffFormatter.scan(commit.getParent(0), commit);
                for (DiffEntry diff : diffs) {
                    dev.processDiff(diff, git.getRepository());
                }
            } else {
                List<DiffEntry> diffs = diffFormatter.scan(null, commit);
                for (DiffEntry diff : diffs) {
                    dev.processDiff(diff, git.getRepository());
                }
            }
        }
    }

    public static void updateFileOwnerBasedOnBlame(String filePath, Git git, Map<String, DeveloperInfo> developers) throws GitAPIException {
        BlameResult blameResult = git.blame().setFilePath(filePath).call();
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
            owner.ifPresent(stringIntegerEntry -> developers.get(stringIntegerEntry.getKey()).addOwnedFile(filePath));
        } else System.out.println("Blame for file " + filePath + " not found");
    }
}
