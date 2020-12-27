package sample05;

import org.repodriller.domain.Commit;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.CommitVisitor;
import org.repodriller.scm.SCMRepository;

import java.util.HashMap;
import java.util.Map;

public class ModificationsVisitor implements CommitVisitor {
  public Map<String, Integer> devs;

  public ModificationsVisitor() {
    this.devs = new HashMap<>();
  }

  @Override
  public void process(SCMRepository repo, Commit commit, PersistenceMechanism writer) {
    String dev = commit.getCommitter().getName();
    if (!devs.containsKey(dev)) devs.put(dev, 0);

    int currentFiles = devs.get(dev);
    devs.put(dev, currentFiles + commit.getModifications().size());
  }
}

