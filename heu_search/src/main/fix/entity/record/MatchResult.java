package fix.entity.record;

public class MatchResult {
    boolean flag = false;//whether match
    int index = -1;//which one match

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isFlag() {
        return flag;
    }

    public int getIndex() {
        return index;
    }
}
