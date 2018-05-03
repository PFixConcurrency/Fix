package verify.entity.pattern;

import verify.entity.Node;
import verify.entity.PatternType;
import verify.entity.ReadWriteNode;

import java.util.ArrayList;
import java.util.List;

public class Pattern {
	protected PatternType patternType;
    protected ReadWriteNode[] nodes;
    protected static String patternSet = "falcon";

    private static PatternType rw = new PatternType("R1(x), W2(x)");
    private static PatternType wr = new PatternType("W1(x), R2(x)");
    private static PatternType ww = new PatternType("W1(x), W2(x)");
	
	public Pattern(PatternType patternType) {
		this.patternType = patternType;
		this.nodes = null;
	}
	
	public Pattern(PatternType patternType, ReadWriteNode[] nodes) {
		this.patternType = patternType;
		this.nodes =nodes;
	}
	
	public PatternType getPatternType() {
		return this.patternType;
	}
	
	public ReadWriteNode[] getNodes() {
		return this.nodes;
	}
	
	public void setNodes(ReadWriteNode[] nodes) {
		this.nodes = nodes;
	}

	public static String getPatternSet() {
	    return patternSet;
    }

    public static void setPatternSet(String patternSet) {
	    Pattern.patternSet = patternSet;
    }
	
	public boolean isMatched() {
		return nodes != null;
	}
	
	public boolean isSamePattern(Pattern pattern) {
		if (!(this.isMatched() && pattern.isMatched())) {
			throw new RuntimeException("Two patterns should be both matched.");
		}
		
		if (!this.isSamePatternType(pattern)) {
			return false;
		}
		
		ReadWriteNode[] nodes = pattern.getNodes();
		for (int i = 0; i < this.nodes.length; ++i) {
			if (!this.nodes[i].isSame(nodes[i])) {
				return false;
			}
		}
		return true;
	}

	public boolean isSameExecptThread(Pattern pattern) {
        if (!(this.isMatched() && pattern.isMatched())) {
            throw new RuntimeException("Two patterns should be both matched.");
        }

        if (!this.isSamePatternType(pattern)) {
            return false;
        }

        ReadWriteNode[] nodes = pattern.getNodes();
        for (int i = 0; i < this.nodes.length; ++i) {
            ReadWriteNode node1 = this.nodes[i];
            ReadWriteNode node2 = nodes[i];
            if (!node1.isSameExceptThread(node2)) {
                return false;
            }
        }
        return true;
    }
	
	public boolean isSamePatternType(Pattern pattern) {
		return this.patternType.isSame(pattern.getPatternType());
	}

	public boolean isSameInstance(Pattern pattern) {
	    return this.nodes[0].isSameInstance(pattern.getNodes()[0]);
    }

