///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.tomitribe.jamira:jamira-cli:0.1

public class jamira {

    public static void main(String... args) throws Exception {
        org.tomitribe.crest.Main.main(args);
    }
}
