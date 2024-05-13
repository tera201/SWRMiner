package org.repodriller.scm;

import org.repodriller.util.DataBaseUtil;

public abstract class GitRemoteRepositoryBuilder {

	protected String tempDir;
	protected boolean bare = false;
	protected String username;
	protected String password;
	protected DataBaseUtil dataBaseUtil;
	
}