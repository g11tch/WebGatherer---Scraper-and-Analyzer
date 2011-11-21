package org.Webgatherer.Utility.HtmlParsing;

import com.google.inject.Inject;
import org.htmlcleaner.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Rick Dane
 */
public class HtmlParserImpl implements HtmlParser {

    private HtmlCleaner htmlCleaner;
    private CleanerProperties htmlCleanerProperties;

    @Inject
    public HtmlParserImpl(HtmlCleaner htmlCleaner) {
        this.htmlCleaner = htmlCleaner;
        htmlCleanerProperties = htmlCleaner.getProperties();
    }

    public List<String> extractLinks(String htmlPage) {
        TagNode node = htmlCleaner.clean(htmlPage);

        TagNode[] nodesHref = node.getElementsByName("a", true);
        List<String> urlList = new ArrayList<String>();

        for (TagNode curNode : nodesHref) {
            Map<String, String> attributes = curNode.getAttributes();
            if (attributes.containsKey("href")) {
                urlList.add(curNode.getText().toString());
                //urlList.add(attributes.get("href"));
            }
        }

        return urlList;
    }

    public String getText(String htmlPage) {
        TagNode node = htmlCleaner.clean(htmlPage);
        StringBuffer stringBuffer = node.getText();
        return stringBuffer.toString();
    }

    private void defaultConfigHtmlCleaner() {
        htmlCleanerProperties.setOmitComments(true);
        htmlCleanerProperties.setTreatUnknownTagsAsContent(true);
    }

}
