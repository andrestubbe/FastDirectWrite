package fastdirectwrite;

public enum FontWeight {
    THIN(100),
    EXTRA_LIGHT(200),
    LIGHT(300),
    SEMI_LIGHT(350),
    REGULAR(400),
    MEDIUM(500),
    SEMI_BOLD(600),
    BOLD(700),
    EXTRA_BOLD(800),
    BLACK(900),
    EXTRA_BLACK(950);

    private final int value;

    FontWeight(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
