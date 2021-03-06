
package org.eclipse.m2e.tests.util.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.SearchExpression;
import org.eclipse.m2e.core.ui.internal.search.util.IndexSearchEngine;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class IndexSearchEngineTest extends AbstractMavenProjectTestCase {
  @Test
  public void testGroupIDProposal() throws Exception {
    IndexSearchEngine engine = new IndexSearchEngine(new TestIndex());
    Collection<String> results = engine.findGroupIds("group", Packaging.ALL, null);
    assertTrue(results.contains("group"));
    assertTrue(results.contains("groupSomething"));
    assertFalse(results.contains("grouoo"));
  }

  private static class TestIndex implements IIndex {
    String[] entries = new String[] {"group", "groupSomething", "grouoo"};

    public TestIndex() {
    }

    @Override
    public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) {
      return null;
    }

    @Override
    public IndexedArtifactFile identify(File file) {
      return null;
    }

    @Override
    public Collection<IndexedArtifact> find(SearchExpression groupId, SearchExpression artifactId,
        SearchExpression version, SearchExpression packaging) {
      Set<IndexedArtifact> results = new HashSet<>();
      for(String entry : entries) {
        if(entry.startsWith(groupId.getStringValue())) {
          IndexedArtifact artifact = new IndexedArtifact(entry, null, null, null, null);
          results.add(artifact);
        }
      }
      return results;
    }

    @Override
    public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType) {
      return null;
    }

    @Override
    public Map<String, IndexedArtifact> search(SearchExpression expression, String searchType, int classifier) {
      return null;
    }

    @Override
    public Collection<IndexedArtifact> find(Collection<SearchExpression> groupId,
        Collection<SearchExpression> artifactId, Collection<SearchExpression> version,
        Collection<SearchExpression> packaging) {
      return null;
    }

  }
}
