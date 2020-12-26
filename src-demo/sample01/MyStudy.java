package sample01;

import org.repodriller.RepoDriller;
import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRepository;

import java.io.File;

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

    new RepositoryMining()
            .in(GitRepository.singleProject(gitPath))
            .through(Commits.all())
            .process(new DevelopersVisitor(), new CSVFile(cvePath + "/devs1.csv"))
            .mine();
  }
}