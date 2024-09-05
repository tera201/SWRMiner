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

import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.*;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.repodriller.RepoDrillerException;
import org.repodriller.domain.*;
import org.repodriller.filter.diff.DiffFilter;
import org.repodriller.scm.entities.*;
import org.repodriller.scm.exceptions.CheckoutException;
import org.repodriller.util.DataBaseUtil;
import org.repodriller.util.FileEntity;
import org.repodriller.util.PathUtils;
import org.repodriller.util.RDFileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Everything you need to work with a Git-based source code repository.
 *
 *
 * @author Mauricio Aniche
 */
/* TODO Name: Sounds like it inherits SCMRepository, but it actually implements SCM. */
public class GitRepository implements SCM {

	/* Constants. */
	private static final int MAX_SIZE_OF_A_DIFF = 100000;
	private static final int DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT = 5000;
	private static final String BRANCH_MM = "mm"; /* TODO mm -> rd. */

	/* Auto-determined. */
	private String mainBranchName = null;
	private int maxNumberFilesInACommit = -1; /* TODO Expose an API to control this value? Also in SubversionRepository. */
	private int maxSizeOfDiff = -1; /* TODO Expose an API to control this value? Also in SubversionRepository. */

	private CollectConfiguration collectConfig;

	private static Logger log = LogManager.getLogger(GitRepository.class);

	private String repoName = null;

	/* User-specified. */
	private String path = null;
	private boolean firstParentOnly = false;
	Map<ObjectId, Long> sizeCache = new ConcurrentHashMap<>();
	protected DataBaseUtil dataBaseUtil;
	protected Integer projectId;

	/**
	 * Intended for sub-classes.
	 * Make sure you initialize appropriately with the Setters.
	 */
	protected GitRepository() {
		this(null);
	}

	public GitRepository(String path) {
		this(path, false);
	}

	public GitRepository(String path, boolean firstParentOnly) {
		log.debug("Creating a GitRepository from path " + path);
		setPath(path);
		setFirstParentOnly(firstParentOnly);
		maxNumberFilesInACommit = checkMaxNumberOfFiles();
		maxSizeOfDiff = checkMaxSizeOfDiff();

		this.collectConfig = new CollectConfiguration().everything();
	}

	public GitRepository(String path, boolean firstParentOnly, DataBaseUtil dataBaseUtil) {
		log.debug("Creating a GitRepository from path " + path);
		this.dataBaseUtil = dataBaseUtil;
		String[] splitPath = path.replace("\\", "/").split("/");
		setRepoName(splitPath[splitPath.length - 1]);
		Integer projectId = dataBaseUtil.getProjectId(repoName, path);
		this.projectId = Objects.requireNonNullElseGet(projectId, () -> dataBaseUtil.insertProject(repoName, path));
		setPath(path);
		setFirstParentOnly(firstParentOnly);

		maxNumberFilesInACommit = checkMaxNumberOfFiles();
		maxSizeOfDiff = checkMaxSizeOfDiff();

		this.collectConfig = new CollectConfiguration().everything();
	}

	public static SCMRepository singleProject(String path) {
		return new GitRepository(path).info();
	}

	public static SCMRepository singleProject(String path, boolean singleParentOnly, DataBaseUtil dataBaseUtil) {
		return new GitRepository(path, singleParentOnly, dataBaseUtil).info();
	}

	public static SCMRepository[] allProjectsIn(String path) {
		return allProjectsIn(path, false);
	}

	public static SCMRepository[] allProjectsIn(String path, boolean singleParentOnly) {
		List<SCMRepository> repos = new ArrayList<>();

		for (String dir : RDFileUtils.getAllDirsIn(path)) {
			repos.add(singleProject(dir, singleParentOnly, null));
		}

		return repos.toArray(new SCMRepository[repos.size()]);
	}

	public SCMRepository info() {
		try (Git git = openRepository(); RevWalk rw = new RevWalk(git.getRepository())) {
			AnyObjectId headId = git.getRepository().resolve(Constants.HEAD);

			RevCommit root = rw.parseCommit(headId);
			rw.sort(RevSort.REVERSE);
			rw.markStart(root);
			RevCommit lastCommit = rw.next();

			String origin = git.getRepository().getConfig().getString("remote", "origin", "url");

			return new SCMRepository(this, origin, repoName, path, headId.getName(), lastCommit.getName());
		} catch (Exception e) {
			throw new RuntimeException("Couldn't create JGit instance with path " + path);
		}
	}

