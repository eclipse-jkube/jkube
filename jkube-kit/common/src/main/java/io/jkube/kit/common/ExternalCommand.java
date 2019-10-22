/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package io.jkube.kit.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;

/**
 * @author roland
 * @since 14/09/16
 */
public abstract class ExternalCommand {
    protected final KitLogger log;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private int statusCode;

    public ExternalCommand(KitLogger log) {
        this.log = log;
    }

    public void execute() throws IOException {
        execute(null);
    }

    public void execute(String processInput) throws IOException {
        final Process process = startProcess();
        start();
        try {
            inputStreamPump(process.getOutputStream(),processInput);

            Future<IOException> stderrFuture = startStreamPump(process.getErrorStream());
            outputStreamPump(process.getInputStream());

            stopStreamPump(stderrFuture);
            checkProcessExit(process);
        } catch (IOException e) {
            process.destroy();
            throw e;
        } finally {
            end();
        }
        if (statusCode != 0) {
            throw new IOException(String.format("Process '%s' exited with status %d",
                                                getCommandAsString(), statusCode));
        }

    }

    // Hooks for logging ...
    protected void start() {}

    protected void end() {}

    protected int getStatusCode() {
        return statusCode;
    }

    private void checkProcessExit(Process process) {
        try {
            statusCode = process.waitFor();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (IllegalThreadStateException | InterruptedException e) {
            process.destroy();
            statusCode = -1;
        }
    }

    private void inputStreamPump(OutputStream outputStream, String processInput) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            if (processInput != null) {
                writer.write(processInput);
                writer.flush();
            }
        } catch (IOException e) {
            log.info("Failed to close process output stream: %s", e.getMessage());
        }
    }

    private Process startProcess() throws IOException {
        try {
            return Runtime.getRuntime().exec(getArgs());
        } catch (IOException e) {
            throw new IOException(String.format("Failed to start '%s' : %s",
                                                getCommandAsString(),
                                                e.getMessage()), e);
        }
    }

    protected String getCommandAsString() {
        return StringUtils.join(getArgs(), " ");
    }

    protected abstract String[] getArgs();

    private void outputStreamPump(final InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            for (; ; ) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                processLine(line);
            }
        } catch (IOException e) {
            throw new IOException(String.format("Failed to read process '%s' output: %s",
                                                getCommandAsString(),
                                                e.getMessage()), e);
        }
    }

    protected void processLine(String line) {
        log.verbose(line);
    }

    private Future<IOException> startStreamPump(final InputStream errorStream) {
        return executor.submit(new Callable<IOException>() {
            @Override
            public IOException call() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));) {
                    for (; ; ) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        synchronized (log) {
                            log.warn(line);
                        }
                    }
                    return null;
                } catch (IOException e) {
                    return e;
                }
            }
        });
    }

    private void stopStreamPump(Future<IOException> future) throws IOException {
        try {
            IOException e = future.get(2, TimeUnit.SECONDS);
            if (e != null) {
                throw new IOException(String.format("Failed to read process '%s' error stream",
                                                    getCommandAsString()), e);
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException(String.format("Failed to stop process '%s' error stream",
                                                getCommandAsString()), e);
        }
    }
}
