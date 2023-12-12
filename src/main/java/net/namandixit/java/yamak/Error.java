package net.namandixit.java.yamak;

public class Error {

    private final String file;
    private final Position pos;
    private final String msg;

    Error(String file, Position pos, String msg) {
        this.file = file;
        this.pos = pos;
        this.msg = msg;
    }

    public String toString(Error err) {
        String str = "[" + err.file + ":" + err.pos.from_line() + ":" + err.pos.from_cursor() + "] " + msg;
        return str;
    }
}
