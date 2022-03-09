void $(Function<String, Object> f) { lines().map(f).forEach(o -> println(o)); }
Stream<Line> $lines() { return lines().map(Line::new); }
void $$(Function<Line, Object> f) { $lines().map(f).forEach(o -> println(o)); }
<T> void $(Stream<T> s) { s.forEach(i -> println(i)); }

class Line {
    private final String line;
    private final String pattern;
    private final String[] fields; 
    public final int nf;

    Line(String line, String pattern) {
        this.line = line;
        this.pattern = pattern;
        this.fields = line.split(pattern);
        this.nf = fields.length == 1 ? 0 : fields.length;
    }

    Line(String line) { this(line, "\\s+"); }

    public String s(int n) {
        if (n == 0) return line;
        return fields[n-1];
    }

    public int i(int n) {
        return Integer.parseInt(s(n));
    }

    public double d(int n) {
        return Double.parseDouble(s(n));
    }

    public String toString() { return line; }
}


