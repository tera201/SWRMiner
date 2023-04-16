package console;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.repodriller.RepositoryMining;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRemoteRepository;
import org.repodriller.scm.GitRepository;
import org.repodriller.scm.SCMRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.util.Comparator.comparingInt;

public class BuildModel {

  private static Logger log = LogManager.getLogger(GitRepository.class);

  public static void main(String[] args) {
    Model model = UMLFactory.eINSTANCE.createModel();
    model.setName("JUnit4");

    String projectRoot = new File(".").getAbsolutePath();

    String csvPath = projectRoot.replace(".", "csv-generated");
    String tempDir = projectRoot.replace(".", "clonnedGit");

    new File(csvPath).mkdirs();

    String gitUrl = "https://github.com/junit-team/junit4.git";

    System.out.println(getBranches(gitUrl));

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
            .inTempDir(System.getProperty("user.dir") + "/clonnedGit")
//          .asBareRepos()
            .buildAsSCMRepository();
  }

  public static String getBranches(String gitUrl) {
      SCMRepository repo = GitRemoteRepository
            .hostedOn(gitUrl)
            .inTempDir(System.getProperty("user.dir") + "/clonnedGit")
            .asBareRepos()
              .buildAsSCMRepository();
      String branches = repo.getScm().getAllBranches().toString();
      String tags = repo.getScm().getAllTags().toString();
      repo.getScm().delete();
      return tags + branches;
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
