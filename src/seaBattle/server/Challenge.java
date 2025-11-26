package seaBattle.server;

public class Challenge {
    private final long id;
    private final String fromNic;
    private final String toNic;

    public Challenge(long id, String fromNic, String toNic) {
        this.id = id;
        this.fromNic = fromNic;
        this.toNic = toNic;
    }

    public long getId() {
        return id;
    }

    public String getFromNic() {
        return fromNic;
    }

    public String getToNic() {
        return toNic;
    }
}
