package org.repodriller.filter.range;

import org.repodriller.domain.ChangeSet;
import org.repodriller.scm.SCM;

import java.util.List;

public class AllCommits implements CommitRange {

	@Override
	public List<ChangeSet> get(SCM scm) {
		return scm.getChangeSets();
	}

}
