package verify.entity.pattern;

import verify.entity.PatternType;
import verify.entity.ReadWriteNode;

public class MemoryAccessPair extends Pattern {
    public MemoryAccessPair(PatternType patternType) {
        super(patternType);
        if (patternType.getNodes().length != 2) {
            throw new RuntimeException("not a memory access pair");
        }
    }

    public MemoryAccessPair(PatternType patternType, ReadWriteNode[] nodes) {
        super(patternType, nodes);
        if (patternType.getNodes().length != 2) {
            throw new RuntimeException("not a memory access pair");
        }
    }

    public ReadWriteNode getFirst() {
        if (isMatched()) {
            return this.nodes[0];
        }
        else {
            return null;
        }
    }

    public ReadWriteNode getLast() {
        if (isMatched()) {
            return this.nodes[1];
        }
        else {
            return null;
        }
    }
}
