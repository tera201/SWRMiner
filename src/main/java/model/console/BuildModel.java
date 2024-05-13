package model.console;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.repodriller.RepositoryMining;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRemoteRepository;
import org.repodriller.scm.GitRepository;
import org.repodriller.scm.SCMRepository;
import org.repodriller.scm.SingleGitRemoteRepositoryBuilder;
import org.repodriller.scm.exceptions.CheckoutException;
import org.repodriller.util.DataBaseUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;

public class BuildModel {

  private static Logger log = LogManager.getLogger(GitRepository.class);

  public static void main(String[] args) throws CheckoutException, GitAPIException, IOException {

    String projectRoot = new File(".").getAbsolutePath();

    String csvPath = projectRoot.replace(".", "csv-generated");
    String tempDir = projectRoot.replace(".", "clonnedGit");

    new File(csvPath).mkdirs();

    String gitUrl = "https://github.com/arnohaase/a-foundation.git";

    BuildModel buildModel = new BuildModel();

//    FileUtils.deleteDirectory(new File(tempDir));

//    SCMRepository repo = GitRemoteRepository
//            .hostedOn(gitUrl)
//            .inTempDir(tempDir)
//            .getAsSCMRepository();
      SCMRepository repo = buildModel.getRepository(gitUrl, tempDir + "/a-foundation", tempDir + "/db");
//      SCMRepository repo = buildModel.createClone(gitUrl,tempDir + "/a-foundation", dataBaseUtil);
      repo.getScm().getDeveloperInfo();

//    SCMRepository repo = GitRemoteRepository
//            .hostedOn(gitUrl)
//            .inTempDir(tempDir)
//            .buildAsSCMRepository();


//    System.out.println(repo.getPath());
//    System.out.println(repo.getRepoName());

//    List<String> branches = buildModel.getBranches(repo);
//    List<String> tags = buildModel.getTags(repo);
    //different checkout, work only after buildModel.checkout
//    repo.getScm().checkoutTo(tags.get(8));
//    System.out.println(repo.getScm().getCurrentBranchOrTagName());
//    repo.getScm().checkoutTo(branches.get(0));
//    System.out.println(repo.getScm().getCurrentBranchOrTagName());


    //work for tags
//    buildModel.checkout(repo, branches.get(0));

//    System.out.println(branches);
//    System.out.println(tags);
//    System.out.println(buildModel.getTags(repo));
//    buildModel.cleanData();
//    repo.getScm().delete();

//    SCMRepository remoteGitRepo = GitRemoteRepository
//            .hostedOn(gitUrl)
//            .inTempDir(tempDir)
//          .asBareRepos()
//            .buildAsSCMRepository();

    //
    // Проход по репозиторию для сбора информации:
    // какой автор сколько сделал изменений в программе.
    //
//    collectAuthorChanges(csvPath, remoteGitRepo);

    //
    // Проход по репозиторию для сбора информации:
    // какой автор какие файлы создавал.
    //
//    FileCreatorVisitor fileCreateVisitor = new FileCreatorVisitor();
//    new RepositoryMining()
//            .in(remoteGitRepo)
////            .setRepoTmpDir(new File(tempDir).toPath())
//            .visitorsAreThreadSafe(true)
//            .withThreads(6)
//            .through(Commits.all())
//            .visitorsChangeRepoState(false)
//            .process(fileCreateVisitor,
//                    new CSVFile(csvPath + "/junit4-author-files-created.csv"))
//            .mine();

    CSVFile csvByName = new CSVFile(
            csvPath + "/junit4-author-files-created-count.csv",
            new String[]{"Developer", "create-files"});
//    fileCreateVisitor.fileCreatorMap.forEach(
//            (developer, files) -> csvByName.write(developer, files.size())
//    );

//    fileCreateVisitor.fileCreatorMap.entrySet()
//            .stream()
//            .sorted(comparingInt(entry -> entry.getValue().size()))
//            .forEach(e -> csvByName.write(e.getKey(), e.getValue().size()));
  }
  public SCMRepository createClone(String gitUrl) {

    return GitRemoteRepository
            .hostedOn(gitUrl)
            .buildAsSCMRepository();
  }
  public SCMRepository createClone(String gitUrl, String path, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository.db");
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .inTempDir(path)
            .dateBase(dataBaseUtil)
            .buildAsSCMRepository();
  }
  public SCMRepository createClone(String gitUrl, String path, String username, String password, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository.db");
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .inTempDir(path)
            .creds(username, password)
            .dateBase(dataBaseUtil)
            .buildAsSCMRepository();
  }

  public SCMRepository getRepository(String gitUrl, String path, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository.db");
    return GitRemoteRepository
            .hostedOn(gitUrl)
            .inTempDir(path)
            .dateBase(dataBaseUtil)
            .getAsSCMRepository();
  }

  public SCMRepository getRepository(String projectPath, String dataBaseDirPath) {
    DataBaseUtil dataBaseUtil = new DataBaseUtil(dataBaseDirPath + "/repository.db");
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

    public void collectAuthorChanges(SCMRepository remoteGitRepo) {
    String csvPath = System.getProperty("user.dir") + "/analyseGit";
    FileCreatorVisitor fileCreateVisitor = new FileCreatorVisitor();
    new RepositoryMining()
            .in(remoteGitRepo)
            .visitorsAreThreadSafe(true)
            .withThreads(6)
            .through(Commits.all())
            .visitorsChangeRepoState(false)
            .process(fileCreateVisitor,
                    new CSVFile(csvPath + "/author-files-created.csv"))
            .mine();

    CSVFile csvByName = new CSVFile(
            csvPath + "/author-files-created-count.csv",
            new String[]{"Developer", "create-files"});

    fileCreateVisitor.fileCreatorMap.entrySet()
            .stream()
            .sorted(comparingInt(entry -> entry.getValue().size()))
            .forEach(e -> csvByName.write(e.getKey(), e.getValue().size()));
  }

  public void cleanData() {
    String csvPath = System.getProperty("user.dir") + "/analyseGit";
    String gitPath = System.getProperty("user.dir") + "/clonnedGit";
    try {
      FileUtils.deleteDirectory(new File(csvPath));
      FileUtils.deleteDirectory(new File(gitPath));
    } catch (IOException e) {
      log.info("Delete failed: " + e);
    }
  }

  public void removeRepo() {
    String csvPath = System.getProperty("user.dir") + "/analyseGit";
    String gitPath = System.getProperty("user.dir") + "/clonnedGit";
    try {
      FileUtils.deleteDirectory(new File(csvPath));
      FileUtils.deleteDirectory(new File(gitPath));
    } catch (IOException e) {
      log.info("Delete failed: " + e);
    }
  }

  private static void collectAuthorChanges(String csvPath, SCMRepository remoteGitRepo) {
    ModificationsVisitor visitor = new ModificationsVisitor();

    new RepositoryMining()
            .in(remoteGitRepo)
            .through(Commits.all())
            .process(visitor)
            .mine();

    // Записываем в файл список пар: <автор, количество-изменений>
    // отсортированный по фамилиям авторов.
    CSVFile csvByName = new CSVFile(
            csvPath + "/junit4-authors-by-name.csv",
            new String[]{"developer", "changes"});
    visitor.devs.forEach((developer, count) -> csvByName.write(developer, count));

    // Записываем в файл список пар: <автор, количество-изменений>
    // отсортированный по фамилиям авторов.
    CSVFile csvByChanges = new CSVFile(
            csvPath + "/junit4-by-changes.csv",
            new String[]{"developer", "changes"});
    visitor.devs.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> csvByChanges.write(e.getKey(), e.getValue()));
  }
}
