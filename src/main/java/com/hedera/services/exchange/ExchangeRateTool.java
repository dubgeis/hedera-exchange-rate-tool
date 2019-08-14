package com.hedera.services.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class ExchangeRateTool {

    // logger object to write logs into
    private static final Logger log = LogManager.getLogger(ExchangeRateTool.class);

    // member variables
    private String privateKey;
    private List<String> exchangeAPIList;
    private String mainNetAPI;
    private String pricingDBAPI;
    private Double maxDelta;
    private Double prevMedian;
    private Double currMedian;
    private String hederaFileIdentifier;
    private Double frequency;

    public static void main(String args[]){
        // TODO : read the config file and save the parameters.

        // using the frequency read from the config file, spawn a thread that does the functions.
     //   ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
     //   service.scheduleAtFixedRate( new ERTproc(privateKey, exchangeAPIList, mainNetAPI,
     //   pricingDBAPI, maxDelta, prevMedian, currMedian, hederaFileIdentifier),
     //           0, frequency, Timer.Seconds);

        // we wait a while for the thread to finish executing and fetch the details the ERTproc writes to the
        // database and update prev and curr medians so that we can send them to the new thread.


    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public List<String> getExchangeAPIList() {
        return exchangeAPIList;
    }

    public void setExchangeAPIList(List<String> exchangeAPIList) {
        this.exchangeAPIList = exchangeAPIList;
    }

    public String getMainNetAPI() {
        return mainNetAPI;
    }

    public void setMainNetAPI(String mainNetAPI) {
        this.mainNetAPI = mainNetAPI;
    }

    public String getPricingDBAPI() {
        return pricingDBAPI;
    }

    public void setPricingDBAPI(String pricingDBAPI) {
        this.pricingDBAPI = pricingDBAPI;
    }

    public Double getMaxDelta() {
        return maxDelta;
    }

    public void setMaxDelta(Double maxDelta) {
        this.maxDelta = maxDelta;
    }

    public Double getPrevMedian() {
        return prevMedian;
    }

    public void setPrevMedian(Double prevMedian) {
        this.prevMedian = prevMedian;
    }

    public Double getCurrMedian() {
        return currMedian;
    }

    public void setCurrMedian(Double currMedian) {
        this.currMedian = currMedian;
    }

    public String getHederaFileIdentifier() {
        return hederaFileIdentifier;
    }

    public void setHederaFileIdentifier(String hederaFileIdentifier) {
        this.hederaFileIdentifier = hederaFileIdentifier;
    }

    public Double getFrequency() {
        return frequency;
    }

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }
}
