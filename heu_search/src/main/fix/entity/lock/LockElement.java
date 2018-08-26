package fix.entity.lock;
//Store each variable in the lock sequence
public class LockElement {
    public String instance;
    public String field;
    public String thread;
    public String location;

    public LockElement(String instance, String field, String thread, String location) {
        super();
        this.instance = instance;
        this.field = field;
        this.thread = thread;
        this.location = location;
    }


    public String toString(){
        return "instance: " + this.instance + "\tfield: " + this.field
                + "\tthread: " + this.thread + "\tlocation: " + this.location;
    }

}