	public SCMRepository getInfo() {
		RevWalk rw = null;
		try (Git git = openRepository()) {
			ObjectId head = git.getRepository().resolve(Constants.HEAD);

			rw = new RevWalk(git.getRepository());
			RevCommit root = rw.parseCommit(head);
			rw.sort(RevSort.REVERSE);
			rw.markStart(root);
			RevCommit lastCommit = rw.next();
			String origin = git.getRepository().getConfig().getString("remote", "origin", "url");

			String repoName = GitRemoteRepository.repoNameFromURI(origin);

			return new SCMRepository(this, origin, repoName, path, head.getName(), lastCommit.getName());
		} catch (Exception e) {
			throw new RuntimeException("Couldn't create JGit instance with path " + path);
		}
	}

	public Git openRepository() throws IOException, GitAPIException {
		Git git = Git.open(new File(path));
		if (this.mainBranchName == null) {
			this.mainBranchName = discoverMainBranchName(git);
		}
		return git;
	}

	private String discoverMainBranchName(Git git) throws IOException {
		return git.getRepository().getBranch();
	}

	public ChangeSet getHead() {
		RevWalk revWalk = null;
		try (Git git = openRepository()) {
			ObjectId head = git.getRepository().resolve(Constants.HEAD);

			revWalk = new RevWalk(git.getRepository());
			RevCommit r = revWalk.parseCommit(head);
			git.close();
			return new ChangeSet(r.getName(), convertToDate(r));

		} catch (Exception e) {
			throw new RuntimeException("error in getHead() for " + path, e);
		} finally {
			revWalk.close();
		}
	}

	@Override
	public List<ChangeSet> getChangeSets() {
		try (Git git = openRepository()) {
			List<ChangeSet> allCs;
			if (!firstParentOnly) allCs = getAllCommits(git);
			else allCs = firstParentsOnly(git);
			git.close();
			return allCs;
		} catch (Exception e) {
			throw new RuntimeException("error in getChangeSets for " + path, e);
		}
	}

	@Override
	public void createCommit(String message) {
		try (Git git = openRepository()) {
			Status status = git.status().call();
			if(status.hasUncommittedChanges()) {
			AddCommand add = git.add();
				for (String entry : status.getModified()) {
					add.addFilepattern(entry);
				}
				add.call();
				git.commit().setMessage(message).call();
			}
		} catch (Exception e) {
			throw new RuntimeException("error in create commit for " + path, e);
		}
	}

	@Override
	public void  resetLastCommitsWithMessage(String message) {
		try (Git git = openRepository()) {
			RevCommit commit = null;
			for (RevCommit r : git.log().call()) {
				if (!r.getFullMessage().contains(message)) {
					commit = r;
					break;
				}
			}
			if (commit != null) {
				git.reset().setMode(ResetType.MIXED).setRef(extractChangeSet(commit).getId()).call();
			} else {
				log.info("Reset doesn't required");
			}

		} catch (Exception e) {
			throw new RuntimeException("Reset failed ", e);
		}
	}

	private List<ChangeSet> firstParentsOnly(Git git) {
		RevWalk revWalk = null;
		try {
			List<ChangeSet> allCs = new ArrayList<>();

			revWalk = new RevWalk(git.getRepository());
			revWalk.setRevFilter(new FirstParentFilter());
			revWalk.sort(RevSort.TOPO);
			Ref headRef = git.getRepository().findRef(Constants.HEAD);
			RevCommit headCommit = revWalk.parseCommit(headRef.getObjectId());
			revWalk.markStart(headCommit);
			for (RevCommit revCommit : revWalk) {
				allCs.add(extractChangeSet(revCommit));
			}

			return allCs;

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			revWalk.close();
		}
	}

	private List<ChangeSet> getAllCommits(Git git) throws GitAPIException, IOException {
		List<ChangeSet> allCs = new ArrayList<>();

		for (RevCommit r : git.log().call()) {
			allCs.add(extractChangeSet(r));

		}
		return allCs;
	}

	private ChangeSet extractChangeSet(RevCommit r) {
		String hash = r.getName();
		GregorianCalendar date = convertToDate(r);

		return new ChangeSet(hash, date);
	}

