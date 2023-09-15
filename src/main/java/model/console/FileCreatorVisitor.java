package model.console;

import org.repodriller.domain.Commit;
import org.repodriller.domain.Modification;
import org.repodriller.domain.ModificationType;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.CommitVisitor;
import org.repodriller.scm.SCMRepository;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class FileCreatorVisitor implements CommitVisitor {
  public Map<String, ArrayList<String>> fileCreatorMap = new TreeMap<>();

  @Override
  public void process(SCMRepository repo, Commit commit, PersistenceMechanism writer) {
    String authorName = commit.getAuthor().getName();

    for (Modification m : commit.getModifications()) {
      ModificationType mod = m.getType();
      String fileName = m.getFileName();

      if (!m.fileNameEndsWith(".java"))
        continue;

      if (fileName.contains("/test/"))
        continue;

      if (mod != ModificationType.ADD)
        continue;

      if (!fileCreatorMap.containsKey(authorName))
        fileCreatorMap.put(authorName, new ArrayList<>());

      fileCreatorMap.get(authorName).add(fileName);

      writer.write(
              commit.getBranches(),
              authorName,
              fileName,
              mod,
              commit.getDate().getTime()
      );
    }
  }
}