	public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Pattern {\n");
        stringBuilder.append("\t" + this.patternType.toString() + "\n");
        stringBuilder.append("\tmatched: " + this.isMatched() + "\n");
        if (this.isMatched()) {
            for (Node node : this.nodes) {
                stringBuilder.append("\t" + node.toString() + "\n");
            }
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public static MemoryAccessPair[] getMemoryAccessPairs() {
        List<MemoryAccessPair> pairs = new ArrayList<>();

        //P1 R1(x), W2(x)
        pairs.add(new MemoryAccessPair(new PatternType("R1(x), W2(x)")));

        //P2 W1(x), R2(x)
        pairs.add(new MemoryAccessPair(new PatternType("W1(x), R2(x)")));

        //P3 W1(x), W2(x)
        pairs.add(new MemoryAccessPair(new PatternType("W1(x), W2(x)")));

        return pairs.toArray(new MemoryAccessPair[pairs.size()]);
    }

    public static Pattern tryConstructFalconPattern(MemoryAccessPair pair1, MemoryAccessPair pair2) {
	    if (pair1.getLast().getId() == pair2.getFirst().getId()
                && pair1.getFirst().getThread().equals(pair2.getLast().getThread())) {
	        String ptStr;
	        if (pair1.getFirst().getType().equals("READ")) {
	            if (pair2.getLast().getType().equals("READ")) {
                    //P4 R1(x), W2(x), R1(x)
	                ptStr = "R1(x), W2(x), R1(x)";
                }
                else {
                    //P8 W1(x), W2(x), W1(x)
	                ptStr = "W1(x), W2(x), W1(x)";
                }
            }
            else {
	            if (pair1.getLast().getType().equals("READ")) {
                    //P6 W1(x), R2(x), W1(x)
	                ptStr = "W1(x), R2(x), W1(x)";
                }
                else {
	                if (pair2.getLast().getType().equals("READ")) {
                        //P5 W1(x), W2(x), R1(x)
	                    ptStr = "W1(x), W2(x), R1(x)";
                    }
                    else {
                        //P7 R1(x), W2(x), W1(x)
	                    ptStr = "R1(x), W2(x), W1(x)";
                    }
                }
            }
	        return new Pattern(new PatternType(ptStr), new ReadWriteNode[]{
                    pair1.getFirst(),
                    pair1.getLast(),
                    pair2.getLast()
            });
        }
        else if (pair2.getLast().getId() == pair1.getFirst().getId()
                && pair2.getFirst().getThread().equals(pair1.getLast().getThread())) {
            String ptStr;
            if (pair2.getFirst().getType().equals("READ")) {
                if (pair1.getLast().getType().equals("READ")) {
                    //P4 R1(x), W2(x), R1(x)
                    ptStr = "R1(x), W2(x), R1(x)";
                }
                else {
                    //P8 W1(x), W2(x), W1(x)
                    ptStr = "W1(x), W2(x), W1(x)";
                }
            }
            else {
                if (pair2.getLast().getType().equals("READ")) {
                    //P6 W1(x), R2(x), W1(x)
                    ptStr = "W1(x), R2(x), W1(x)";
                }
                else {
                    if (pair1.getLast().getType().equals("READ")) {
                        //P5 W1(x), W2(x), R1(x)
                        ptStr = "W1(x), W2(x), R1(x)";
                    }
                    else {
                        //P7 R1(x), W2(x), W1(x)
                        ptStr = "R1(x), W2(x), W1(x)";
                    }
                }
            }
            return new Pattern(new PatternType(ptStr), new ReadWriteNode[]{
                    pair2.getFirst(),
                    pair2.getLast(),
                    pair1.getLast()
            });
        }
        else {
            return null;
        }
    }

    public static Pattern tryConstrucUnicornPattern(MemoryAccessPair pair1, MemoryAccessPair pair2) {
	    Pattern pattern = tryConstructFalconPattern(pair1, pair2);
	    if (pattern != null) {
	        return pattern;
        }
	    //检查pair1, pair2是否属于同一个实例却是同样两个线程的交互操作,切线程顺序相反
	    if (!pair1.isSameInstance(pair2)
                && pair1.getFirst().getThread().equals(pair2.getLast().getThread())
                && pair1.getLast().getThread().equals(pair2.getFirst().getThread())) {

            //P9 W1(x), W2(x), W2(y), W1(y)
            if (pair1.getPatternType().isSame(ww) && pair2.getPatternType().isSame(ww)) {
                if (pair1.getLast().getId() < pair2.getFirst().getId()) {
                    return new Pattern(new PatternType("W1(x), W2(x), W2(y), W1(y)"), new ReadWriteNode[] {
                            pair1.getFirst(),
                            pair1.getLast(),
                            pair2.getFirst(),
                            pair2.getLast()
                    });
                }
                else if (pair2.getLast().getId() < pair1.getFirst().getId()) {
                    return new Pattern(new PatternType("W1(x), W2(x), W2(y), W1(y)"), new ReadWriteNode[] {
                            pair2.getFirst(),
                            pair2.getLast(),
                            pair1.getFirst(),
                            pair1.getLast()
                    });
                }
            }

            //P10 W1(x), W2(y), W2(x), W1(y)
            if (pair1.getPatternType().isSame(ww) && pair2.getPatternType().isSame(ww)) {
                if (pair1.getFirst().getId() < pair2.getFirst().getId()
                        && pair2.getFirst().getId() < pair1.getLast().getId()
                        && pair1.getLast().getId() < pair2.getLast().getId()) {
                    return new Pattern(new PatternType("W1(x), W2(y), W2(x), W1(y)"), new ReadWriteNode[] {
                            pair1.getFirst(),
                            pair2.getFirst(),
                            pair1.getLast(),
                            pair2.getLast()
                    });
                }
                else if (pair2.getFirst().getId() < pair1.getFirst().getId()
                        && pair1.getFirst().getId() < pair2.getLast().getId()
                        && pair2.getLast().getId() < pair1.getLast().getId()) {
                    return new Pattern(new PatternType("W1(x), W2(y), W2(x), W1(y)"), new ReadWriteNode[] {
                            pair2.getFirst(),
                            pair1.getFirst(),
                            pair2.getLast(),
                            pair1.getLast()
                    });
                }
            }

            //P11 W1(x), W2(y), W1(y), W2(x)
            if (pair1.getPatternType().isSame(ww) && pair2.getPatternType().isSame(ww)) {
                if (pair1.getFirst().getId() < pair2.getFirst().getId()
                        && pair2.getLast().getId() < pair1.getLast().getId()) {
                    return new Pattern(new PatternType("W1(x), W2(y), W1(y), W2(x)"), new ReadWriteNode[] {
                            pair1.getFirst(),
                            pair2.getFirst(),
                            pair2.getLast(),
                            pair1.getLast()
                    });
                }
                else if (pair2.getFirst().getId() < pair1.getFirst().getId()
                        && pair1.getLast().getId() < pair2.getLast().getId()) {
                    return new Pattern(new PatternType("W1(x), W2(y), W1(y), W2(x)"), new ReadWriteNode[] {
                            pair2.getFirst(),
                            pair1.getFirst(),
                            pair1.getLast(),
                            pair2.getLast()
                    });
                }
            }

            //P12 W1(x), R2(x), R2(y), W1(y) pair1 < pair2
            if (pair1.getPatternType().isSame(wr) && pair2.getPatternType().isSame(rw)
                    && pair1.getLast().getId() < pair2.getFirst().getId()) {
                return new Pattern(new PatternType("W1(x), R2(x), R2(y), W1(y)"), new ReadWriteNode[] {
                        pair1.getFirst(),
                        pair1.getLast(),
                        pair2.getFirst(),
                        pair2.getLast()
                });
            }

            //P12 W1(x), R2(x), R2(y), W1(y) pair2 < pair1
            if (pair2.getPatternType().isSame(wr) && pair1.getPatternType().isSame(rw)
                    && pair2.getLast().getId() < pair1.getFirst().getId()) {
                return new Pattern(new PatternType("W1(x), R2(x), R2(y), W1(y)"), new ReadWriteNode[] {
                        pair2.getFirst(),
                        pair2.getLast(),
                        pair1.getFirst(),
                        pair1.getLast()
                });
            }

            //P13 W1(x), R2(y), R2(x), W1(y) pair1 < pair2
            if (pair1.getPatternType().isSame(wr) && pair2.getPatternType().isSame(rw)
                    && pair1.getFirst().getId() < pair2.getFirst().getId()
                    && pair2.getFirst().getId() < pair1.getLast().getId()
                    && pair1.getLast().getId() < pair2.getLast().getId()) {
                return new Pattern(new PatternType("W1(x), R2(y), R2(x), W1(y)"), new ReadWriteNode[] {
                        pair1.getFirst(),
                        pair2.getFirst(),
                        pair1.getLast(),
                        pair2.getLast()
                });
            }

            //P13 W1(x), R2(y), R2(x), W1(y) pair2 < pair1
            if (pair2.getPatternType().isSame(wr) && pair1.getPatternType().isSame(rw)
                    && pair2.getFirst().getId() < pair1.getFirst().getId()
                    && pair1.getFirst().getId() < pair2.getLast().getId()
                    && pair2.getLast().getId() < pair1.getLast().getId()) {
                return new Pattern(new PatternType("W1(x), R2(y), R2(x), W1(y)"), new ReadWriteNode[] {
                        pair2.getFirst(),
                        pair1.getFirst(),
                        pair2.getLast(),
                        pair1.getLast()
                });
            }

            //P14 R1(x), W2(x), W2(y), R1(y) pair1 < pair2
            if (pair1.getPatternType().isSame(rw) && pair2.getPatternType().isSame(wr)
                    && pair1.getLast().getId() < pair2.getFirst().getId()) {
                return new Pattern(new PatternType("R1(x), W2(x), W2(y), R1(y)"), new ReadWriteNode[] {
                        pair1.getFirst(),
                        pair1.getLast(),
                        pair2.getFirst(),
                        pair2.getLast()
                });
            }

            //P14 R1(x), W2(x), W2(y), R1(y) pair2 < pair1
            if (pair2.getPatternType().isSame(rw) && pair1.getPatternType().isSame(wr)
                    && pair2.getLast().getId() < pair1.getFirst().getId()) {
                return new Pattern(new PatternType("R1(x), W2(x), W2(y), R1(y)"), new ReadWriteNode[] {
                        pair2.getFirst(),
                        pair2.getLast(),
                        pair1.getFirst(),
                        pair1.getLast()
                });
            }

            //P15 R1(x), W2(y), W2(x), R1(y) pair1 < pair2
            if (pair1.getPatternType().isSame(rw) && pair2.getPatternType().isSame(wr)
                    && pair1.getFirst().getId() < pair2.getFirst().getId()
                    && pair2.getFirst().getId() < pair1.getLast().getId()
                    && pair1.getLast().getId() < pair2.getLast().getId()) {
                return new Pattern(new PatternType("R1(x), W2(y), W2(x), R1(y)"), new ReadWriteNode[] {
                        pair1.getFirst(),
                        pair2.getFirst(),
                        pair1.getLast(),
                        pair2.getLast()
                });
            }

            //P15 R1(x), W2(y), W2(x), R1(y) pair2 < pair1
            if (pair2.getPatternType().isSame(rw) && pair1.getPatternType().isSame(wr)
                    && pair2.getFirst().getId() < pair1.getFirst().getId()
                    && pair1.getFirst().getId() < pair2.getLast().getId()
                    && pair2.getLast().getId() < pair1.getLast().getId()) {
                return new Pattern(new PatternType("R1(x), W2(y), W2(x), R1(y)"), new ReadWriteNode[] {
                        pair2.getFirst(),
                        pair1.getFirst(),
                        pair2.getLast(),
                        pair1.getLast()
                });
            }

            //P16 R1(x), W2(y), R1(y), W2(x) pair1 < pair2
            if (pair1.getPatternType().isSame(rw) && pair2.getPatternType().isSame(wr)
                    && pair1.getFirst().getId() < pair2.getFirst().getId()
                    && pair2.getLast().getId() < pair1.getLast().getId()) {
                return new Pattern(new PatternType("R1(x), W2(y), R1(y), W2(x)"), new ReadWriteNode[] {
                        pair1.getFirst(),
                        pair2.getFirst(),
                        pair2.getLast(),
                        pair1.getLast()
                });
            }

            //P16 R1(x), W2(y), R1(y), W2(x) pair2 < pair1
            if (pair2.getPatternType().isSame(rw) && pair1.getPatternType().isSame(wr)
                    && pair2.getFirst().getId() < pair1.getFirst().getId()
                    && pair1.getLast().getId() < pair2.getLast().getId()) {
                return new Pattern(new PatternType("R1(x), W2(y), R1(y), W2(x)"), new ReadWriteNode[] {
                        pair2.getFirst(),
                        pair1.getFirst(),
                        pair1.getLast(),
                        pair2.getLast()
                });
            }

            //P17 W1(x), R2(y), W1(y), R2(x) pair1 < pair2
            if (pair1.getPatternType().isSame(wr) && pair2.getPatternType().isSame(rw)
                    && pair1.getFirst().getId() < pair2.getFirst().getId()
                    && pair2.getLast().getId() < pair1.getLast().getId()) {
                return new Pattern(new PatternType("W1(x), R2(y), W1(y), R2(x)"), new ReadWriteNode[] {
                        pair1.getFirst(),
                        pair2.getFirst(),
                        pair2.getLast(),
                        pair1.getLast()
                });
            }

            //P17 W1(x), R2(y), W1(y), R2(x) pair2 < pair1
            if (pair2.getPatternType().isSame(wr) && pair1.getPatternType().isSame(rw)
                    && pair2.getFirst().getId() < pair1.getFirst().getId()
                    && pair1.getLast().getId() < pair2.getLast().getId()) {
                return new Pattern(new PatternType("W1(x), R2(y), W1(y), R2(x)"), new ReadWriteNode[] {
                        pair2.getFirst(),
                        pair1.getFirst(),
                        pair1.getLast(),
                        pair2.getLast()
                });
            }
        }
        return null;
    }

    /* logic needs to update: use memory access pairs to find patterns
	public static Pattern[] getFalconPatterns() {
		List<Pattern> patterns = new ArrayList<>();

		//P1 R1(x), W2(x)
		patterns.add(new Pattern(new PatternType("R1(x), W2(x)")));

		//P2 W1(x), R2(x)
        patterns.add(new Pattern(new PatternType("W1(x), R2(x)")));

        //P3 W1(x), W2(x)
        patterns.add(new Pattern(new PatternType("W1(x), W2(x)")));

        //P4 R1(x), W2(x), R1(x)
        patterns.add(new Pattern(new PatternType("R1(x), W2(x), R1(x)")));

        //P5 W1(x), W2(x), R1(x)
        patterns.add(new Pattern(new PatternType("W1(x), W2(x), R1(x)")));

        //P6 W1(x), R2(x), W1(x)
        patterns.add(new Pattern(new PatternType("W1(x), R2(x), W1(x)")));

        //P7 R1(x), W2(x), W1(x)
        patterns.add(new Pattern(new PatternType("R1(x), W2(x), W1(x)")));

        //P8 W1(x), W2(x), W1(x)
        patterns.add(new Pattern(new PatternType("W1(x), W2(x), W1(x)")));

        return patterns.toArray(new Pattern[patterns.size()]);
	}
	*/

    /*
	public static Pattern[] getUnicornPatterns() {
        List<Pattern> patterns = new ArrayList<>();

        //P1 R1(x), W2(x)
        patterns.add(new Pattern(new PatternType("R1(x), W2(x)")));

        //P2 W1(x), R2(x)
        patterns.add(new Pattern(new PatternType("W1(x), R2(x)")));

        //P3 W1(x), W2(x)
        patterns.add(new Pattern(new PatternType("W1(x), W2(x)")));

        //P4 R1(x), W2(x), R1(x)
        patterns.add(new Pattern(new PatternType("R1(x), W2(x), R1(x)")));

        //P5 W1(x), W2(x), R1(x)
        patterns.add(new Pattern(new PatternType("W1(x), W2(x), R1(x)")));

        //P6 W1(x), R2(x), W1(x)
        patterns.add(new Pattern(new PatternType("W1(x), R2(x), W1(x)")));

        //P7 R1(x), W2(x), W1(x)
        patterns.add(new Pattern(new PatternType("R1(x), W2(x), W1(x)")));

        //P8 W1(x), W2(x), W1(x)
        patterns.add(new Pattern(new PatternType("W1(x), W2(x), W1(x)")));

        //P9 W1(x), W2(x), W2(y), W1(y)
        patterns.add(new Pattern(new PatternType("W1(x), W2(x), W2(y), W1(y)")));

        //P10 W1(x), W2(y), W2(x), W1(y)
        patterns.add(new Pattern(new PatternType("W1(x), W2(y), W2(x), W1(y)")));

        //P11 W1(x), W2(y), W1(y), W2(x)
        patterns.add(new Pattern(new PatternType("W1(x), W2(y), W1(y), W2(x)")));

        //P12 W1(x), R2(x), R2(y), W1(y)
        patterns.add(new Pattern(new PatternType("W1(x), R2(x), R2(y), W1(y)")));

        //P13 W1(x), R2(y), R2(x), W1(y)
        patterns.add(new Pattern(new PatternType("W1(x), R2(y), R2(x), W1(y)")));

        //P14 R1(x), W2(x), W2(y), R1(y)
        patterns.add(new Pattern(new PatternType("R1(x), W2(x), W2(y), R1(y)")));

        //P15 R1(x), W2(y), W2(x), R1(y)
        patterns.add(new Pattern(new PatternType("R1(x), W2(y), W2(x), R1(y)")));

        //P16 R1(x), W2(y), R1(y), W2(x)
        patterns.add(new Pattern(new PatternType("R1(x), W2(y), R1(y), W2(x)")));

        //P17 W1(x), R2(y), W1(y), R2(x)
        patterns.add(new Pattern(new PatternType("W1(x), R2(y), W1(y), R2(x)")));

        return patterns.toArray(new Pattern[patterns.size()]);
    }
    */
}
