package fastdirectwrite;

public enum FontStyle {
    NORMAL(0),
    OBLIQUE(1),
    ITALIC(2);

    private final int value;

    FontStyle(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
