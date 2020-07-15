package org.voyanttools.trombone.model;

/**
 * Copied from org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo
 * Its constructor was made private in 7+
 */
public final class TermInfo implements Comparable<TermInfo>{

    private final String text;
    private final int startOffset;
    private final int endOffset;
    private final int position;    

    // IDF-weight of this term
    private final float weight;
    
    // pointer to other TermInfo's at the same position.
    // this is a circular list, so with no syns, just points to itself
    private TermInfo next;

    public TermInfo(String text, int startOffset, int endOffset, int position, float weight){
      this.text = text;
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.position = position;
      this.weight = weight;
      this.next = this;
    }
    
    void setNext(TermInfo next) { this.next = next; }
    /** 
     * Returns the next TermInfo at this same position.
     * This is a circular list!
     */
    public TermInfo getNext() { return next; }
    public String getText(){ return text; }
    public int getStartOffset(){ return startOffset; }
    public int getEndOffset(){ return endOffset; }
    public int getPosition(){ return position; }
    public float getWeight(){ return weight; }
    
    @Override
    public String toString(){
      return text + '(' + startOffset + ',' + endOffset + ',' + position + ')';
    }

    @Override
    public int compareTo( TermInfo o ){
      return ( this.position - o.position );
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + position;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      TermInfo other = (TermInfo) obj;
      return position == other.position;
    }
  }