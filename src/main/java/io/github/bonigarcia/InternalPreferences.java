/*
 * (C) Copyright 2019 Boni Garcia (http://bonigarcia.github.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.bonigarcia;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.prefs.Preferences.userNodeForPackage;
import static org.slf4j.LoggerFactory.getLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.BackingStoreException;

import org.slf4j.Logger;

import io.github.bonigarcia.config.Config;

/**
 * Persistent data (as Java preferences).
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.0.0
 */
public class InternalPreferences {

    final Logger log = getLogger(lookup().lookupClass());

    static final String TTL = "-ttl";

    java.util.prefs.Preferences prefs = userNodeForPackage(
            SeleniumJupiter.class);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Config config;

    public InternalPreferences(Config config) {
        this.config = config;
    }

    public String getValueFromPreferences(String key) {
        return prefs.get(key, null);
    }

    public long getExpirationTimeFromPreferences(String key) {
        return prefs.getLong(getExpirationKey(key), 0);
    }

    public void putValueInPreferencesIfEmpty(String key, String value) {
        if (getValueFromPreferences(key) == null) {
            prefs.put(key, value);
            long expirationTime = new Date().getTime()
                    + SECONDS.toMillis(config.getTtlSec());
            prefs.putLong(getExpirationKey(key), expirationTime);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Storing {} for {} as Java preferences (valid until {})",
                        value, key, formatTime(expirationTime));
            }
        }
    }

    public void clearFromPreferences(String key) {
        prefs.remove(key);
        prefs.remove(getExpirationKey(key));
    }

    public void clear() {
        try {
            log.info("Clearing Selenium-Jupiter preferences");
            prefs.clear();
        } catch (BackingStoreException e) {
            log.warn("Exception clearing preferences", e);
        }
    }

    public boolean checkValidity(String key, String value,
            long expirationTime) {
        long now = new Date().getTime();
        boolean isValid = value != null && expirationTime != 0
                && expirationTime > now;
        if (!isValid) {
            String expirationDate = formatTime(expirationTime);
            log.debug("Removing preference {} {} (expired on {})", key, value,
                    expirationDate);
            clearFromPreferences(key);
        }
        return isValid;
    }

    public String formatTime(long time) {
        return dateFormat.format(new Date(time));
    }

    private String getExpirationKey(String key) {
        return key + TTL;
    }

    public boolean checkKeyInPreferences(String key) {
        String valueFromPreferences = getValueFromPreferences(key);
        boolean valueInPreferences = valueFromPreferences != null
                && !valueFromPreferences.isEmpty();
        if (valueInPreferences) {
            long expirationTime = getExpirationTimeFromPreferences(key);
            String expirationDate = formatTime(expirationTime);
            valueInPreferences &= checkValidity(key, valueFromPreferences,
                    expirationTime);
            if (valueInPreferences) {
                log.trace("{}={} in preferences (expiration date {})", key,
                        valueFromPreferences, expirationDate);
            }
        }
        return valueInPreferences;
    }

}
