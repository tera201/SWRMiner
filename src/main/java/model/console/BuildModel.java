package model.console;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.repodriller.scm.GitRemoteRepository;
import org.repodriller.scm.GitRepository;
import org.repodriller.scm.SCMRepository;
import org.repodriller.scm.SingleGitRemoteRepositoryBuilder;
import org.repodriller.scm.exceptions.CheckoutException;
import org.repodriller.util.DataBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class BuildModel {

  private static Logger log = LoggerFactory.getLogger(GitRepository.class);

  public static void main(String[] args) throws GitAPIException, IOException {

    String projectRoot = new File(".").getAbsolutePath();
    String csvPath = projectRoot.replace(".", "csv-generated");
    String tempDir = projectRoot.replace(".", "clonedGit");

    new File(csvPath).mkdirs();

    String gitUrl = "https://github.com/arnohaase/a-foundation.git";

    BuildModel buildModel = new BuildModel();

    SCMRepository repo = buildModel.getRepository(gitUrl, tempDir + "/a-foundation", tempDir + "/db");

    long startTime = System.currentTimeMillis();
    repo.getScm().dbPrepared();
    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;
    System.out.println("dbPrepared executed in " + executionTime + " ms");

    repo.getScm().getDeveloperInfo();
  }

  public SCMRepository createClone(String gitUrl) {
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .buildAsSCMRepository();
  }

  public SCMRepository createClone(String gitUrl, String path, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository");
    dataBaseUtil.create();
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .inTempDir(path)
            .dateBase(dataBaseUtil)
            .buildAsSCMRepository();
  }

  public SCMRepository createClone(String gitUrl, String path, String username, String password, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository");
    dataBaseUtil.create();
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .inTempDir(path)
            .creds(username, password)
            .dateBase(dataBaseUtil)
            .buildAsSCMRepository();
  }

  public SCMRepository getRepository(String gitUrl, String path, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository");
    dataBaseUtil.create();
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .inTempDir(path)
            .dateBase(dataBaseUtil)
            .getAsSCMRepository();
  }

  public SCMRepository getRepository(String projectPath, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository");
    dataBaseUtil.create();
    return new SingleGitRemoteRepositoryBuilder()
            .inTempDir(projectPath)
            .dateBase(dataBaseUtil)
            .getAsSCMRepository();
  }

  public String getRepoNameByUrl(String gitUrl) {
    return GitRemoteRepository.repoNameFromURI(gitUrl);
  }

  public GitRemoteRepository createRepo(String gitUrl) throws GitAPIException {
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .build();
  }

  public List<String> getBranches(SCMRepository repo) {
    return repo.getScm().getAllBranches().stream().map(Ref::getName).collect(Collectors.toList());
  }

  public List<String> getTags(SCMRepository repo) {
    return repo.getScm().getAllTags().stream().map(Ref::getName).collect(Collectors.toList());
  }

  public void checkout(SCMRepository repo, String branch) throws CheckoutException {
    repo.getScm().checkoutTo(branch);
  }

  public void cleanData() {
    String csvPath = System.getProperty("user.dir") + "/analyseGit";
    String gitPath = System.getProperty("user.dir") + "/clonedGit";
    try {
      FileUtils.deleteDirectory(new File(csvPath));
      FileUtils.deleteDirectory(new File(gitPath));
    } catch (IOException e) {
      log.info("Delete failed: " + e);
    }
  }

  public void removeRepo() {
    String csvPath = System.getProperty("user.dir") + "/analyseGit";
    String gitPath = System.getProperty("user.dir") + "/clonedGit";
    try {
      FileUtils.deleteDirectory(new File(csvPath));
      FileUtils.deleteDirectory(new File(gitPath));
    } catch (IOException e) {
      log.info("Delete failed: " + e);
    }
  }
}