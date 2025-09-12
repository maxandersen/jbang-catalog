


///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//REPOS central,jitpack
//DEPS com.github.maxandersen:jjava:940cc9284775f1f1ba621dbb1d212c6e9108dc76
//JAVA_OPTIONS -ea --add-opens jdk.jshell/jdk.jshell=ALL-UNNAMED

public class jjbang {
    public static void main(String... args) throws Exception {
        org.dflib.jjava.JJava.main(args);
    }
}
