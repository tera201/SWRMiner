package org.repodriller.filter.range;

import org.repodriller.domain.ChangeSet;
import org.repodriller.scm.SCM;

import java.util.Arrays;
import java.util.List;

public class OnlyInHead implements CommitRange {

	@Override
	public List<ChangeSet> get(SCM scm) {
		return Arrays.asList(scm.getHead());
	}

}
