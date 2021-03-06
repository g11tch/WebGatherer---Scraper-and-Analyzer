package org.Webgatherer.CoreEngine.Core.Threadable.WebGather;

import com.google.inject.Inject;
import org.Webgatherer.CoreEngine.Core.ThreadCommunication.ThreadCommunication;
import org.Webgatherer.CoreEngine.Core.ThreadCommunication.ThreadCommunicationBase;
import org.Webgatherer.CoreEngine.lib.WebDriverFactory;
import org.Webgatherer.ExperimentalLabs.HtmlProcessing.HtmlParser;
import org.Webgatherer.WorkflowExample.Workflows.Base.DataInterpetor.TextExtraction;
import org.Webgatherer.WorkflowExample.Workflows.Implementations.WebGatherer.EnumUrlRetrieveOptions;
import org.apache.commons.net.nntp.Threadable;
import org.openqa.selenium.WebDriver;

import java.util.Date;

/**
 * @author Rick Dane
 */
public class ThreadRetrievePage extends Thread {

    protected WebDriver driver;
    private TextExtraction textExtraction;
    protected ThreadCommunication threadCommunication;
    protected String[] entry;
    private int retrieveType;
    private ThreadCommunicationPageRetriever threadCommunicationPageRetriever;
    protected HtmlParser htmlParser;

    @Inject
    public ThreadRetrievePage(WebDriverFactory webDriverFactory, TextExtraction textExtraction, HtmlParser htmlParser) {
        driver = webDriverFactory.createNewWebDriver();
        this.textExtraction = textExtraction;
        this.htmlParser = htmlParser;
        this.threadCommunication = threadCommunication;
    }

    public void configure(String[] entry, ThreadCommunication threadCommunication, int retrieveType, ThreadCommunicationPageRetriever threadCommunicationPageRetriever) {
        this.threadCommunication = threadCommunication;
        this.entry = entry;
        this.retrieveType = retrieveType;
        this.threadCommunicationPageRetriever = threadCommunicationPageRetriever;
    }

    public void run() {
        retrievePageFromUrl();
    }

    public void retrievePageFromUrl() {
        System.out.println("thread launched");

        threadCommunicationPageRetriever.registerNewThread();

        //if domain was marked as slow, don't attempt to load page
        if (threadCommunicationPageRetriever.doesSlowLoadingUrlsContain(entry[ThreadCommunicationBase.PageQueueEntries.KEY.ordinal()])) {
            threadCommunicationPageRetriever.registerKilledThread();
            System.out.println("domain marked as slow");
            return;
        }

        Date before = new Date();
        try {

            if (!actionIfUrlValid()) {
                return;
            }

            getPage();

            Date after = new Date();

            if (after.getTime() - before.getTime() > threadCommunicationPageRetriever.getMaxMillisecondTimeout()) {
                //this is due to a bug in the Selenium Web Driver, it doesn't always respect the timeout setting so we need to
                //check how long the request took and if it was too long then block all future urls from this site since it will make the crawler too slow
                threadCommunicationPageRetriever.addSlowLoadingIgnoreUrl(entry[ThreadCommunicationBase.PageQueueEntries.KEY.ordinal()]);
                threadCommunicationPageRetriever.registerKilledThread();
                return;
            }

            if (retrieveType == EnumUrlRetrieveOptions.HTMLPAGE.ordinal()) {
                addPage();

            }
            if (retrieveType == EnumUrlRetrieveOptions.TEXTPAGE.ordinal()) {
                //TODO change this, its not currently implemented
            }

            threadCommunication.addToOutputDataHolder(entry);

            driver.close();
        } catch (Exception e) {
            driver.close();
        }

        threadCommunicationPageRetriever.registerKilledThread();
    }

    protected boolean actionIfUrlValid() {
        if (!textExtraction.isUrlValid(entry[ThreadCommunicationBase.PageQueueEntries.BASE_URL.ordinal()])) {
            threadCommunicationPageRetriever.registerKilledThread();
            return false;
        }
        return true;
    }

    protected void addPage() {
        entry[ThreadCommunicationBase.PageQueueEntries.SCRAPED_PAGE.ordinal()] = driver.getPageSource();
    }

    protected void getPage() {
        driver.get(entry[ThreadCommunicationBase.PageQueueEntries.BASE_URL.ordinal()]);
    }

    public boolean isDummy() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String messageThreadId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] messageThreadReferences() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String simplifiedSubject() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean subjectIsReply() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setChild(Threadable threadable) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setNext(Threadable threadable) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Threadable makeDummy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
