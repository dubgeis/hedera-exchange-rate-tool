package com.hedera.exchange;

/*-
 * ‌
 * Hedera Exchange Rate Tool
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.exchange.exchanges.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 * Operations Performed:
 * <ul>
 *  <li>load Exchanges using URLs provided and fetch HBAR-USD exchange rate.</li>
 *  <li>Calculate the median of the fetched exchange rates.</li>
 *  <li>Check if the median is valid : if small change ..i.e., if in bound.</li>
 *  <li>if not a small change clip it.</li>
 *  <li>check if the clipped rate is more then floor if not floor the rate.</li>
 *  <li>generate ExchangeRate file using the final calculated rate and return it.</li>
 *</ul>
 *  
 * @author Anirudh, Cesar
 */
public class ERTproc {

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private static final Map<String, Function<String, Exchange>> EXCHANGES = new HashMap<>();

    static {
        EXCHANGES.put("bitrex", Bitrex::load);
        EXCHANGES.put("liquid", Liquid::load);
        EXCHANGES.put("coinbase", Coinbase::load);
        EXCHANGES.put("upbit", UpBit::load);
        EXCHANGES.put("okcoin", OkCoin::load);
        EXCHANGES.put("binance", Binance::load);
    }

    private final Map<String, String> exchangeApis;
    private final long bound;
    private final long floor;
    private List<Exchange> exchanges;
    private final Rate midnightExchangeRate;
    private final Rate currentExchangeRate;
    private final long hbarEquiv;
    private final long frequencyInSeconds;

    public ERTproc(final long hbarEquiv,
            final Map<String, String> exchangeApis,
            final long bound,
            final long floor,
            final Rate midnightExchangeRate,
            final Rate currentExchangeRate,
            final long frequencyInSeconds) {
        this.hbarEquiv = hbarEquiv;
        this.exchangeApis = exchangeApis;
        this.bound = bound;
        this.floor = floor;
        this.midnightExchangeRate = midnightExchangeRate;
        this.currentExchangeRate = currentExchangeRate;
        this.frequencyInSeconds = frequencyInSeconds;
    }

    /**
     * Main method that executed the Logic to generate a new Exchange Rate by fetching the rates from respective Exchanges
     * and calculating the median among them. ALso perform isSmallChange checks and clip if necessary and floor the rate if
     * its lower then recommended value.
     * @return ExchangeRate object
     */
    public ExchangeRate call() {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Start of ERT Logic");

        try {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Generating exchange objects");
            exchanges = generateExchanges();
            currentExchangeRate.setExpirationTime(ERTParams.getCurrentExpirationTime());
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next hour as current expiration time :{}",
                    currentExchangeRate.getExpirationTimeInSeconds());
            final long nextExpirationTimeInSeconds = currentExchangeRate.getExpirationTimeInSeconds() + frequencyInSeconds;

            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next-next hour as next expiration time :{}",
                    nextExpirationTimeInSeconds);

            final Double medianExRate = ExchangeRateUtils.calculateMedianRate(exchanges);
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Median calculated : {}", medianExRate);
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Exchanges worked : {}", this.getExchangeJson());

            if (medianExRate == null){
                LOGGER.warn(Exchange.EXCHANGE_FILTER, "No median computed. Using current rate as next rate: {}",
                        this.currentExchangeRate.toJson());
                final Rate nextRate = new Rate(this.currentExchangeRate.getHBarEquiv(),
                        this.currentExchangeRate.getCentEquiv() ,
                        nextExpirationTimeInSeconds);
                return new ExchangeRate(this.currentExchangeRate, nextRate);
            }

            Rate nextRate = new Rate(this.hbarEquiv,
                    (int) (medianExRate * 100 * this.hbarEquiv),
                    nextExpirationTimeInSeconds);

            if(midnightExchangeRate != null && !midnightExchangeRate.isSmallChange(this.bound, nextRate)) {
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "last midnight value present. Validating the median with {}",
                        midnightExchangeRate.toJson());
                    nextRate = midnightExchangeRate.clipRate(nextRate, this.bound);
            } else {
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "No midnight value found, or the median is not far off " +
                        "Skipping validation of the calculated median");
            }

            LOGGER.debug(Exchange.EXCHANGE_FILTER, "checking floor");
            long newCentEquiv = Math.max(nextRate.getCentEquiv(), floor * nextRate.getHBarEquiv());
            if(newCentEquiv != nextRate.getCentEquiv()){
                LOGGER.warn(Exchange.EXCHANGE_FILTER, "Flooring the rate. calculated : {}, floored to : {}",
                        nextRate.getCentEquiv(), newCentEquiv);
                nextRate = new Rate(nextRate.getHBarEquiv(), newCentEquiv,
                        nextExpirationTimeInSeconds);
            }

            return new ExchangeRate(currentExchangeRate, nextRate);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the list of Exchange objects with HBAR-USD exchange rate using the URL endpoints provided for each
     * Exchange int he config file.
     * @return List of Exchange objects.
     */
    public List<Exchange> generateExchanges() {
        List<Exchange> exchanges = new ArrayList<>();

        for (final Map.Entry<String, String> api : this.exchangeApis.entrySet()) {
            final Function<String, Exchange> exchangeLoader = EXCHANGES.get(api.getKey());
            if (exchangeLoader == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not found", api.getKey());
                continue;
            }

            final String endpoint = api.getValue();
            final Exchange exchange = exchangeLoader.apply(endpoint);
            if (exchange == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not loaded", api.getKey());
                continue;
            }

            exchanges.add(exchange);
        }

        return exchanges;
    }


    /**
     * Return the list of exchanges that worked in json string format using OBJECT_MAPPER
     * @return Json String
     * @throws JsonProcessingException
     */
    public String getExchangeJson() throws JsonProcessingException {
        return Exchange.OBJECT_MAPPER.writeValueAsString(exchanges);
    }
}
