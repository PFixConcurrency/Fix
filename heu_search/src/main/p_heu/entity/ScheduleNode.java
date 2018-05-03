package p_heu.entity;

public class ScheduleNode extends Node {
    private String thread;
    private String location;
    private String type;

    public ScheduleNode(int id, String thread, String location, String type) {
        this.id = id;
        this.thread = thread;
        this.location = location;
        this.type = type;
    }

    public String getThread() {
        return this.thread;
    }

    public String getLocation() {
        return location;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean isIdentical(Node node) {
        return this.id == node.id && isSame(node);
    }

    @Override
    public boolean isSame(Node node) {
        if (node instanceof ScheduleNode) {
            ScheduleNode scheduleNode = (ScheduleNode)node;
            return this.thread.equals(scheduleNode.getThread())
                    && this.location.equals(scheduleNode.getLocation())
                    && this.type.equals(scheduleNode.getType());
        }
        return false;
    }

    @Override
    public String toString() {
        return "ScheduleNode[" + id + "," + thread + "," + location + "," + type + "]";
    }
}
