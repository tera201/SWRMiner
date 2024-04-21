package org.repodriller.scm;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.repodriller.RepoDrillerException;
import org.repodriller.domain.*;
import org.repodriller.scm.entities.BlameManager;
import org.repodriller.scm.entities.BlamedLine;
import org.repodriller.scm.entities.CommitSize;
import org.repodriller.scm.entities.DeveloperInfo;
import org.repodriller.util.RDFileUtils;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Juliano Silva
 *
 */
/* TODO Name: Sounds like it inherits SCMRepository, but it actually implements SCM. */
public class SubversionRepository implements SCM {

	private static final int MAX_SIZE_OF_A_DIFF = 100000;
	private static final int DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT = 50;

	private static Logger log = LogManager.getLogger(SubversionRepository.class);

	private String repoName;
	private String path;
	private String username;
	private String password;
	private String workingCopyPath;
	private Integer maxNumberFilesInACommit;

	public SubversionRepository(String path, String username, String password) {
		this(path, username, password, DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT);
	}

	public SubversionRepository(String path, String username, String password, Integer maxNumberOfFilesInACommit) {
		this.path = path;
		this.username = username;
		this.password = password;
		maxNumberOfFilesInACommit = checkMaxNumber(maxNumberOfFilesInACommit);
		this.maxNumberFilesInACommit = maxNumberOfFilesInACommit;

		workingCopyPath = createWorkingCopy();
	}

	private Integer checkMaxNumber(Integer maxNumberOfFilesInACommit) {
		if(maxNumberOfFilesInACommit == null) {
			maxNumberOfFilesInACommit = DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT;
		}
		if(maxNumberOfFilesInACommit <= 0){
			throw new IllegalArgumentException("Max number of files in a commit should be 0 or greater."
					+ "Default value is " + DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT);
		}
		return maxNumberOfFilesInACommit;
	}

	public SubversionRepository(String repositoryPath) {
		this(repositoryPath, null, null, DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT);
	}

	public SubversionRepository(String repositoryPath, Integer maxNumberOfFilesInACommit) {
		this(repositoryPath, null, null, maxNumberOfFilesInACommit);
	}

	public static SCMRepository singleProject(String path) {
		return singleProject(path, DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT);
	}

	public static SCMRepository singleProject(String path, Integer maxNumberOfFilesInACommit) {
		return new SubversionRepository(path, maxNumberOfFilesInACommit).info();
	}

	public static SCMRepository[] allProjectsIn(String path) {
		return allProjectsIn(path, DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT);
	}

	public static SCMRepository[] allProjectsIn(String path, Integer maxNumberOfFilesInACommit) {
		List<SCMRepository> repos = new ArrayList<SCMRepository>();

		for (String dir : RDFileUtils.getAllDirsIn(path)) {
			repos.add(singleProject(dir, maxNumberOfFilesInACommit));
		}

		return repos.toArray(new SCMRepository[repos.size()]);
	}

