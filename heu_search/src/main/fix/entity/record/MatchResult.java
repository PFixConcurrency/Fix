package fix.entity.record;

public class MatchResult {
    boolean flag = false;//表示是否匹配
    int index = -1;//表示是第几个匹配

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
