package sample02;

import org.repodriller.RepoDriller;
import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRemoteRepository;
import org.repodriller.scm.SCMRepository;

import java.io.File;

/**
 * Конфигурирование удаленного Git-репозитория.
 */
public class MyStudy implements Study {

  public static void main(String[] args) {
    new RepoDriller().start(new MyStudy());
  }

  @Override
  public void execute() {
    String projectRoot = new File(".").getAbsolutePath();

    String gitPath = "/__git-jetbrain/repodriller";
    String cvePath = projectRoot.replace(".", "csv-generated");

    new File(cvePath).mkdirs();

//    You can clone as a bare repository,
//    if your study will work only with repository metadata
//    (commit history info, modifications, etc.)
//    and won't need to checkout/reset files.
    String gitUrl = "https://github.com/mauricioaniche/repodriller.git";

    SCMRepository remoteGitRepo = GitRemoteRepository
            .hostedOn(gitUrl)
//          .inTempDir(tempDir)
//          .asBareRepos()
            .buildAsSCMRepository();

    new RepositoryMining()
            .in(remoteGitRepo)
            .through(Commits.all())
            .process(new DevelopersVisitor(), new CSVFile(cvePath + "/devs2.csv"))
            .mine();
  }
}