	private GregorianCalendar convertToDate(RevCommit revCommit) {
		GregorianCalendar date = new GregorianCalendar();
		date.setTimeZone(revCommit.getAuthorIdent().getTimeZone());
		date.setTime(revCommit.getAuthorIdent().getWhen());

		return date;
	}

	/**
	 * Get the commit with this commit id.
	 * Caveats:
	 *   - If commit modifies more than maxNumberFilesInACommit, throws an exception
	 *   - If one of the file diffs exceeds maxSizeOfDiff, the diffText is discarded
	 *
	 * @param id    The SHA1 hash that identifies a git commit.
	 * @returns Commit 	The corresponding Commit, or null.
	 */
	@Override
	public Commit getCommit(String id) {
		try (Git git = openRepository()) {
			/* Using JGit, this commit will be the first entry in the log beginning at id. */
			Repository repo = git.getRepository();
			Iterable<RevCommit> jgitCommits = git.log().add(repo.resolve(id)).call();
			Iterator<RevCommit> itr = jgitCommits.iterator();

			if (!itr.hasNext())
				return null;
			RevCommit jgitCommit = itr.next();

			/* Extract metadata. */
			Developer author = new Developer(jgitCommit.getAuthorIdent().getName(), jgitCommit.getAuthorIdent().getEmailAddress());
			Developer committer = new Developer(jgitCommit.getCommitterIdent().getName(), jgitCommit.getCommitterIdent().getEmailAddress());
			TimeZone authorTimeZone = jgitCommit.getAuthorIdent().getTimeZone();
			TimeZone committerTimeZone = jgitCommit.getCommitterIdent().getTimeZone();

			String msg = collectConfig.isCollectingCommitMessages() ? jgitCommit.getFullMessage().trim() : "";
			String hash = jgitCommit.getName().toString();
			List<String> parents = Arrays.stream(jgitCommit.getParents())
					.map(rc -> rc.getName().toString()).collect(Collectors.toList());

			GregorianCalendar authorDate = new GregorianCalendar();
			authorDate.setTime(jgitCommit.getAuthorIdent().getWhen());
			authorDate.setTimeZone(jgitCommit.getAuthorIdent().getTimeZone());

			GregorianCalendar committerDate = new GregorianCalendar();
			committerDate.setTime(jgitCommit.getCommitterIdent().getWhen());
			committerDate.setTimeZone(jgitCommit.getCommitterIdent().getTimeZone());

			boolean isMerge = (jgitCommit.getParentCount() > 1);

			Set<String> branches = getBranches(git, hash);
			boolean isCommitInMainBranch = branches.contains(this.mainBranchName);

			/* Create one of our Commit's based on the jgitCommit metadata. */
			Commit commit = new Commit(hash, author, committer, authorDate, authorTimeZone, committerDate, committerTimeZone, msg, parents, isMerge, branches, isCommitInMainBranch);

			/* Convert each of the associated DiffEntry's to a Modification. */
			List<DiffEntry> diffsForTheCommit = diffsForTheCommit(repo, jgitCommit);
			if (diffsForTheCommit.size() > maxNumberFilesInACommit) {
				String errMsg = "Commit " + id + " touches more than " + maxNumberFilesInACommit + " files";
				log.error(errMsg);
				throw new RepoDrillerException(errMsg);
			}

			for (DiffEntry diff : diffsForTheCommit) {
				if (this.diffFiltersAccept(diff)) {
					Modification m = this.diffToModification(repo, diff);
					commit.addModification(m);
				}
			}

			git.close();
			return commit;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("error detailing " + id + " in " + path, e);
		}
	}

	private Set<String> getBranches(Git git, String hash) throws GitAPIException {

		if(!collectConfig.isCollectingBranches())
			return new HashSet<>();

		List<Ref> gitBranches = git.branchList().setContains(hash).call();
		Set<String> mappedBranches = gitBranches.stream()
				.map(
					  (ref) -> ref.getName().substring(ref.getName().lastIndexOf("/") + 1))
				.collect(Collectors.toSet());
		return mappedBranches;
	}
	@Override
	public List<Ref> getAllBranches() {
		try (Git git = openRepository()) {
			return git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
		} catch (Exception e) {
			throw new RuntimeException("error getting branches in " + path,
					e);
		}
	}
	@Override
	public List<Ref> getAllTags() {
		try (Git git = openRepository()) {
			return git.tagList().call();
		} catch (Exception e) {
			throw new RuntimeException("error getting tags in " + path,
					e);
		}
	}