	public SCMRepository info() {
		SVNRepository repository = null;
		try {
			SVNURL url = SVNURL.parseURIEncoded(path);
			repository = SVNRepositoryFactory.create(url);

			authenticateIfNecessary(repository);

			SVNDirEntry firstRevision = repository.info("/", 0);
			SVNDirEntry lastRevision = repository.info("/", SVNRevision.HEAD.getNumber());

			return new SCMRepository(this, lastRevision.getURL().getPath(), repoName, path, String.valueOf(lastRevision.getRevision()), String.valueOf(firstRevision
					.getRevision()));

		} catch (SVNException e) {
			throw new RuntimeException("error in getHead() for " + path, e);
		} finally {
			if (repository != null)
				repository.closeSession();
		}

	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<ChangeSet> getChangeSets() {
		SVNRepository repository = null;

		try {
			SVNURL url = SVNURL.parseURIEncoded(path);
			repository = SVNRepositoryFactory.create(url);

			authenticateIfNecessary(repository);

			List<ChangeSet> allCs = new ArrayList<ChangeSet>();

			long startRevision = 0;
			long endRevision = -1; // HEAD (the latest) revision
			Collection log = repository.log(new String[] { "" }, null, startRevision, endRevision, true, true);
			for (Iterator iterator = log.iterator(); iterator.hasNext();) {
				SVNLogEntry entry = (SVNLogEntry) iterator.next();
				allCs.add(new ChangeSet(String.valueOf(entry.getRevision()), convertToCalendar(entry.getDate())));
			}

			return allCs;

		} catch (SVNException e) {
			throw new RuntimeException("error in getHead() for " + path, e);
		} finally {
			if (repository != null)
				repository.closeSession();
		}
	}

	@Override
	public void createCommit(String message) {

	}

	@Override
	public void resetLastCommitsWithMessage(String message) {

	}

	@SuppressWarnings("rawtypes")
	@Override
	/* TODO Refactor as in GitRepository.getCommit. */
	public Commit getCommit(String id) {

		SVNRepository repository = null;

		try {
			SVNURL url = SVNURL.parseURIEncoded(path);
			repository = SVNRepositoryFactory.create(url);

			authenticateIfNecessary(repository);

			long revision = Long.parseLong(id);
			long startRevision = revision;
			long endRevision = revision;

			Collection repositoryLog = repository.log(new String[] { "" }, null, startRevision, endRevision, true, true);

			for (Iterator iterator = repositoryLog.iterator(); iterator.hasNext();) {
				SVNLogEntry logEntry = (SVNLogEntry) iterator.next();

				Commit commit = createCommit(logEntry);

				List<Modification> modifications = getModifications(repository, url, revision, logEntry);

				if (modifications.size() > this.maxNumberFilesInACommit) {
					log.error("commit " + id + " has more than files than the limit");
					throw new RuntimeException("commit " + id + " too big, sorry");
				}

				commit.addModifications(modifications);

				return commit;
			}

		} catch (Exception e) {
			throw new RuntimeException("error in getCommit() for " + path, e);
		} finally {
			if (repository != null)
				repository.closeSession();
		}
		return null;
	}

	@Override
	public List<Ref> getAllBranches() {
		return null;
	}

	@Override
	public List<Ref> getAllTags() {
		return null;
	}

	@Override
	public void checkoutTo(String branch) {
	}

	@Override
	public String getCurrentBranchOrTagName() {
		return null;
	}

	private Commit createCommit(SVNLogEntry logEntry) {
		Developer committer = new Developer(logEntry.getAuthor(), null);
		Calendar date = convertToCalendar(logEntry.getDate());
		Commit commit = new Commit(String.valueOf(logEntry.getRevision()), null, committer, date, date, logEntry.getMessage(),
				null);
		return commit;
	}

	private List<Modification> getModifications(SVNRepository repository, SVNURL url, long revision, SVNLogEntry logEntry) throws SVNException,
			UnsupportedEncodingException {

		List<Modification> modifications = new ArrayList<Modification>();
		for (Entry<String, SVNLogEntryPath> entry : logEntry.getChangedPaths().entrySet()) {
			SVNLogEntryPath e = entry.getValue();

			String diffText = getDiffText(repository, url, e, revision);

			String sc = getSourceCode(repository, revision, e);

			Modification modification = new Modification(e.getCopyPath(), e.getPath(), getModificationType(e), diffText, sc);
			modifications.add(modification);
		}

		return modifications;
	}

	private String getSourceCode(SVNRepository repository, long endRevision, SVNLogEntryPath e) throws SVNException, UnsupportedEncodingException {
		if (e.getType() == 'D')
			return "";

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		repository.getFile(e.getPath(), endRevision, null, out);

		String sc = out.toString("UTF-8");
		return sc;
	}

	private String getDiffText(SVNRepository repository, SVNURL url, SVNLogEntryPath entry, long revision) {
		try {
			SVNClientManager clientManager = SVNClientManager.newInstance(null, repository.getAuthenticationManager());
			SVNDiffClient diffClient = clientManager.getDiffClient();

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			SVNRevision startRevision = SVNRevision.create(revision - 1);
			SVNRevision endRevision = SVNRevision.create(revision);

			diffClient.doDiff(url, startRevision, startRevision, endRevision, SVNDepth.FILES, true, out);

			String diffText = out.toString("UTF-8");
			if (diffText.length() > MAX_SIZE_OF_A_DIFF) {
				log.error("diffs for " + entry.getPath() + " too big");
				diffText = "-- TOO BIG --";
			}
			return diffText;

		} catch (Exception e) {
			return "";
		}
	}
	
	@Override
	public List<Modification> getDiffBetweenCommits(String priorCommit, String laterCommit) {
		// TODO Not yet implemented for SVN.
		throw new RepoDrillerException("This feature has not yet been implemented for Subversion repos.");
	}

	@Override
	public Map<String, CommitSize> repositoryAllSize() {
		// TODO Not yet implemented for SVN.
		throw new RepoDrillerException("This feature has not yet been implemented for Subversion repos.");
	}

	private ModificationType getModificationType(SVNLogEntryPath e) {
		if (e.getType() == 'A') {
			return ModificationType.ADD;
		} else if (e.getType() == 'D') {
			return ModificationType.DELETE;
		} else if (e.getType() == 'M') {
			return ModificationType.MODIFY;
		} else if (e.getType() == 'R') {
			return ModificationType.COPY;
		}
		return null;
	}

	@Override
	public ChangeSet getHead() {
		SVNRepository repository = null;

		try {
			SVNURL url = SVNURL.parseURIEncoded(path);
			repository = SVNRepositoryFactory.create(url);

			authenticateIfNecessary(repository);

			SVNDirEntry entry = repository.info("/", -1);
			return new ChangeSet(String.valueOf(entry.getRevision()), convertToCalendar(entry.getDate()));

		} catch (SVNException e) {
			throw new RuntimeException("error in getHead() for " + path, e);
		} finally {
			if (repository != null)
				repository.closeSession();
		}
	}

	private GregorianCalendar convertToCalendar(Date date) {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		return calendar;
	}

	@Override
	public List<RepositoryFile> files() {
		List<RepositoryFile> all = new ArrayList<RepositoryFile>();
		for (File f : getAllFilesInPath()) {
			if (isNotAnImportantFile(f))
				continue;
			all.add(new RepositoryFile(f));
		}

		return all;
	}

	private List<File> getAllFilesInPath() {
		return RDFileUtils.getAllFilesInPath(workingCopyPath);
	}

	private boolean isNotAnImportantFile(File f) {
		return f.getName().equals(".DS_Store");
	}

	@Override
	public long totalCommits() {
		return getChangeSets().size();
	}

	@Override
	public void reset() {
		SVNRepository repository = null;
		try {
			SVNRevision revision = SVNRevision.HEAD;

			SVNURL url = SVNURL.parseURIEncoded(path);
			repository = SVNRepositoryFactory.create(url);

			authenticateIfNecessary(repository);

			SVNClientManager ourClientManager = SVNClientManager.newInstance(null, repository.getAuthenticationManager());
			SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
			updateClient.setIgnoreExternals(false);
			updateClient.doCheckout(url, new File(workingCopyPath), revision, revision, SVNDepth.INFINITY, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (repository != null)
				repository.closeSession();
		}
	}

	@Override
	public void checkout(String id) {
		SVNRepository repository = null;
		try {
			clearWorkingCopy();

			SVNRevision revision = SVNRevision.create(Integer.parseInt(id));

			SVNURL url = SVNURL.parseURIEncoded(path);
			repository = SVNRepositoryFactory.create(url);

			authenticateIfNecessary(repository);

			SVNClientManager ourClientManager = SVNClientManager.newInstance(null, repository.getAuthenticationManager());
			SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
			updateClient.setIgnoreExternals(false);
			updateClient.doCheckout(url, new File(workingCopyPath), revision, revision, SVNDepth.INFINITY, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (repository != null)
				repository.closeSession();
		}
	}

	private void clearWorkingCopy() {
		try {
			org.apache.commons.io.FileUtils.cleanDirectory(new File(workingCopyPath));
		} catch (IOException e) {
			throw new RuntimeException("Unable to clean working copy path", e);
		}
	}

	@Override
	@Deprecated
	public String blame(String file, String currentCommit, Integer line) {
		try {
			SVNURL url = SVNURL.parseURIEncoded(path + File.separator + file);

			ISVNAuthenticationManager authManager = getAuthenticationManager();

			SVNLogClient logClient = SVNClientManager.newInstance(null, authManager).getLogClient();
			boolean ignoreMimeType = false;
			boolean includeMergedRevisions = false;

			logClient.doAnnotate(url, SVNRevision.UNDEFINED, SVNRevision.create(Integer.parseInt(currentCommit)), SVNRevision.HEAD, ignoreMimeType,
					includeMergedRevisions, null, null);

			return String.valueOf(SVNRevision.create(Integer.parseInt(currentCommit)).getNumber());

		} catch (SVNException e) {
			throw new RuntimeException(e);
		}
	}

	private void authenticateIfNecessary(SVNRepository repository) {
		ISVNAuthenticationManager authManager = getAuthenticationManager();
		if (authManager != null)
			repository.setAuthenticationManager(authManager);
	}

	private BasicAuthenticationManager getAuthenticationManager() {
		if (username != null && password != null) {
			return BasicAuthenticationManager.newInstance(username, password.toCharArray());
		}
		return null;
	}

	private String createWorkingCopy() {
		String tmpDirPath = System.getProperty("java.io.tmpdir");
		File tmpDir = new File(tmpDirPath + File.separator + "metricminer");
		if (!tmpDir.exists()) {
			boolean created = tmpDir.mkdirs();
			if (!created) {
				throw new RuntimeException("Unable to create temporary folder for working copy in " + tmpDir);
			}
		}

		return tmpDir.getPath();
	}

	public String getPath() {
		return path;
	}

	@Override
	public Map<String, CommitSize> currentRepositorySize() {
		throw new RuntimeException("implement me!");
	}

	@Override
	public Map<String, CommitSize> repositorySize(String branchOrTag, String filePath) {
		throw new RuntimeException("implement me!");
	}

	@Override
	public List<BlamedLine> blame(String file) {
		throw new RuntimeException("implement me!");
	}

	@Override
	public BlameManager blameManager() {
		throw new RuntimeException("implement me!");
	}

	@Override
	public List<BlamedLine> blame(String file, String currentCommit, boolean priorCommit) {
		// pull request me!
		throw new RuntimeException("implement me!");
	}

	@Override
	public Map<String, DeveloperInfo> getDeveloperInfo() {
		throw new RuntimeException("implement me!");
	}

	@Override
	public Map<String, DeveloperInfo> getDeveloperInfo(String nodePath) throws IOException, GitAPIException {
		throw new RuntimeException("implement me!");
	}

	public Integer getMaxNumberFilesInACommit() {
		return maxNumberFilesInACommit;
	}

	@Override
	public String getCommitFromTag(String tag) {
		// pull request me!
		throw new RuntimeException("implement me!");
	}

	@Override
	public SCM clone(Path dest) {
		log.info("Cloning to " + dest);
		RDFileUtils.copyDirTree(Paths.get(path), dest);
		return new GitRepository(dest.toString());
	}

	@Override
	public void delete() {
		// allow to be destroyed more than once
		if (RDFileUtils.exists(Paths.get(path))) {
			log.info("Deleting: " + path);
			try {
				FileUtils.deleteDirectory(new File(path.toString()));
			} catch (IOException e) {
				log.info("Delete failed: " + e);
			}
		}
	}

	@Override
	public void setDataToCollect (CollectConfiguration config) {
		throw new RuntimeException("SVN does not accept a different collect configuration. What about you sending us a PR? ;)");
	}
}
