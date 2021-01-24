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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Locale;

import com.google.common.net.HostAndPort;

/**
 * It represents a dockerd endpoint (a codified DOCKER_HOST).
 *
 * @author Boni Garcia (boni.gg@gmail.com)
 * @since 3.4.0
 */

public class DockerHost {

    private static final String DEFAULT_UNIX_ENDPOINT = "unix:///var/run/docker.sock";
    private static final String DEFAULT_WINDOWS_ENDPOINT = "npipe:////./pipe/docker_engine";
    private static final String DEFAULT_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 2375;

    private final String host;
    private final URI uri;
    private final URI bindUri;
    private final String address;
    private final int port;
    private final String certPath;
    private final String endpoint;

    private DockerHost(final String endpoint, final String certPath) {
        if (endpoint.startsWith("unix://")) {
            this.port = 0;
            this.address = DEFAULT_ADDRESS;
            this.host = endpoint;
            this.uri = URI.create(endpoint);
            this.bindUri = URI.create(endpoint);
        } else {
            final String stripped = endpoint.replaceAll(".*://", "");
            final HostAndPort hostAndPort = HostAndPort.fromString(stripped);
            final String hostText = hostAndPort.getHost();
            final String scheme = isNullOrEmpty(certPath) ? "http" : "https";

            this.port = hostAndPort.getPortOrDefault(defaultPort());
            this.address = isNullOrEmpty(hostText) ? DEFAULT_ADDRESS : hostText;
            this.host = address + ":" + port;
            this.uri = URI.create(scheme + "://" + address + ":" + port);
            this.bindUri = URI.create("tcp://" + address + ":" + port);
        }

        this.endpoint = endpoint;
        this.certPath = certPath;
    }

    public String endpoint() {
        return endpoint;
    }

    public String host() {
        return host;
    }

    public URI uri() {
        return uri;
    }

    public URI bindUri() {
        return bindUri;
    }

    public int port() {
        return port;
    }

    public String address() {
        return address;
    }

    public String dockerCertPath() {
        return certPath;
    }

    public static DockerHost fromEnv() {
        final String host = endpointFromEnv();
        final String certPath = certPathFromEnv();
        return new DockerHost(host, certPath);
    }

    public static DockerHost from(final String endpoint,
            final String certPath) {
        return new DockerHost(endpoint, certPath);
    }

    public static String defaultDockerEndpoint() {
        final String osName = System.getProperty("os.name");
        final String os = osName.toLowerCase(Locale.ENGLISH);
        if (os.equalsIgnoreCase("linux") || os.contains("mac")) {
            return defaultUnixEndpoint();
        } else if (System.getProperty("os.name")
                .equalsIgnoreCase("Windows 10")) {
            return defaultWindowsEndpoint();
        } else {
            return defaultAddress() + ":" + defaultPort();
        }
    }

    public static String endpointFromEnv() {
        return firstNonNull(System.getenv("DOCKER_HOST"),
                defaultDockerEndpoint());
    }

    public static String defaultUnixEndpoint() {
        return DEFAULT_UNIX_ENDPOINT;
    }

    public static String defaultWindowsEndpoint() {
        return DEFAULT_WINDOWS_ENDPOINT;
    }

    public static String defaultAddress() {
        return DEFAULT_ADDRESS;
    }

    public static int defaultPort() {
        return DEFAULT_PORT;
    }

    public static int portFromEnv() {
        final String port = System.getenv("DOCKER_PORT");
        if (port == null) {
            return defaultPort();
        }
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return defaultPort();
        }
    }

    public static String defaultCertPath() {
        final String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".docker").toString();
    }

    public static String certPathFromEnv() {
        return System.getenv("DOCKER_CERT_PATH");
    }

    public static String configPathFromEnv() {
        return System.getenv("DOCKER_CONFIG");
    }

}
