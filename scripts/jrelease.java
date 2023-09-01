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
///usr/bin/env jbang "$0" "$@" ; exit $?
// //DEPS <dependency1> <dependency2>
//DEPS org.jline:jline:3.16.0
//DEPS de.codeshelf.consoleui:consoleui:0.0.13
//DEPS org.fusesource.jansi:jansi:2.3.4
//DEPS info.picocli:picocli:4.6.1
//DEPS org.apache.maven:maven-model:3.8.1

import static java.lang.System.out;

import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.InputResult;
import de.codeshelf.consoleui.prompt.ListResult;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.ListPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import jline.TerminalFactory;
import picocli.CommandLine;

public class jrelease {

    public static void main(String... args) throws Exception {

        print("Welcome to @|green JKube release|@ script");

        String currentVersion = retrieveCurrentVersionFromPomFile();
        print("Current version is @|bold,green " + currentVersion + "|@\n");

        // prompt the user for the release version
        String nextReleaseVersion = promptWithSuggestion(
                "Enter the release version to use",
                guessNextReleaseVersion(currentVersion));
        print("Chosen release version is @|bold,green " + nextReleaseVersion + "|@");

        // prompt the user for the next snapshot version
        String nextSnapshotVersion = promptWithSuggestion(
                "Enter the next snapshot version to use",
                guessNextSnapshotVersion(nextReleaseVersion));
        print("Chosen next snapshot version is @|bold,green " + nextSnapshotVersion + "|@");

        // display the git remotes
        print("Git remotes are:");
        executeInteractiveCommand("bash", "-c", "git remote -v");

        // choose the git remote to use as a main repository
        String gitUpstreamRemote = promptWithSuggestion("Enter the git remote to use as the upstream", "origin");
        // choose the git remote to use as a fork repository
        String gitForkRemote = promptWithSuggestion("Enter the git remote to use as a fork", "fork");
        // choose the branch name to use for the release
        String releaseBranchName = promptWithSuggestion("Enter the branch name to use for the release", "release/"
                + nextReleaseVersion);

        // 1. Make sure the branch to release is up to date on your fork (master by
        // default)
        printStep(1, "Make sure the branch to release is up to date on your fork (master by default)");
        executeStep("git fetch --all && git checkout master && git reset --hard " + gitUpstreamRemote + "/master");
        executeStep("git checkout -b " + releaseBranchName);
        print("Check that the branch is up to date with the remote");
        executeStep("git --no-pager log -n 5 --pretty=oneline --abbrev-commit --graph --decorate");

        // 2. Set release version
        // Make sure project.build.outputTimestamp property gets updated
        printStep(2, "Set release version");
        executeStep("./scripts/release.sh setReleaseVersion " + nextReleaseVersion);

        // 3. Update Quickstarts version
        printStep(3, "Update Quickstarts version");
        executeStep("./scripts/quickstarts.sh version");

        // 4. Replace ### x.y-SNAPSHOT with ### x.y.z (yyyy-MM-dd) in CHANGELOG.md
        printStep(4, "Replace ### x.x-SNAPSHOT with ### x.y.z (202x-xx-xx) in CHANGELOG.md");
        String changelogNewReleaseHeader = "### " + nextReleaseVersion + " ("
                + new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + ")";
        executeStep("sed -i 's/### " + currentVersion + "/" + changelogNewReleaseHeader + "/g' CHANGELOG.md");

        // 5. Update bug template .github/ISSUE_TEMPLATE/bug_report.yml to add versions:
        printStep(5, "Update bug template .github/ISSUE_TEMPLATE/bug_report.yml to add versions");
        executeStep("sed -E '/(- )\"SNAPSHOT\".*/{p;s//\\1\""
                + nextReleaseVersion
                + "\"/}' -i .github/ISSUE_TEMPLATE/bug_report.yml");

        // 6. Send a message to Gitter channel `Starting release process for vx.y.z`
        printStep(6, "Send a message to Gitter channel `Starting release process for v" + nextReleaseVersion + "`");
        print("Press enter to continue");
        System.in.read();

        // 7. Git add and Commit ‘[RELEASE] Updated project version to x.y.z’
        printStep(7, "Git add and Commit ‘[RELEASE] Updated project version to " + nextReleaseVersion + "’");
        executeStep("git add . && git commit -m \"[RELEASE] Updated project version to " + nextReleaseVersion + "\"");

        // 8. Set snapshot version
        printStep(8, "Set snapshot version");
        executeStep("./scripts/release.sh setReleaseVersion " + nextSnapshotVersion);

        // 9. Add ### x.y+1-SNAPSHOT line to CHANGELOG.md
        printStep(9, "Add ### " + nextSnapshotVersion + " line to CHANGELOG.md");
        executeStep("sed -E '/" + escape(changelogNewReleaseHeader) + "/{s//### " + nextSnapshotVersion
                + "\\n\\n\\0/}' -i CHANGELOG.md");

        // 10. Git add and commit ‘[RELEASE] Prepare for next development iteration’
        printStep(10, "Git add and commit ‘[RELEASE] Prepare for next development iteration’");
        executeStep("git add . && git commit -m \"[RELEASE] Prepare for next development iteration\"");

        // 11. Create Pull Request
        printStep(11, "Create Pull Request");
        executeStep("git push " + gitForkRemote + " " + releaseBranchName);
        print("Create the PR in github using the branch " + releaseBranchName + " in the " + gitForkRemote
                + " repository");
        print("Press enter to continue");
        System.in.read();

        // 12. After rebase and merge create tag for release in first commit
        printStep(12, "After rebase and merge create tag for release in first commit");
        executeStep("git fetch --all && git checkout master && git reset --hard " + gitUpstreamRemote
                + "/master && git --no-pager log -n 5 --pretty=oneline --abbrev-commit --graph --decorate");
        String gitRevToTag = promptWithSuggestion(
                "Enter the git rev of the commit [RELEASE] Updated project version to " + nextReleaseVersion,
                "xxxxxxx");
        executeStep("git tag -a v" + nextReleaseVersion + " " + gitRevToTag + " -m \"" + nextReleaseVersion + " ("
                + new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) + ")\"");
        executeStep("git push " + gitUpstreamRemote + " v" + nextReleaseVersion);

