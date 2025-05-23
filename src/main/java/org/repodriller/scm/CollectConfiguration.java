package org.repodriller.scm;

import org.repodriller.filter.diff.DiffFilter;
import org.repodriller.filter.diff.NoDiffFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by mauricioaniche on 17/12/2017.
 */
public class CollectConfiguration {
    private boolean sourceCode = false;
    private boolean diff = false;
    private boolean branches = false;
    private boolean commitMessages = false;
    private List<DiffFilter> diffFilters = Collections.singletonList(new NoDiffFilter());

    /**
     * Configures repodriller to retrieve the current source code of each modified file.
     * @return the current collect configuration
     */
    public CollectConfiguration sourceCode() {
        this.sourceCode = true;
        return this;
    }

    /**
     * Configures repodriller to retrieve the diffs of all modified files in a commit.
     * @return the current collect configuration
     */
    public CollectConfiguration diffs () {
        this.diff = true;
        return this;
    }
    
    /**
     * Configures repodriller to retrieve diffs from a commit based on the specified filters
     * @return the current collect configuration
     */
    public CollectConfiguration diffs(DiffFilter... diffFilters) {
    		this.diff = true;
    		this.diffFilters = Arrays.asList(diffFilters);
    		return this;
    }

    /**
     * Configures repodriller to retrieve the branches that a commit belongs to.
     * Without this configuration, Commit#isMainBranch does not work.
     * @return the current collect configuration
     */
    public CollectConfiguration branches() {
        this.branches = true;
        return this;
    }

    /**
     * Configures repodriller to extract the entire commit message of a commit
     * @return the current collect configuration
     */
    public CollectConfiguration commitMessages() {
        this.commitMessages = true;
        return this;
    }

    /**
     * Configures repodriller to extract just the basic information of a repository,
     * such as authors, files changed, and commit dates.
     * @return the current collect configuration
     */
    public CollectConfiguration basicOnly() {
        sourceCode = false;
        diff = false;
        branches = false;
        commitMessages = false;

        return this;
    }

    /**
     * Configures repodriller to extract everything it can,
     * meaning, source code, diff, branches, and commit messages
     * @return the current collect configuration
     */
    public CollectConfiguration everything () {
        sourceCode = true;
        diff = true;
        branches = true;
        commitMessages = true;

        return this;
    }

    public boolean isCollectingSourceCode () {
        return sourceCode;
    }

    public boolean isCollectingBranches () {
        return branches;
    }

    public boolean isCollectingCommitMessages () {
        return commitMessages;
    }

    public boolean isCollectingDiffs () {
        return diff;
    }
    
    public List<DiffFilter> getDiffFilters() {
    		return this.diffFilters;
    }
}