	@Override
	public void checkoutTo(String branch) throws CheckoutException {
		try (Git git = openRepository()) {
			if (git.getRepository().isBare()) throw new CheckoutException("error repo is bare");

			if (git.getRepository().findRef(branch) == null) {
				throw new CheckoutException("Branch does not exist: " + branch);
			}

			Status status = git.status().call();
			if (status.hasUncommittedChanges()) {
				throw new CheckoutException("There are uncommitted changes in the working directory");
			}

			git.checkout().setName(branch).call();
		} catch (IOException | GitAPIException e) {
			throw new CheckoutException("Error checking out to " + branch,
					e);
		}
    }

	public String getCurrentBranchOrTagName() {
		try (Git git = openRepository()) {
			ObjectId head = git.getRepository().resolve("HEAD");
			return git.getRepository().getAllRefsByPeeledObjectId().get(head).stream()
					.map(Ref::getName)
					.distinct().filter(it -> it.startsWith("refs/")).findFirst().orElse(head.getName());
		}  catch (Exception e) {
			throw new RuntimeException("Error getting branch name", e);
		}
	}

	private Modification diffToModification(Repository repo, DiffEntry diff) throws IOException {
		ModificationType change = Enum.valueOf(ModificationType.class, diff.getChangeType().toString());

		String oldPath = diff.getOldPath();
		String newPath = diff.getNewPath();

		String diffText = "";
		String sc = "";
		if (diff.getChangeType() != ChangeType.DELETE) {
			diffText = getDiffText(repo, diff);
			sc = getSourceCode(repo, diff);
		}

		if (diffText.length() > maxSizeOfDiff) {
			log.error("diff for " + newPath + " too big");
			diffText = "-- TOO BIG --";
		}

		return new Modification(oldPath, newPath, change, diffText, sc);
	}

	private List<DiffEntry> diffsForTheCommit(Repository repo, RevCommit commit) throws IOException {

		AnyObjectId currentCommit = repo.resolve(commit.getName());
		AnyObjectId parentCommit = commit.getParentCount() > 0 ? repo.resolve(commit.getParent(0).getName()) : null;

		return this.getDiffBetweenCommits(repo, parentCommit, currentCommit);
	}

	@Override
	public List<Modification> getDiffBetweenCommits(String priorCommitHash, String laterCommitHash) {
		try (Git git = openRepository()) {
			Repository repo = git.getRepository();
			AnyObjectId priorCommit = repo.resolve(priorCommitHash);
			AnyObjectId laterCommit = repo.resolve(laterCommitHash);

			List<DiffEntry> diffs = this.getDiffBetweenCommits(repo, priorCommit, laterCommit);
			List<Modification> modifications = diffs.stream()
				.map(diff -> {
						try {
							return this.diffToModification(repo, diff);
						} catch (IOException e) {
							throw new RuntimeException("error diffing " + priorCommitHash + " and " + laterCommitHash + " in " + path, e);
						}
				})
				.collect(Collectors.toList());
			git.close();
			return modifications;
		} catch (Exception e) {
			throw new RuntimeException("error diffing " + priorCommitHash + " and " + laterCommitHash + " in " + path,
					e);
		}
	}

	private List<DiffEntry> getDiffBetweenCommits(Repository repo, AnyObjectId parentCommit,
			AnyObjectId currentCommit) {
		try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

			df.setBinaryFileThreshold(2 * 1024); // 2 mb max a file
			df.setRepository(repo);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);

			setContext(df);