        // 13. https://ci.eclipse.org/jkube
        printStep(13, "https://ci.eclipse.org/jkube");
        print("- Run build https://ci.eclipse.org/jkube/job/ReleasePipelines/job/simplified-maven-central-release/ with tag v"
                + nextReleaseVersion + " as RELEASE_TAG build parameter");
        print("Press enter to continue");
        System.in.read();
        print("- Run https://ci.eclipse.org/jkube/job/ReleasePipelines/job/simplified-downloads-eclipse-release/ with tag v"
                + nextReleaseVersion + " as RELEASE_TAG build parameter");
        print("Press enter to continue");
        System.in.read();

        // 14. Wait for maven central
        printStep(14, "Wait for maven central, check");
        print("- Check https://download.eclipse.org/jkube/");
        print("- Check https://repo1.maven.org/maven2/org/eclipse/jkube/kubernetes-maven-plugin-doc/");
        print("Press enter to continue");
        System.in.read();

        // 15. Create GitHub release
        printStep(15, "Create GitHub release from previously pushed tag to upstream, enable `discussions`");
        print("Add changelog this to the release description:");
        executeInteractiveCommand("bash", "-c", "./scripts/changelog.sh extract " + nextReleaseVersion);

        // 16. Tag new release in https://github.com/jkubeio/jkube-website
        printStep(16, "Tag new release in https://github.com/jkubeio/jkube-website (keep empty title/description)");
        print("Press enter to continue");
        System.in.read();

        // 17. Publish tweet in jkubeio profile
        printStep(17, "Publish tweet in jkubeio profile");
        print("🔥 @ECDTools #JKube new release 🚀");
        print("📦 v" + nextReleaseVersion);
        print("✨ new feat");
        print("✨ another great feature");
        print("✨ some other improvements");
        print("🐞 Many bug fixes & improvements");
        print("");
        print("📢 Please help us spread the word & share your experience @jkubeio");
        print("");
        print("https://github.com/eclipse/jkube/releases/tag/v" + nextReleaseVersion);
        print("");
        print("Press enter to continue");
        System.in.read();

        // 18. Announce the release in Gitter
        printStep(18, "Announce the release in Gitter");
        print("🔥 Eclipse JKube new release 🚀");
        print("📦 v" + nextReleaseVersion);
        print("✨ new feat");
        print("✨ another great feature");
        print("✨ some other improvements");
        print("🐞 Many bug fixes & improvements");
        print("");
        print("📢 Please help us spread the word & share your experience https://twitter.com/jkubeio");
        print("");
        print("https://github.com/eclipse/jkube/releases/tag/v" + nextReleaseVersion);
        print("");
        print("Press enter to continue");
        System.in.read();

        // 19. Publish release
        printStep(19, "Publish release in https://projects.eclipse.org/projects/ecd.jkube");
        print("Press enter to continue");
        System.in.read();

