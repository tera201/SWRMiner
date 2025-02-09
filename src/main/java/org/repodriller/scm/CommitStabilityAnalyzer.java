package org.repodriller.scm;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.util.*;

public class CommitStabilityAnalyzer {

    public static Double analyzeCommit(Git git, List<RevCommit> commitList, RevCommit commit, int index) throws Exception {
        double commitStability = 1D;
        Date commitDate = commit.getCommitterIdent().getWhen();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(commitDate);
        calendar.add(Calendar.MONTH, 1);
        Date oneMonthLater = calendar.getTime();

        RevCommit nextMonthCommit = findCommitInNextMonth(commitList, index, commitDate,  oneMonthLater);

        if (nextMonthCommit != null) {
            commitStability = calculateCommitStability(git, commit, nextMonthCommit);
        }
        return commitStability;
    }

    private static RevCommit findCommitInNextMonth(List<RevCommit> commitList, int index, Date currentCommitDate, Date oneMonthLater) {
        RevCommit nextMonthCommits = null;
        for (int i = 0; i < index; i++) {
            RevCommit commit = commitList.get(i);
            Date commitDate = commit.getCommitterIdent().getWhen();
            if (commitDate.after(currentCommitDate) && commitDate.before(oneMonthLater)) {
                nextMonthCommits = commit;
            }
        }
        return nextMonthCommits;
    }

    private static double calculateCommitStability(Git git, RevCommit targetCommit, RevCommit lastMonthCommit) throws Exception {
        RevCommit parent = targetCommit.getParentCount() > 0 ? targetCommit.getParent(0) : null;
        List<Edit> editsAB = new ArrayList<>();
        List<Edit> editsBC = new ArrayList<>();
        long abSize;

        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(git.getRepository());

            List<DiffEntry> diffs = diffFormatter.scan(parent, targetCommit);
            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = diffFormatter.toFileHeader(diff);
                EditList edits = fileHeader.toEditList();
                editsAB.addAll(edits);
            }

            abSize = editsAB.stream().mapToLong(Edit::getLengthB).sum();
            if (abSize == 0) return 0;

            diffs = diffFormatter.scan(targetCommit, lastMonthCommit);
            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = diffFormatter.toFileHeader(diff);
                EditList edits = fileHeader.toEditList();
                editsBC.addAll(edits);
            }
        }
        double intersectionSize = getIntersectingEdits(editsAB, editsBC).stream().mapToDouble(it -> it[1] - it[0]).sum();

        return 1 - intersectionSize / abSize;
    }
    

    private static int[] getIntersectionStartAndEnd(Edit editA, int[] editB) {
        int start = Math.max(editA.getBeginB(), editB[0]);
        int end = Math.min(editA.getEndB(), editB[1]);

        if (start < end) {
            return new int[]{start, end};
        } else {
            return new int[0];
        }
    }

    public static List<int[]> mergeIntersectingRanges(List<Edit> edits) {
        if (edits.isEmpty()) return Collections.emptyList();
        edits.sort(Comparator.comparingInt(Edit::getBeginA));
        List<int[]> mergedRanges = new ArrayList<>();
        int[] currentRange = new int[]{edits.get(0).getBeginA(), edits.get(0).getEndA()};

        for (Edit edit : edits) {
            int startA = edit.getBeginA();
            int endA = edit.getEndA();

            if (startA <= currentRange[1]) {
                currentRange[1] = Math.max(currentRange[1], endA);
            } else {
                mergedRanges.add(currentRange);
                currentRange = new int[]{startA, endA};
            }
        }
        mergedRanges.add(currentRange);
        return mergedRanges;
    }

    private static List<int[]> getIntersectingEdits(List<Edit> editsA, List<Edit> editsB) {
        List<int[]> intersectingEdits = new ArrayList<>();
        List<int[]> mergedEditsB = mergeIntersectingRanges(editsB);
        for (Edit editA : editsA) {
            for (int[] editB : mergedEditsB) {
                int[] intersection = getIntersectionStartAndEnd(editA, editB);
                if (intersection.length != 0) {
                    intersectingEdits.add(intersection);
                }
            }
        }
        return intersectingEdits;
    }
}