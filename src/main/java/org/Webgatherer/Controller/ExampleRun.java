package org.Webgatherer.Controller;

import org.Webgatherer.CoreEngine.DependencyInjection.DependencyBindingModule;
import org.Webgatherer.CoreEngine.Core.ThreadCommunication.FinalOutputContainer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.Webgatherer.WorkflowExample.DataHolders.ContainerBase;

import java.util.*;


/**
 * @author Rick Dane
 */
public class ExampleRun {

    public static void main(String[] args) {

        Injector injector = Guice.createInjector(new DependencyBindingModule());

        ControllerFlow wfContrl = injector.getInstance(ControllerFlow.class);
        FinalOutputContainer finalOutputContainer = launchWebGathererThread(injector, wfContrl, "org.Webgatherer.WorkflowExample.Workflows.Implementations.WebGatherer.Workflow_WebGather_1", "org.Webgatherer.WorkflowExample.Workflows.Implementations.DataInterpetor.Workflow_DataInterpretor_1");

        testPrintResults(finalOutputContainer);
    }

    private static FinalOutputContainer launchWebGathererThread(Injector injector, ControllerFlow wfContrl, String workflow2, String workflow3) {
        List<String> workflowlist = new ArrayList<String>();
        workflowlist.add(workflow2);
        workflowlist.add(workflow3);

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("pageQueue", testLoadPages());

        FinalOutputContainer finalOutputContainer = injector.getInstance(FinalOutputContainer.class);

        wfContrl.configure(finalOutputContainer, workflowlist, parameterMap);
        wfContrl.start();
        return finalOutputContainer;
    }

    private static void testPrintResults(FinalOutputContainer finalOutputContainer) {
        int THREAD_SLEEP = 15000;
        int LIST_FIRST_ITEM = 0;

        while (true) {
            Map<String, ContainerBase> outputMap = null;
            try {
                outputMap = finalOutputContainer.removeFromFinalOutputContainer();
            } catch (Exception e) {

                try {
                    Thread.sleep(THREAD_SLEEP);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                continue;
            }
            if (outputMap == null || outputMap.isEmpty()) {
                try {
                    Thread.sleep(THREAD_SLEEP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            ContainerBase outputContainer = outputMap.get("site1.aboutus");
            LinkedList<String> list = outputContainer.getEntries();
            System.out.println(list.get(0));

            break;

        }
    }

    private static Queue testLoadPages() {

        Queue<String[]> pageQueue = new LinkedList<String[]>();

        String[] site1 = {"site1", "", null, null};

        pageQueue.add(site1);

        return pageQueue;
    }

}