        // 20. Run the Gradle verification test
        printStep(20,
                "Run the Gradle verification test: https://github.com/jkubeio/jkube-integration-tests/actions/workflows/smoke-tests.yml");
        print("Press enter to continue");
        System.in.read();

    }

    private static String retrieveCurrentVersionFromPomFile() throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        return model.getVersion();
    }

    private static void print(String cmd) {
        out.println(text(cmd));
    }

    private static String text(String txt) {
        return CommandLine.Help.Ansi.AUTO.text(txt).toString();
    }

    private static String guessNextSnapshotVersion(String nextReleaseVersion) {
        String nextSnapshotVersion = "";
        String[] nextReleaseVersionParts = nextReleaseVersion.split("\\.");
        if (nextReleaseVersionParts.length == 3) {
            int minorVersion = Integer.parseInt(nextReleaseVersionParts[1]);
            nextSnapshotVersion = nextReleaseVersionParts[0] + "." + (minorVersion + 1) + "-SNAPSHOT";
        } else {
            String[] minorVersionParts = nextReleaseVersionParts[1].split("-");
            int minorVersion = Integer.parseInt(minorVersionParts[0]);
            nextSnapshotVersion = nextReleaseVersionParts[0] + "." + (minorVersion + 1) + "-SNAPSHOT";
        }
        return nextSnapshotVersion;
    }

    private static String promptWithSuggestion(String message, String defaultValue) throws IOException {
        try {
            ConsolePrompt prompt = new ConsolePrompt();
            PromptBuilder promptBuilder = prompt.getPromptBuilder();
            promptBuilder.createInputPrompt().name("name").message(text(message)).defaultValue(defaultValue)
                    .addPrompt();
            // create a PromptBuilder with editable suggestion

            HashMap<String, ? extends PromtResultItemIF> result = prompt.prompt(promptBuilder.build());
            return ((InputResult) result.get("name")).getInput();
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String guessNextReleaseVersion(String currentVersion) {
        String nextReleaseVersion = "";
        String[] currentVersionParts = currentVersion.split("\\.");
        if (currentVersionParts.length == 3) {
            String[] patchVersionParts = currentVersionParts[2].split("-");
            int patchVersion = Integer.parseInt(patchVersionParts[0]);
            nextReleaseVersion = currentVersionParts[0] + "." + currentVersionParts[1] + "." + (patchVersion + 1);
        } else {
            String[] minorVersionParts = currentVersionParts[1].split("-");
            int minorVersion = Integer.parseInt(minorVersionParts[0]);
            nextReleaseVersion = currentVersionParts[0] + "." + minorVersion + ".0";
        }
        return nextReleaseVersion;
    }

    private static void printStep(int step, String desc) {
        out.println(text(
                "\n🔵 @|bold,green Step " + step + ". |@ " + desc));
    }

    private static void executeInteractiveCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder p = new ProcessBuilder(command);

        p.redirectInput(Redirect.INHERIT);
        p.redirectOutput(Redirect.INHERIT);
        p.redirectError(Redirect.INHERIT);

        Process process = p.start();
        process.waitFor();
    }

    private static void executeStep(String cmd)
            throws IOException, InterruptedException {
        String selectedId = "";
        try {
            ConsolePrompt prompt = new ConsolePrompt();
            PromptBuilder promptBuilder = prompt.getPromptBuilder();

            ListPromptBuilder chooserBuilder = promptBuilder.createListPrompt()
                    .name("idList")
                    .message(text("Choose an action"));

            chooserBuilder
                    .newItem().name("1").text("Execute command: " + cmd).add();
            chooserBuilder
                    .newItem().name("2").text("Start interactive shell").add();
            chooserBuilder
                    .newItem().name("3").text("Skip").add();
            chooserBuilder.addPrompt();
            HashMap<String, ? extends PromtResultItemIF> result = prompt.prompt(promptBuilder.build());
            selectedId = ((ListResult) result.get("idList")).getSelectedId();
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        switch (selectedId) {
            case "1":
                executeInteractiveCommand("bash", "-c", "set -x; " + cmd);
                break;
            case "2":
                print("Default command is: \n" + cmd);
                print("Type `exit` to continue");
                executeInteractiveCommand("bash", "--norc");
                break;
            case "3":
                print("Skipping");
                break;
            default:
                print("None of the options was selected, skipping");
                break;
        }
    }

    private static String escape(String changelogNewReleaseHeader) {
        return changelogNewReleaseHeader.replace("(", "\\(").replace(")", "\\)");
    }

}
