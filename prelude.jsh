import java.util.concurrent.atomic.AtomicInteger;

void $(Function<String, Object> f) { lines().map(f).forEach(o -> println(o)); }
Stream<Line> $lines() { AtomicInteger idx = new AtomicInteger(); return lines().map(l -> new Line(idx.incrementAndGet(), l)); }
void $$(Function<Line, Object> f) { $lines().map(f).forEach(o -> println(o)); }
<T> void $(Stream<T> s) { s.forEach(i -> println(i)); }

class Line {
    private final String line;
    private final int nr;
    private String[] fields; 
    private String fs = "\\s+";

    Line(int nr, String line) {
        this.nr = nr;
        this.line = line;
    }

    private String[] fields() {
        if (fields == null) {
            fields = line.split(fs);
        }
        return fields;
    }

    // Returns Number of Record
    public int nr() {
        return nr;
    }

    // Returns Field Separator
    public String fs() {
        return fs;
    }

    // Sets Field Separator and reparses line
    public Line fs(String fs) {
        this.fs = fs;
        fields = null;
        return this;
    }

    // Returns Number of Fields
    public int nf() {
        return fields().length;
    }

    // Returns the Field with the given index, counting from 1.
    // Index 0 returns the entire line while negative indices
    // count from the last element backwards.
    public String s(int n) {
        if (n == 0) return line;
        if (n > 0)
            return fields()[n-1];
        else
            return fields()[nf()+n];
    }

    // Like s(n) but returns the def value if the index n is out of range
    public String s(int n, String def) {
        try {
            return s(n);
        } catch (ArrayIndexOutOfBoundsException e) {
            return def;
        }
    }

    // Returns the indicated Field as an integer
    public int i(int n) {
        return Integer.parseInt(s(n));
    }

    // Like i(n) but returns the def value if the index n is out of range
    // or the Field cannot be parsed as an integer
    public int i(int n, int def) {
        try {
            return i(n);
        } catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
            return def;
        }
    }

    // Returns the indicated Field as a double
    public double d(int n) {
        return Double.parseDouble(s(n));
    }

    // Like d(n) but returns the def value if the index n is out of range
    // or the Field cannot be parsed as a double
    public double d(int n, double def) {
        try {
            return d(n);
        } catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
            return def;
        }
    }

    public String toString() { return line; }
}


