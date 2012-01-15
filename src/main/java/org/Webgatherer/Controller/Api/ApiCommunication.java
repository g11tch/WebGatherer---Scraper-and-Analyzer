package org.Webgatherer.Controller.Api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rickdane.springmodularizedproject.api.transport.EmailTransport;
import com.rickdane.springmodularizedproject.api.transport.Rawscrapeddata;
import com.rickdane.springmodularizedproject.api.transport.Scraper;
import com.rickdane.springmodularizedproject.api.transport.TransportBase;
import org.Webgatherer.Api.Scraper.ScraperFactory;
import org.Webgatherer.Common.Properties.PropertiesContainer;
import org.Webgatherer.Controller.EntityTransport.EntryTransport;
import org.Webgatherer.CoreEngine.Core.ThreadCommunication.ThreadCommunication;
import org.Webgatherer.CoreEngine.Core.ThreadCommunication.ThreadCommunicationBase;
import org.Webgatherer.ExperimentalLabs.DependencyInjection.DependencyBindingModule;
import org.Webgatherer.ExperimentalLabs.EmailExtraction.PageRetrieverThreadManagerEmailExtraction;
import org.Webgatherer.ExperimentalLabs.Mail.SendEmail;
import org.Webgatherer.ExperimentalLabs.Scraper.Core.ScraperBase;
import org.Webgatherer.Persistence.InputOutput.PersistenceImpl_WriteToFile;
import org.Webgatherer.Utility.RandomSelector;
import org.Webgatherer.Utility.ReadFiles;
import org.Webgatherer.WorkflowExample.Workflows.Base.DataInterpetor.EmailExtractor;
import org.Webgatherer.WorkflowExample.Workflows.Implementations.WebGatherer.EnumUrlRetrieveOptions;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This is the main method for the applicatin, it polls the API at a set interval to check for new jobs to run
 *
 * @author Rick Dane
 */
public class ApiCommunication extends BaseApiCommunication {

    private static final String baseApiUrl = "http://localhost:8080/springmodularizedproject/";
    private static final String serviceEndpointGetScraper = baseApiUrl + "webgathererjobs/getPendingJobToLaunch";
    private static final String servicePersistRawscrapeddata = baseApiUrl + "rawscrapeddatas";
    private static final String serviceUrlsAwaitingEmailScrape = baseApiUrl + "rawscrapeddatas/retrieveUrlsAwaitingEmailScrape";
    private static final String scraperEndPoint = baseApiUrl + "/scrapers";
    private static final String postEmailListEndPoint = baseApiUrl + "/emailaddresses/postEmailMessages";

    private static final String emailToSendEndPoint = baseApiUrl + "/emailaddresses/getEmailToSend";

    private static int callIntervalSeconds = 10;
    private static boolean isRunning = true;

    private static int pageNum = 1;
    private static int maxPages = 1;

    private static int maxUrlEmailScrapeUrls = 20;

    private static int sizeOfStringArrayEnum = 9;
    
    private static PropertiesContainer propertiesContainer = new PropertiesContainer();

    public static void main(String[] args) {

        while (isRunning) {
            EntryTransport entryTransport = new EntryTransport();
            Scraper curScraper = apiPost(entryTransport, serviceEndpointGetScraper, Scraper.class);

//            runUrlScrapeJob(curScraper);
//
//            runEmailScrapeJob();

            Date curTime = new Date();

//            if (nextEmailSendTime == null || curTime.getTime() > nextEmailSendTime.getTime()) {
//                getEmailAndSend();
//            }

            postEmailList();

            sleep();

        }
    }


    private static void runEmailScrapeJob() {
        int i = 1;
        Map<String, Rawscrapeddata> rawscrapeddataList = new HashMap<String, Rawscrapeddata>();
        for (i = 1; i <= maxUrlEmailScrapeUrls; i++) {

            //dummy object
            TransportBase transportBase = new TransportBase();

            Rawscrapeddata rawscrapeddata = apiPost(transportBase, serviceUrlsAwaitingEmailScrape, Rawscrapeddata.class);

            if (rawscrapeddata != null) {
                rawscrapeddataList.put(rawscrapeddata.getUrl(), rawscrapeddata);
            }
        }

        if (!rawscrapeddataList.isEmpty()) {
            runEmailExtractionJob(rawscrapeddataList);
        }

    }
    
    private static void postEmailList() {
       
        List<EmailTransport> emailTransportList = new ArrayList<EmailTransport> ();

        EmailTransport trans1 = new EmailTransport();

        trans1.setSubject("hi this is a test");

        EmailTransport trans2 = new EmailTransport();
        trans2.setSubject("just something to check what its doing, yeah");

        emailTransportList.add(trans1);
        emailTransportList.add(trans2);

        apiPost(emailTransportList, postEmailListEndPoint);
    }

