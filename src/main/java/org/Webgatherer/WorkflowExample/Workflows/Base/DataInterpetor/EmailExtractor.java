package org.Webgatherer.WorkflowExample.Workflows.Base.DataInterpetor;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rick Dane
 */
public class EmailExtractor {

    private int lastIndexNeedsRemoval;
    private int calcLastIndex;
    private String curHtmlPage;

    public LinkedList<String> extractEmailAddressesList(String htmlPage) {
        curHtmlPage = htmlPage;
        String retEmailAddr = "";
        LinkedList<String> retList = new LinkedList<String>();

        while (retEmailAddr != null) {
            try {
                retEmailAddr = extractEmailAddress();
            } catch (Exception e) {
                return retList;
            }
            curHtmlPage = curHtmlPage.substring(calcLastIndex, curHtmlPage.length() - 1);
            if (retEmailAddr != null) {
                retList.add(retEmailAddr);
            }
        }
        return retList;
    }

    private String extractEmailAddress() {
        int index = curHtmlPage.indexOf("@"); //
        lastIndexNeedsRemoval = index;

        String retStr = runEmailFindLoop(curHtmlPage, index, false, false);
        String retStr2 = null;
        if (retStr == null) {
            return null;

        }
        retStr2 = runEmailFindLoop(curHtmlPage, index, true, false);
        if (retStr2 == null) {
            return null;
        }

        return retStr + "@" + retStr2;

    }

    private String doCalculation(String inputStr, int index, int iter, boolean isPlus) {
        if (isPlus) {
            int curIndex = index + iter;
            calcLastIndex = curIndex;
            return inputStr.substring(index + 1, curIndex);
        } else {
            int curIndex = index - iter;
            return inputStr.substring(curIndex, index);

        }
    }

    private String runEmailFindLoop(String inputStr, int index, boolean isPlus, boolean isRecursive) {
        boolean isAlphaNumeric = true;
        String setStr = null;
        String retStr = null;

        int iter = 1;
        if (isPlus) {
            iter = 2;
        }
        while (isAlphaNumeric) {

            setStr = doCalculation(inputStr, index, iter, isPlus);

            if (!isAplhaNumeric(setStr)) {
                break;
            }
            retStr = setStr;
            iter++;
        }

        String emailDomain = "";
        if (!isRecursive && isPlus) {
            //check for the email address domain name
            String checkPeriod = inputStr.substring(index + iter - 1, index + iter);
            if (checkPeriod.equals(".")) {
                emailDomain = "." + runEmailFindLoop(inputStr, index + iter - 1, isPlus, true);
                if (emailDomain == null) {
                    return null;
                }
                lastIndexNeedsRemoval = calcLastIndex;
            } else {
                return null;
            }
        }
        return retStr + emailDomain;
    }

    private boolean isAplhaNumeric(String inputStr) {
        Pattern p = Pattern.compile("^[a-zA-Z0-9]+$", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(inputStr);
        return m.find();
    }
}
