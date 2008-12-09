/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.maven.ide.eclipse.core.MavenLogger;


/**
 * This class creates an org.eclipse.ltk.core.refactoring.DocumentChange instance based on old and new text values
 * 
 * @author Anton Kraev
 */
public class ChangeCreator {
  private String label;

  private IDocument oldDocument;

  private IDocument newDocument;

  private IFile oldFile;

  public ChangeCreator(IFile oldFile, IDocument oldDocument, IDocument newDocument, String label) {
    this.newDocument = newDocument;
    this.oldDocument = oldDocument;
    this.oldFile = oldFile;
    this.label = label;
  }

  public TextFileChange createChange() throws Exception {
    TextFileChange change = new TextFileChange(label, oldFile);
    // change.setSaveMode(TextFileChange.FORCE_SAVE);
    change.setEdit(new MultiTextEdit());
    Object leftSide = new LineComparator(oldDocument);
    Object rightSide = new LineComparator(newDocument);

    RangeDifference[] differences = RangeDifferencer.findDifferences((IRangeComparator) leftSide, (IRangeComparator) rightSide);
    int insertOffset = 0;
    for(int i = 0; i < differences.length; i++ ) {
      RangeDifference curr = differences[i];
      // when comparing 2 files, only RangeDifference.CHANGE is possible, no need to test
      if (curr.rightLength() == curr.leftLength()) {
        // replace
        int startLine = curr.rightStart();
        int endLine = curr.rightEnd() - 1;
        for(int j = startLine; j <= endLine; j++ ) {
          int newPos = curr.leftStart() - startLine + j;
          String newText = newDocument.get(newDocument.getLineOffset(newPos), newDocument.getLineLength(newPos));
          change.addEdit(new ReplaceEdit(oldDocument.getLineOffset(j), oldDocument.getLineLength(j), newText));
        }
      } else if (curr.rightLength() > 0 && curr.leftLength() == 0) {
        // insert
        int startLine = curr.rightStart();
        int endLine = curr.rightEnd() - 1;
        int posInsert = oldDocument.getLineOffset(curr.leftStart());
        for(int j = startLine; j <= endLine; j++ ) {
          int newPos = curr.leftStart() - startLine + j + insertOffset;
          String newText = newDocument.get(newDocument.getLineOffset(newPos), newDocument.getLineLength(newPos));
          change.addEdit(new InsertEdit(posInsert, newText));
        }
        insertOffset += curr.rightEnd() - curr.rightStart();
      } else if (curr.leftLength() > 0 && curr.rightLength() == 0) {
        // delete
        int startLine = curr.leftStart();
        int endLine = curr.leftEnd() - 1;
        for(int j = startLine; j <= endLine; j++ ) {
          change.addEdit(new DeleteEdit(oldDocument.getLineOffset(j), oldDocument.getLineLength(j)));
        }
        insertOffset -= (curr.leftEnd() - curr.leftStart());
      } else {
        // unhandled
      }
    }
    return change;
  }
  
  public static class LineComparator implements IRangeComparator {
    private final IDocument document;
    private final ArrayList<Integer> hashes;

      /**
     * Create a line comparator for the given document.
     * 
     * @param document
     */
    public LineComparator(IDocument document) {
      this.document = document;
      this.hashes = new ArrayList<Integer>(Arrays.asList(new Integer[document.getNumberOfLines()]));
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#getRangeCount()
     */
    public int getRangeCount() {
      return document.getNumberOfLines();
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#rangesEqual(int, org.eclipse.compare.rangedifferencer.IRangeComparator, int)
     */
    public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
      try {
        return getHash(thisIndex).equals(((LineComparator) other).getHash(otherIndex));
      } catch (BadLocationException e) {
        MavenLogger.log("Problem comparing", e);
        return false;
      }
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#skipRangeComparison(int, int, org.eclipse.compare.rangedifferencer.IRangeComparator)
     */
    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
      return false;
    }

    /**
     * @param line the number of the line in the document to get the hash for
     * @return the hash of the line
     * @throws BadLocationException if the line number is invalid
     */
    private Integer getHash(int line) throws BadLocationException {
      Integer hash = hashes.get(line);
      if (hash == null) {
        IRegion lineRegion;
        lineRegion = document.getLineInformation(line);
        String lineContents= document.get(lineRegion.getOffset(), lineRegion.getLength());
        hash = new Integer(computeDJBHash(lineContents));
        hashes.set(line, hash);
      }
      return hash;
    }

    /**
     * Compute a hash using the DJB hash algorithm
     * 
     * @param string the string for which to compute a hash
     * @return the DJB hash value of the string
     */
    private int computeDJBHash(String string) {
      int hash = 5381;
      int len = string.length();
      for (int i = 0; i < len; i++) {
        hash = (hash << 5) + hash + string.charAt(i);
      }
      return hash;
    }
  }

}
