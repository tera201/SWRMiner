/**
 * Copyright 2014 Maurício Aniche

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.repodriller.scm;

import lombok.Getter;

/**
 * An SCMRepository represents a Source Code Management Repository, i.e. an instance of source code maintained with a version control system.
 * An SCMRepository includes:
 *  - some metadata about the repo
 *  - an SCM instance with the contents of the repo
 *
 * @author Mauricio Aniche
 */
/* TODO Naming is confusing. */
public class SCMRepository {

	@Getter
	private String repoName;
	@Getter
    private String path; /* Path in local FS. */
	@Getter
    private String headCommit; /* Most recent commit. */
	@Getter
    private String firstCommit; /* First commit. */
	@Getter
    private SCM scm;
	private String origin; /* e.g. GitHub URL */

	public SCMRepository(SCM scm, String origin, String repoName, String path, String headCommit, String firstCommit) {
		this.scm = scm;
		this.origin = origin;
		this.repoName = repoName;
		this.path = path;
		this.headCommit = headCommit;
		this.firstCommit = firstCommit;
	}

    public String getOrigin() {
		return origin == null ? path : origin;
	}

	public String getLastDir() {
		String[] dirs = path.replace("\\", "/").split("/");
		return dirs[dirs.length-1];
	}

	@Override
	public String toString() {
		return "SCMRepository [path=" + path + ", headCommit=" + headCommit + ", lastCommit=" + firstCommit + ", scm="
				+ scm + ", origin=" + origin + "]";
	}

}
