package sample05;

import org.repodriller.RepoDriller;
import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.commit.OnlyInBranches;
import org.repodriller.filter.commit.OnlyInMainBranch;
import org.repodriller.filter.commit.OnlyModificationsWithFileTypes;
import org.repodriller.filter.commit.OnlyNoMerge;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRemoteRepository;
import org.repodriller.scm.SCMRepository;

import java.io.File;

import static java.lang.System.out;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Managing State in the Visitor
 */
public class MyStudy implements Study {

  public static void main(String[] args) {
    new RepoDriller().start(new MyStudy());
  }

  @Override
  public void execute() {
    String projectRoot = new File(".").getAbsolutePath();

    String gitPath = "/__git-jetbrain/repodriller";
    String cvePath = projectRoot.replace(".", "cvs-generated");

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


    ModificationsVisitor visitor = new ModificationsVisitor();

    new RepositoryMining()
            .in(remoteGitRepo)
            .through(Commits.all())
            .filters(
                    new OnlyModificationsWithFileTypes(asList(".java", ".xml")),
                    new OnlyInBranches(singletonList("master")),
                    new OnlyNoMerge(),
                    new OnlyInMainBranch()
            )
            .process(visitor, new CSVFile(cvePath + "/devs5.csv"))
            .mine();


    visitor.devs.forEach((developer, count) ->
            out.format("developer: %s changes: %s %n", developer, count)
    );
  }
}
