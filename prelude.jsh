import java.util.concurrent.atomic.AtomicInteger;

void $(Function<String, Object> f) { lines().map(f).forEach(o -> println(o)); }
Stream<Line> $lines() { AtomicInteger idx = new AtomicInteger(); return lines().map(l -> new Line(idx.incrementAndGet(), l)); }
void $$(Function<Line, Object> f) { $lines().map(f).forEach(o -> println(o)); }
<T> void $(Stream<T> s) { s.forEach(i -> println(i)); }

class Line {
    private final String line;
    private final String pattern;
    private final String[] fields; 
    public final int nr;
    public final int nf;

    Line(int nr, String line, String pattern) {
        this.nr = nr;
        this.line = line;
        this.pattern = pattern;
        this.fields = line.split(pattern);
        this.nf = fields.length == 1 ? 0 : fields.length;
    }

    Line(int nr, String line) { this(nr, line, "\\s+"); }

    public String s(int n) {
        if (n == 0) return line;
        if (n > 0)
            return fields[n-1];
        else
            return fields[nf+n];
    }

    public int s(int n, String def) {
        try {
            return s(n);
        } catch (ArrayIndexOutOfBoundsException e) {
            return def;
        }
    }

    public int i(int n) {
        return Integer.parseInt(s(n));
    }

    public int i(int n, int def) {
        try {
            return i(n);
        } catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
            return def;
        }
    }

    public double d(int n) {
        return Double.parseDouble(s(n));
    }

    public double d(int n, double def) {
        try {
            return d(n);
        } catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
            return def;
        }
    }

    public String toString() { return line; }
}


