package sample06;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.repodriller.domain.Commit;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.CommitVisitor;
import org.repodriller.scm.RepositoryFile;
import org.repodriller.scm.SCMRepository;

public class JavaParserVisitor implements CommitVisitor {

  @Override
  public void process(SCMRepository repo, Commit commit, PersistenceMechanism writer) {

    try {
      repo.getScm().checkout(commit.getHash());

      List<RepositoryFile> files = repo.getScm().files();

      for(RepositoryFile file : files) {
        if(!file.fileNameEndsWith("java")) continue;

        File soFile = file.getFile();

//        NumberOfMethodsVisitor visitor = new NumberOfMethodsVisitor();
//        new JDTRunner().visit(visitor, new ByteArrayInputStream(readFile(soFile).getBytes()));
//
//        int methods = visitor.getQty();

        writer.write(
                commit.getHash(),
                file.getFullName()
//                methods
        );

      }

    } finally {
      repo.getScm().reset();
    }
  }

  private String readFile(File f) {
    try {
      FileInputStream input = new FileInputStream(f);
      String text = IOUtils.toString(input, Charset.defaultCharset());
      input.close();
      return text;
    } catch (Exception e) {
      throw new RuntimeException("error reading file " + f.getAbsolutePath(), e);
    }
  }

}

