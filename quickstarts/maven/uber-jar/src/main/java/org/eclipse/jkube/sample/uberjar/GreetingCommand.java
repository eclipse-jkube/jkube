/*
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
package org.eclipse.jkube.sample.uberjar;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Greeting command
 */
@Command(name = "greeting", mixinStandardHelpOptions = true)
public class GreetingCommand implements Runnable {

    @Parameters(paramLabel = "<name>", defaultValue = "picocli",
        description = "Your name.")
    String name;

    @Override
    public void run() {
        System.out.printf("Hello %s, go go commando!\n", name);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GreetingCommand()).execute(args);
        System.exit(exitCode);
    }

}
