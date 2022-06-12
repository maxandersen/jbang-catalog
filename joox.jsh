//JAVA 9+
//DEPS org.jooq:joox:1.6.2
import org.w3c.dom.Document;
import static org.joox.JOOX.*;

Document sample() throws org.xml.sax.SAXException, java.io.IOException {

	return $(new StringReader("<document>\n" +
        "  <library name=\"Amazon\">\n" +
        "    <books>\n" +
        "      <book id=\"1\">\n" +
        "        <name>1984</name>\n" +
        "        <authors>\n" +
        "          <author>George Orwell</author>\n" +
        "        </authors>\n" +
        "      </book>\n" +
        "      <book id=\"2\">\n" +
        "        <name>Animal Farm</name>\n" +
        "        <authors>\n" +
        "          <author>George Orwell</author>\n" +
        "        </authors>\n" +
        "      </book>\n" +
        "      <book id=\"3\">\n" +
        "        <name>O Alquimista</name>\n" +
        "        <authors>\n" +
        "          <author>Paulo Coelho</author>\n" +
        "        </authors>\n" +
        "      </book>\n" +
        "      <book id=\"4\">\n" +
        "        <name>Brida</name>\n" +
        "        <authors>\n" +
        "          <author>Paulo Coelho</author>\n" +
        "        </authors>\n" +
        "      </book>\n" +
        "    </books>\n" +
        "\n" +
        "    <dvds>\n" +
        "      <dvd id=\"5\">\n" +
        "        <name>Once Upon a Time in the West</name>\n" +
        "        <directors>\n" +
        "          <director>Sergio Leone</director>\n" +
        "        </directors>\n" +
        "        <actors>\n" +
        "          <actor>Charles Bronson</actor>\n" +
        "          <actor>Jason Robards</actor>\n" +
        "          <actor>Claudia Cardinale</actor>\n" +
        "        </actors>\n" +
        "      </dvd>\n" +
        "    </dvds>\n" +
        "  </library>\n" +
        "</document>")).document();
}

println("JOOX loaded - try use it:\n\n$(sample()).find(\"book\").filter(ids(\"1\"))\n");