			List<DiffEntry> diffs = null;
			if (parentCommit == null) {
				try (RevWalk rw = new RevWalk(repo)) {
					RevCommit commit = rw.parseCommit(currentCommit);
					diffs = df.scan(new EmptyTreeIterator(),
							new CanonicalTreeParser(null, rw.getObjectReader(), commit.getTree()));
				}
			} else {
				diffs = df.scan(parentCommit, currentCommit);
			}
			return diffs;
		} catch (IOException e) {
			throw new RuntimeException(
					"error diffing " + parentCommit.getName() + " and " + currentCommit.getName() + " in " + path, e);
		}
	}

	private void setContext(DiffFormatter df) {
		try {
			int context = getSystemProperty("git.diffcontext"); /* TODO: make it into a configuration */
			df.setContext(context);
		} catch (Exception e) {
			return;
		}
	}

	private String getSourceCode(Repository repo, DiffEntry diff) throws IOException {

		if(!collectConfig.isCollectingSourceCode()) return "";

		try {
			ObjectReader reader = repo.newObjectReader();
			byte[] bytes = reader.open(diff.getNewId().toObjectId()).getBytes();
			return new String(bytes, "utf-8");
		} catch (Throwable e) {
			return "";
		}
	}

	private String getDiffText(Repository repo, DiffEntry diff) throws IOException {

		if(!collectConfig.isCollectingDiffs())
			return "";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DiffFormatter df2 = new DiffFormatter(out)) {
            String diffText;
            df2.setRepository(repo);
			df2.format(diff);
			diffText = out.toString("UTF-8");
			return diffText;
		} catch (Throwable e) {
			return "";
		}
	}

	public synchronized void checkout(String hash) {
		try (Git git = openRepository()) {
			git.reset().setMode(ResetType.HARD).call();
			git.checkout().setName(mainBranchName).call();
			deleteMMBranch(git);
			git.checkout().setCreateBranch(true).setName(BRANCH_MM).setStartPoint(hash).setForce(true).setOrphan(true).call();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private synchronized void deleteMMBranch(Git git) throws GitAPIException {
		List<Ref> refs = git.branchList().call();
		for (Ref r : refs) {
			if (r.getName().endsWith(BRANCH_MM)) {
				git.branchDelete().setBranchNames(BRANCH_MM).setForce(true).call();
				break;
			}
		}
	}

	public synchronized List<RepositoryFile> files() {
		List<RepositoryFile> all = new ArrayList<>();
		for (File f : getAllFilesInPath()) {
			all.add(new RepositoryFile(f));
		}

		return all;
	}

	public synchronized void reset() {
		try (Git git = openRepository()) {
			git.checkout().setName(mainBranchName).setForce(true).call();
			git.branchDelete().setBranchNames(BRANCH_MM).setForce(true).call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<File> getAllFilesInPath() {
		return RDFileUtils.getAllFilesInPath(path);
	}

	@Override
	public long totalCommits() {
		return getChangeSets().size();
	}

	@Override
	public Map<String, CommitSize> repositoryAllSize() {
		return repositorySize(true, null, null);
	}

	@Override
	public Map<String, CommitSize> currentRepositorySize() {
		return repositorySize(null, null);
	}

	@Override
	public Map<String, CommitSize> repositorySize(String filePath) {
		return repositorySize(false, null, filePath);
	}

	@Override
	public Map<String, CommitSize> repositorySize(String branchOrTag, String filePath) {
		return repositorySize(false, branchOrTag, filePath);
	}

	private Map<String, CommitSize> repositorySize(Boolean all, String branchOrTag, String filePath) {
			filePath = Objects.equals(filePath, path) ? "" : filePath;
			String localPath = filePath != null && filePath.startsWith(path) ? filePath.substring(path.length() + 1).replace("\\", "/") : "";
        return dataBaseUtil.getCommitSizeMap(projectId, localPath);
	}

	@Override
	@Deprecated
	public String blame(String file, String commitToBeBlamed, Integer line) {
		return blame(file, commitToBeBlamed).get(line).getCommit();
	}

	public List<BlamedLine> blame(String file, String commitToBeBlamed) {
		return blame(file, commitToBeBlamed, true);
	}

	public List<BlamedLine> blame(String file) {
		try (Git git = openRepository()) {
			BlameResult blameResult = git.blame().setFilePath(file.replace("\\", "/")).setFollowFileRenames(true).call();
			if (blameResult != null) {
				int rows = blameResult.getResultContents().size();
				List<BlamedLine> result = new ArrayList<>();
				for (int i = 0; i < rows; i++) {
					result.add(new BlamedLine(i,
							blameResult.getResultContents().getString(i),
							blameResult.getSourceAuthor(i).getName(),
							blameResult.getSourceCommitter(i).getName(),
							blameResult.getSourceCommit(i).getId().getName()));
				}

				return result;
			} else {
				// TODO create notification
				System.out.println("BlameResult not found. File: " + file);
				return new ArrayList<>();
//				throw new RuntimeException("BlameResult not found. File: " + file);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public BlameManager blameManager() {
		try (Git git = openRepository()) {
			Map<String, BlameFileInfo> fileMap = new HashMap<>();

			for (RepositoryFile file : files()) {
				String localFilePath = file.getFile().getPath().substring(path.length() + 1).replace("\\", "/");
				BlameResult blameResult = git.blame().setFilePath(localFilePath).setFollowFileRenames(true).call();

				if (blameResult != null) {
					int rows = blameResult.getResultContents().size();
					for (int i = 0; i < rows; i++) {
						String author = blameResult.getSourceAuthor(i).getName();
						String fileName = blameResult.getSourcePath(i);
						RevCommit commit = blameResult.getSourceCommit(i);
						BlameAuthorInfo blameAuthorInfo = new BlameAuthorInfo(author, Collections.singleton(commit), 1, blameResult.getResultContents().getString(i).getBytes().length);
						fileMap.computeIfAbsent(blameResult.getSourcePath(i), k -> new BlameFileInfo(fileName)).add(blameAuthorInfo);
					}
				} else {
					// TODO create notification
					System.out.println("BlameResult not found. File: " + file + " localFilePath: " + localFilePath);
					//	throw new RuntimeException("BlameResult not found. File: " + file + " localFilePath: " + localFilePath);
				}
			}
			return new BlameManager(fileMap, repoName);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<BlamedLine> blame(String file, String commitToBeBlamed, boolean priorCommit) {
		try (Git git = openRepository()) {
			ObjectId gitCommitToBeBlamed;
			if (priorCommit) {
				Iterable<RevCommit> commits = git.log().add(git.getRepository().resolve(commitToBeBlamed)).call();
				gitCommitToBeBlamed = commits.iterator().next().getParent(0).getId();
			} else {
				gitCommitToBeBlamed = git.getRepository().resolve(commitToBeBlamed);
			}

			BlameResult blameResult = git.blame().setFilePath(file.replace("\\", "/")).setStartCommit(gitCommitToBeBlamed).setFollowFileRenames(true).call();
			if (blameResult != null) {
				int rows = blameResult.getResultContents().size();
				List<BlamedLine> result = new ArrayList<>();
				for (int i = 0; i < rows; i++) {
					result.add(new BlamedLine(i,
							blameResult.getResultContents().getString(i),
							blameResult.getSourceAuthor(i).getName(),
							blameResult.getSourceCommitter(i).getName(),
							blameResult.getSourceCommit(i).getId().getName()));
				}

				return result;
			} else {
				// TODO create notification
				System.out.println("BlameResult not found. File: " + file);
				return new ArrayList<>();
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, DeveloperInfo> getDeveloperInfo() throws IOException, GitAPIException {
		return getDeveloperInfo(null);
	}

	public void dbPrepared() {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<?>> futures = new ArrayList<>();
		try (Git git = openRepository()) {

			List<RevCommit> commits = StreamSupport.stream(git.log().call().spliterator(), true)
					.filter(commit -> !dataBaseUtil.isCommitExist(commit.getName()))
					.toList();

			ConcurrentHashMap<String, Integer> authorIdCache = new ConcurrentHashMap<>();

			for (int i = 0; i < commits.size(); i++) {
				final RevCommit commit = commits.get(i);

				Future<?> future = executor.submit(() -> {
					try {
						List<org.repodriller.scm.entities.FileEntity> fileList = new ArrayList<>();
						PersonIdent author = commit.getAuthorIdent();
						Integer authorId = authorIdCache.computeIfAbsent(author.getEmailAddress(), e -> {
							Integer id = dataBaseUtil.getAuthorId(projectId, e);
							return id != null ? id : dataBaseUtil.insertAuthor(projectId, author.getName(), e);
						});
						Map<String, FileEntity> paths = GitRepositoryUtil.getCommitsFiles(commit, git);
						long start = System.currentTimeMillis();
						double commitStability = CommitStabilityAnalyzer.analyzeCommit(git, commits, commit, commits.indexOf(commit));
//						System.out.println("commitStability выполнился за " + (System.currentTimeMillis() - start) + " мс");
						start = System.currentTimeMillis();
						long commitSize = GitRepositoryUtil.processCommitSize(commit, git);
//						System.out.println("processCommitSize выполнился за " + (System.currentTimeMillis() - start) + " мс");
						start = System.currentTimeMillis();
						FileEntity fileMergedEntity = paths.values().stream().reduce(new FileEntity(0, 0, 0, 0, 0, 0, 0), (acc, fileEntity) -> {
							acc.add(fileEntity);
							return acc;
						});
//						System.out.println("FileEntity выполнился за " + (System.currentTimeMillis() - start) + " мс");
						dataBaseUtil.insertCommit(projectId, authorId, commit.getName(), commit.getCommitTime(), commitSize, commitStability, fileMergedEntity);
						paths.keySet().forEach(it -> fileList.add(new org.repodriller.scm.entities.FileEntity(projectId, it, commit.getName(), commit.getCommitTime())));
						dataBaseUtil.insertFile(fileList);
					} catch (Exception e) {
						System.err.println("Error processing commit " + commit.getName() + ": " + e.getMessage());
					}
				});
				futures.add(future);
			}

			// Дожидаемся завершения всех задач
			for (Future<?> f : futures) {
				f.get();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			executor.shutdown();
		}
	}

	public Map<String, DeveloperInfo> getDeveloperInfo(String nodePath) throws IOException, GitAPIException {
		ConcurrentHashMap<String, DeveloperInfo> developers = new ConcurrentHashMap<>();

		try (Git git = openRepository()) {
			long startTime = System.currentTimeMillis();

			nodePath = Objects.equals(nodePath, path) ? null : nodePath;
			String localPath = nodePath != null && nodePath.startsWith(path) ? nodePath.substring(path.length() + 1).replace("\\", "/") : nodePath;
			Iterable<RevCommit> commits = localPath != null ? git.log().addPath(localPath).call() : git.log().call();
			ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			List<Future<?>> futures = new ArrayList<>();

			System.out.println("pre processDeveloperInfo выполнился за " + (System.currentTimeMillis() - startTime) + " мс");

			startTime = System.currentTimeMillis();

			for (RevCommit commit : commits) {
				// Submitting tasks to the thread pool
				Future<?> future = executorService.submit(() -> processDeveloperInfo(commit, git, developers));
				futures.add(future);
			}

			for (Future<?> future : futures) {
				try {
					future.get();  // Catch exceptions if they occur during task execution
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();  // Logging or other error handling
				}
			}
			System.out.println("processDeveloperInfo выполнился за " + (System.currentTimeMillis() - startTime) + " мс");

			futures.clear();

			startTime = System.currentTimeMillis();

			String finalNodePath = nodePath;
			Stream<String> fileStream = files().stream().parallel().filter(it -> ((finalNodePath == null || it.getFile().getPath().startsWith(finalNodePath)) && !it.getFile().getPath().endsWith(".DS_Store")))
					.map(it -> it.getFile().getPath().substring(path.length() + 1).replace("\\", "/"));
			Stream<Pair<String, String>> fileHashes = fileStream.parallel().map(it -> new Pair<String, String>(it,dataBaseUtil.getLastFileHash(projectId, it))).filter(it -> dataBaseUtil.getBlameFileId(projectId, it.getFirst(), it.getSecond()) == null);
			Stream<Pair<String, Integer>> fileAndBlameHashes = fileHashes.parallel().map(it -> new Pair<String, Integer>(it.getFirst(), dataBaseUtil.insertBlameFile(projectId, it.getFirst(), it.getSecond())));
			Map<String, Integer> devs = dataBaseUtil.getDevelopersByProjectId(projectId);
			
			System.out.println("getDevelopersByProjectId выполнился за " + (System.currentTimeMillis() - startTime) + " мс");

			startTime = System.currentTimeMillis();
			for (Pair<String, Integer> filePair : fileAndBlameHashes.collect(Collectors.toSet())) {
				Future<?> future = executorService.submit(() -> {
                    BlameResult blameResult = null;
                    try {
						ObjectId commitId = ObjectId.fromString(Objects.requireNonNull(dataBaseUtil.getFirstHashForFile(projectId, filePair.getFirst())));
                        blameResult = git.blame().setFilePath(filePair.getFirst()).setStartCommit(commitId).call();
                    } catch (GitAPIException e) {
                        throw new RuntimeException(e);
                    }
                    if (blameResult != null) {
						GitRepositoryUtil.updateFileOwnerBasedOnBlame(blameResult, devs, dataBaseUtil, projectId, filePair.getSecond());
						dataBaseUtil.updateBlameFileSize(filePair.getSecond());
					}
                });
				futures.add(future);
			}
			for (Future<?> future : futures) {
				try {
					future.get();  // Catch exceptions if they occur during task execution
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();  // Logging or other error handling
				}
			}
			System.out.println("updateFileOwnerBasedOnBlame выполнился за " + (System.currentTimeMillis() - startTime) + " мс");
			dataBaseUtil.developerUpdateByBlameInfo(projectId, developers);
			executorService.shutdown();
		}
        return developers;
	}

	private void processDeveloperInfo(RevCommit commit, Git git, ConcurrentHashMap<String, DeveloperInfo> developers) {
		try {
			String email = commit.getAuthorIdent().getEmailAddress();
			DeveloperInfo dev = developers.computeIfAbsent(email, k -> new DeveloperInfo(commit.getAuthorIdent().getName(), email));
			dev.addCommit(commit);
			GitRepositoryUtil.analyzeCommit(commit, git, dev);
		} catch (IOException ignored) {}
	}

	public Integer getMaxNumberFilesInACommit() {
		return maxNumberFilesInACommit;
	}

	@Override
	public String getCommitFromTag(String tag) {

		try (Git git = openRepository()) {
			Repository repo = git.getRepository();

			Iterable<RevCommit> commits = git.log().add(getActualRefObjectId(repo.findRef(tag), repo)).call();
			git.close();
			for (RevCommit commit : commits) {
				return commit.getName().toString();
			}

			throw new RuntimeException("Failed for tag " + tag); // we never arrive here, hopefully

		} catch (Exception e) {
			throw new RuntimeException("Failed for tag " + tag, e);
		}
	}

	private ObjectId getActualRefObjectId(Ref ref, Repository repo) {
		final Ref repoPeeled = repo.peel(ref);
		if (repoPeeled.getPeeledObjectId() != null) {
			return repoPeeled.getPeeledObjectId();
		}
		return ref.getObjectId();
	}

	/**
	 * Return the max number of files in a commit.
	 * Default is hard-coded to "something large".
	 * Override with environment variable "git.maxfiles".
	 *
	 * @return Max number of files in a commit
	 */
	private int checkMaxNumberOfFiles() {
		try {
			return getSystemProperty("git.maxfiles");
		} catch (Exception e) {
			return DEFAULT_MAX_NUMBER_OF_FILES_IN_A_COMMIT;
		}
	}

	/**
	 * Return the max size of a diff in bytes.
	 * Default is hard-coded to "something large".
	 * Override with environment variable "git.maxdiff".
	 *
	 * @return Max diff size
	 */
	private int checkMaxSizeOfDiff() {
		try {
			return getSystemProperty("git.maxdiff");
		} catch (Exception e) {
			return MAX_SIZE_OF_A_DIFF;
		}
	}

	/**
	 * Get this system property (environment variable)'s value as an integer.
	 *
	 * @param name	Environment variable to retrieve
	 * @return	{@code name} successfully parsed as an int
	 * @throws NumberFormatException
	 */
	private int getSystemProperty(String name) throws NumberFormatException {
		String val = System.getProperty(name);
		return Integer.parseInt(val);
	}

	public void setRepoName(String repoName) {
		this.repoName = repoName;
	}

	public void setPath(String path) {
		this.path = PathUtils.fullPath(path);
	}

	public void setFirstParentOnly(boolean firstParentOnly) {
		this.firstParentOnly = firstParentOnly;
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
		this.collectConfig = config;
	}
	
	/**
	 * True if all filters accept, else false.
	 *
	 * @param diff	DiffEntry to evaluate
	 * @return allAccepted
	 */
	private boolean diffFiltersAccept(DiffEntry diff) {
		List<DiffFilter> diffFilters = this.collectConfig.getDiffFilters();
		for (DiffFilter diffFilter : diffFilters) {
			if (!diffFilter.accept(diff.getNewPath())) {
				return false;
			}
		}
		
		return true;
	}
}
