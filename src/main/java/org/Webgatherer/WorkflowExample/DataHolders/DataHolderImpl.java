package org.Webgatherer.WorkflowExample.DataHolders;

import org.Webgatherer.WorkflowExample.Status.StatusIndicator;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;

import java.util.*;

/**
 * @author Rick Dane
 */
public class DataHolderImpl implements DataHolder {

    private Map<String, ContainerBase> containerHolder = new HashMap<String, ContainerBase>();
    private Trie<String, String> internalFinishedKeyTracker = new PatriciaTrie<String, String>(StringKeyAnalyzer.INSTANCE);
    private Queue<String> finishedContainerKeys = new LinkedList<String>();
    private Map<String, Integer> contentTypesMap = new HashMap<String, Integer>();

    public boolean isFinishedContainerQueueEmpty() {
        return finishedContainerKeys.isEmpty();
    }

    public ContainerBase pullFromFinishedContainerQueue() {
        String finishedContainerKey = null;
        if (!finishedContainerKeys.isEmpty()) {
            finishedContainerKey = finishedContainerKeys.remove();

        }
        if (finishedContainerKey != null) {
            return containerHolder.get(finishedContainerKey);
        }
        return null;
    }

    /**
     * meant to be called before getContainerByIdentifier so calling code knows if it should even bother trying to retrieve object
     *
     * @param identifier
     * @return
     */
    public StatusIndicator checkIfContainerAvailable(String identifier) {
        ContainerBase cb = containerHolder.get(identifier);
        if (cb == null) {
            return StatusIndicator.DOESNOTEXIST;
        }
        if (cb.isUnLocked()) {
            return StatusIndicator.NOTAVAILABLE;
        }
        return StatusIndicator.AVAILABLE;
    }

    /**
     * returns the instance of Container that matches the key, if none exists or the instance is locked, it returns null
     *
     * @param identifier
     * @return
     */
    public ContainerBase getContainerByIdentifier(String identifier) {
        ContainerBase cb = containerHolder.get(identifier);
        if (cb == null || cb.isUnLocked()) {
            return null;
        }
        return cb;
    }


    public StatusIndicator createContainer(String identifier, int maxEntries, int maxAttempts) {
        if (containerHolder.containsKey(identifier)) {
            return StatusIndicator.ALREADYEXISTS;
        }
        //TODO, convert this to DI
        ContainerBase cb = new ContainerBase(identifier, maxEntries, maxAttempts);
        containerHolder.put(identifier, cb);
        return StatusIndicator.SUCCESS;
    }

    public StatusIndicator addEntryToContainer(String identifier, String entry) {
        ContainerBase cb = containerHolder.get(identifier);
        if (cb == null) {
            return StatusIndicator.DOESNOTEXIST;
        }
        if (cb.isUnLocked()) {
            return StatusIndicator.NOTAVAILABLE;
        }
        StatusIndicator status = cb.addContent(entry);
        if (status == StatusIndicator.JUSTUNLOCKED) {
            finishedContainerKeys.add(identifier);
            internalFinishedKeyTracker.put(identifier, null);
        }
        return StatusIndicator.SUCCESS;
    }

    public void incrementContainerAllowedAttempts(String identifier) {
        ContainerBase cb = containerHolder.get(identifier);
        StatusIndicator status = cb.incrementAttempts();
        if (status == StatusIndicator.JUSTUNLOCKED) {
            finishedContainerKeys.add(identifier);
            internalFinishedKeyTracker.put(identifier, null);
        }
    }

    /**
     * This should only be called when the thread is ready to be destroyed (all pages have been visited), generally its good practice
     * to make sure there has been a delay of at least a few seconds without any new pages coming into the workflow queue before calling this,
     * it gives the remaining data to the workflow that never reached its max number of attempts
     *
     * @return
     */
    public void destroyRetrieveFinalData() {
        for (Map.Entry<String, ContainerBase> curEntry : containerHolder.entrySet()) {
            String id = curEntry.getKey();
            ContainerBase cb = curEntry.getValue();
            if (!internalFinishedKeyTracker.containsKey(id)) {
                cb.forceUnlock();
                finishedContainerKeys.add(id);
                internalFinishedKeyTracker.put(id, null);
            }
        }
    }

}