    private static boolean runUrlScrapeJob(Scraper curScraper) {

        String scraperType = "";

        if (curScraper.getType() == Scraper.Type.CRAIGSLIST) {
            scraperType = "generic";
        } else {
            return false;
        }

        ScraperBase scraper = ScraperFactory.createScraper(scraperType);

        List<String[]> urlEntries = scraper.run("java", pageNum, maxPages);

        for (String[] curEntry : urlEntries) {
            Rawscrapeddata rawscrapeddata = new Rawscrapeddata();
            rawscrapeddata.setUrl(curEntry[1]);
            rawscrapeddata.setFkScraperId(curScraper.getId());
            rawscrapeddata.setRawscrapeddataEmailScrapeAttempted(Rawscrapeddata.RawscrapeddataEmailScrapeAttempted.NOT_ATTEMPTED);
            apiPost(rawscrapeddata, servicePersistRawscrapeddata, Rawscrapeddata.class);
        }

        curScraper.setStatus(Scraper.ProcessStatus.PROCESSED);
        apiPut(curScraper, scraperEndPoint);

        return true;
    }

    private static void sleep() {
        try {
            Thread.sleep(callIntervalSeconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static Queue<String> prepareQueueForEmails(Map<String, Rawscrapeddata> rawscrapeddataList) {

        Queue queue = new ConcurrentLinkedQueue<String>();

        for (Map.Entry<String, Rawscrapeddata> curEntry : rawscrapeddataList.entrySet()) {
            Rawscrapeddata curRawscrapeddata = curEntry.getValue();
            String[] testEntry = new String[sizeOfStringArrayEnum];
            testEntry[ThreadCommunicationBase.PageQueueEntries.BASE_URL.ordinal()] = curRawscrapeddata.getUrl();
            queue.add(testEntry);
        }
        return queue;
    }

    private static void runEmailExtractionJob(Map<String, Rawscrapeddata> rawscrapeddataList) {

        Injector injector = Guice.createInjector(new DependencyBindingModule());

        PageRetrieverThreadManagerEmailExtraction pageRetrieverThreadManager = injector.getInstance(PageRetrieverThreadManagerEmailExtraction.class);

        ThreadCommunication threadCommunication = injector.getInstance(ThreadCommunication.class);
        pageRetrieverThreadManager.configure(threadCommunication);

        threadCommunication.setPageQueue(prepareQueueForEmails(rawscrapeddataList));

        while (!threadCommunication.isPageQueueEmpty()) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
            }
            pageRetrieverThreadManager.run(EnumUrlRetrieveOptions.HTMLPAGE.ordinal());
        }

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
        }

        while (!threadCommunication.isOutputDataHolderEmpty()) {
            //TODO: Note that this will overwrite where there is more than 1 rawscrapeddata entry per url, consider re-working this at some point to account for this
            String[] curEntry = threadCommunication.getFromOutputDataHolder();
            String urlKey = curEntry[ThreadCommunicationBase.PageQueueEntries.BASE_URL.ordinal()];
            String email = curEntry[ThreadCommunicationBase.PageQueueEntries.EMAIL_ADDRESSES.ordinal()];

            Rawscrapeddata curRawscrapeddata = rawscrapeddataList.get(urlKey);
            curRawscrapeddata.setEmailAddress(email);
            curRawscrapeddata.setRawscrapeddataEmailScrapeAttempted(Rawscrapeddata.RawscrapeddataEmailScrapeAttempted.ATTEMPTED);

            apiPut(curRawscrapeddata, servicePersistRawscrapeddata);
        }


    }

    private static void getEmailAndSend() {
        //dummy object
        TransportBase transportBase = new TransportBase();

        EmailTransport emailTransport = apiPost(transportBase, emailToSendEndPoint, EmailTransport.class);
        if (emailTransport.getToEmail() != null) {
            sendEmail(emailTransport);
        }

    }


//    private static final int minDelay = 90000;
//    private static final int maxDelay = 260000;
    private static final int minDelay = 3000;
    private static final int maxDelay = 7000;

    private static RandomSelector randomSelector;
    private static Date nextEmailSendTime = null;
    
    private static Properties emailProperties = propertiesContainer.getProperties("emailAccounts");

    private static void sendEmail(EmailTransport emailTransport) {
        Injector injector = Guice.createInjector(new DependencyBindingModule());

        SendEmail sendEmail = injector.getInstance(SendEmail.class);
        sendEmail.configure(emailProperties.getProperty("email_fromName"), emailProperties.getProperty("email1_smtp"), emailProperties.getProperty("email1_address"),emailProperties.getProperty("email1_password"), emailProperties.getProperty("email1_smtp_port"));
        String attachmentFilePath = emailProperties.getProperty("email_attachment1");

        String body = emailTransport.getBody();
        String subject = emailTransport.getSubject();
        sendEmail.sendEmail(body, subject, emailTransport.getToEmail(), attachmentFilePath);   //curEmail

        randomSelector = injector.getInstance(RandomSelector.class);
        int delay = randomSelector.generateRandomNumberInRange(minDelay, maxDelay);

        Date date = new Date();

        nextEmailSendTime = new Date(date.getTime() + delay);
    }


}