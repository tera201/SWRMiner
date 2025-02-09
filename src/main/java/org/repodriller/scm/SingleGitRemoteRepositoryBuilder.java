package org.repodriller.scm;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.repodriller.util.DataBaseUtil;

public class SingleGitRemoteRepositoryBuilder extends GitRemoteRepositoryBuilder {

	private String gitUrl;

	public SingleGitRemoteRepositoryBuilder() {}
	
	public SingleGitRemoteRepositoryBuilder(String gitUrl) {
		this.gitUrl = gitUrl;
	}
	
	public SingleGitRemoteRepositoryBuilder inTempDir(String tempDir) {
		super.tempDir = tempDir;
		return this;
	}

	public SingleGitRemoteRepositoryBuilder creds(String username, String password) {
		super.username = username;
		super.password = password;
		return this;
	}

	public SingleGitRemoteRepositoryBuilder dateBase(DataBaseUtil dateBase) {
		super.dataBaseUtil = dateBase;
		return this;
	}

	public SingleGitRemoteRepositoryBuilder asBareRepos() {
		super.bare = true;
		return this;
	}

	public GitRemoteRepository build() throws GitAPIException {
		return new GitRemoteRepository(this.gitUrl, this.tempDir, this.bare, this.username, this.password, dataBaseUtil);
	}

	public SCMRepository buildAsSCMRepository() {
		return GitRemoteRepository.singleProject(this.gitUrl, this.tempDir, this.bare, this.username, this.password, dataBaseUtil);
	}

	public SCMRepository getAsSCMRepository() {
		if (this.gitUrl != null)
			return GitRemoteRepository.getSingleProject(this.gitUrl, this.tempDir, dataBaseUtil);
		else return GitRemoteRepository.getSingleProject(this.tempDir, dataBaseUtil);
	}

}