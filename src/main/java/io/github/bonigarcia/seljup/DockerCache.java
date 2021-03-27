/*
 * (C) Copyright 2021 Boni Garcia (http://bonigarcia.github.io/)
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
package io.github.bonigarcia.seljup;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

import org.slf4j.Logger;

import io.github.bonigarcia.seljup.config.Config;
import io.github.bonigarcia.wdm.config.WebDriverManagerException;

/**
 * Docker cache.
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.5.0
 */
public class DockerCache {

    final Logger log = getLogger(lookup().lookupClass());

    static final String TTL = "-ttl";
    static final String DOCKER_CACHE_INFO = "Docker Cache (pulled and latest versions from Docker Hub)";

    Properties props = new Properties() {
        private static final long serialVersionUID = -5396369781645687583L;

        @Override
        public synchronized Enumeration<Object> keys() {
            return Collections.enumeration(new TreeSet<Object>(super.keySet()));
        }
    };

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy z");
    Config config;
    File dockerCacheFile;

    public DockerCache(Config config) {
        this.config = config;

        if (!config.isDockerAvoidCache()) {
            String cachePath = config.getCachePath();

            // Create folder if not exits
            File cacheFolder = new File(cachePath);
            if (!cacheFolder.exists()) {
                cacheFolder.mkdirs();
            }

            this.dockerCacheFile = new File(cachePath, config.getDockerCache());
            try {
                if (!dockerCacheFile.exists()) {
                    boolean createNewFile = dockerCacheFile.createNewFile();
                    if (createNewFile) {
                        log.debug("Created new Docker cache file at {}",
                                dockerCacheFile);
                    }
                }
                try (InputStream fis = new FileInputStream(dockerCacheFile)) {
                    props.load(fis);
                }
            } catch (Exception e) {
                throw new WebDriverManagerException(
                        "Exception reading Docker cache as a properties file",
                        e);
            }
        }
    }

    public String getValueFromDockerCache(String key) {
        return props.getProperty(key, null);
    }

    private Date getExpirationDateFromDockerCache(String key) {
        Date result = new Date(0);
        try {
            result = dateFormat.parse(props.getProperty(getExpirationKey(key)));
            return result;
        } catch (Exception e) {
            log.warn("Exception parsing date ({}) from Docker cache {}", key,
                    e.getMessage());
        }
        return result;
    }

    public void putValueInDockerCacheIfEmpty(String key, String value) {
        int ttl = config.getTtlSec();
        if (ttl > 0 && getValueFromDockerCache(key) == null) {
            props.put(key, value);

            long now = new Date().getTime();
            Date expirationDate = new Date(now + SECONDS.toMillis(ttl));
            String expirationDateStr = formatDate(expirationDate);
            props.put(getExpirationKey(key), expirationDateStr);
            log.trace("Storing {}={} in Docker cache (valid until {})", key,
                    value, expirationDateStr);
            storeProperties();
        }
    }

    private synchronized void storeProperties() {
        try (OutputStream fos = new FileOutputStream(dockerCacheFile)) {
            props.store(fos, DOCKER_CACHE_INFO);
        } catch (Exception e) {
            log.warn("Exception writing Docker cache as a properties file {}",
                    e.getClass().getName());
        }
    }

    private void clearFromDockerCache(String key) {
        props.remove(key);
        props.remove(getExpirationKey(key));
    }

    public void clear() {
        log.info("Clearing Docker cache");
        props.clear();
        storeProperties();
    }

    private boolean checkValidity(String key, String value,
            Date expirationDate) {
        long now = new Date().getTime();
        long expirationTime = expirationDate != null ? expirationDate.getTime()
                : 0;
        boolean isValid = value != null && expirationTime != 0
                && expirationTime > now;
        if (!isValid) {
            log.trace("Removing {}={} from Docker cache (expired on {})", key,
                    value, expirationDate);
            clearFromDockerCache(key);
        }
        return isValid;
    }

    private String formatDate(Date date) {
        return date != null ? dateFormat.format(date) : "";
    }

    private String getExpirationKey(String key) {
        return key + TTL;
    }

    public boolean checkKeyInDockerCache(String key) {
        String valueFromDockerCache = getValueFromDockerCache(key);
        boolean valueInDockerCache = valueFromDockerCache != null
                && !valueFromDockerCache.isEmpty();
        if (valueInDockerCache) {
            Date expirationDate = getExpirationDateFromDockerCache(key);
            valueInDockerCache &= checkValidity(key, valueFromDockerCache,
                    expirationDate);
            if (valueInDockerCache) {
                String strDate = formatDate(expirationDate);
                log.trace("Found {}={} in Docker cache (valid until {})", key,
                        valueFromDockerCache, strDate);
            }
        }
        return valueInDockerCache;
    }

}
