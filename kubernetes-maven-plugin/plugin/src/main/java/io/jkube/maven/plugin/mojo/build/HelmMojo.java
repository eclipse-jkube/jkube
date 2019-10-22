package io.jkube.maven.plugin.mojo.build;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import io.jkube.kit.common.ResourceFileType;
import io.jkube.kit.common.util.FileUtil;
import io.jkube.kit.common.util.KubernetesHelper;
import io.jkube.kit.common.util.MavenUtil;
import io.jkube.kit.common.util.ResourceUtil;
import io.jkube.kit.config.resource.HelmConfig;
import io.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.lang.System.getProperty;

/**
 * Generates a Helm chart for the kubernetes resources
 */
@Mojo(name = "helm", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class HelmMojo extends AbstractJkubeMojo {
    protected static final String FAILED_TO_LOAD_KUBERNETES_YAML = "Failed to load kubernetes YAML ";
    protected static final String YAML_EXTENSION = ".yaml";

    @Parameter
    private HelmConfig helm;

    /**
     * The generated kubernetes YAML file
     */
    @Parameter(property = "jkube.kubernetesManifest", defaultValue = "${basedir}/target/classes/META-INF/jkube/kubernetes.yml")
    private File kubernetesManifest;

    /**
     * The generated kubernetes YAML file
     */
    @Parameter(property = "jkube.kubernetesTemplate", defaultValue = "${basedir}/target/classes/META-INF/jkube/k8s-template.yml")
    private File kubernetesTemplate;

    @Component
    private MavenProjectHelper projectHelper;

    @Component(role = Archiver.class, hint = "tar")
    private TarArchiver archiver;

    @Override
    public void executeInternal() throws MojoExecutionException {
        try {
            String chartName = getChartName();

            for (HelmConfig.HelmType type : getHelmTypes()) {
                generateHelmChartDirectory(chartName, type);
            }
        } catch (IOException exception) {
            throw new MojoExecutionException(exception.getMessage());
        }
    }

    protected void generateHelmChartDirectory(String chartName, HelmConfig.HelmType type) throws IOException, MojoExecutionException {
        File outputDir = prepareOutputDir(type);
        File sourceDir = checkSourceDir(chartName, type);
        if (sourceDir == null) {
            return;
        }
        log.info("Creating Helm Chart \"%s\" for %s", chartName, type.getDescription());
        log.verbose("SourceDir: %s", sourceDir);
        log.verbose("OutputDir: %s", outputDir);

        // Copy over all resource descriptors into the helm templates dir
        File templatesDir = copyResourceFilesToTemplatesDir(outputDir, sourceDir);

        // Save Helm chart
        createChartYaml(chartName, outputDir);

        // Copy over support files
        copyTextFile(outputDir, "README");
        copyTextFile(outputDir, "LICENSE");


        Template template = findTemplate();
        if (template != null) {
            createTemplateParameters(outputDir, template, templatesDir);
        }
        // now lets create the tarball
        File destinationFile = new File(project.getBuild().getDirectory(),
                                        chartName + "-" + project.getVersion() + "-" + type.getClassifier() + "." + getChartFileExtension());
        MavenUtil.createArchive(outputDir.getParentFile(), destinationFile, this.archiver);
        projectHelper.attachArtifact(project, getChartFileExtension(), type.getClassifier(), destinationFile);
    }

    private String getChartName() {
        String ret = getProperty("jkube.helm.chart");
        if (ret != null) {
            return ret;
        }
        if (helm != null) {
            ret = helm.getChart();
        }
        return ret != null ? ret : project.getArtifactId();
    }

    private String getChartFileExtension() {
        String ret = getProperty("jkube.helm.chartExtension");
        if (ret != null) {
            return ret;
        }
        if (helm != null) {
            ret = helm.getChartExtension();
        }
        return ret != null ? ret : "tar.gz";
    }

    private File prepareOutputDir(HelmConfig.HelmType type) throws MojoExecutionException {
        String dir = getProperty("jkube.helm.outputDir");
        if (dir == null) {
            dir = String.format("%s/jkube/helm/%s/%s",
                                project.getBuild().getDirectory(),
                                type.getOutputDir(),
                                getChartName());
        }
        File dirF = new File(dir);
        try {
            if (dirF.isFile()) {
                FileUtils.deleteDirectory(dirF);
            }
            return dirF;
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot delete directory " + dir + ": " + e,e);
        }
    }

    private File checkSourceDir(String chartName, HelmConfig.HelmType type) {
        String dir = getProperty("jkube.helm.sourceDir");
        if (dir == null) {
            dir = project.getBuild().getOutputDirectory() + "/META-INF/jkube/" + type.getSourceDir();
        }
        File dirF = new File(dir);
        if (!dirF.isDirectory() || !dirF.exists()) {
            log.warn("Chart source directory %s does not exist so cannot make chart %s. " +
                     "Probably you need run 'mvn kubernetes:resource' before.", dirF, chartName);
            return null;
        }
        if (!containsYamlFiles(dirF)) {
            log.warn("Chart source directory %s does not contain any YAML manifest to make chart %s. " +
                     "Probably you need run 'mvn kubernetes:resource' before.", dirF, chartName);
            return null;
        }
        return dirF;
    }

    private List<HelmConfig.HelmType> getHelmTypes() {
        String helmTypeProp = getProperty("jkube.helm.type");
        if (StringUtils.isNotBlank(helmTypeProp)) {
            String[] propTypes = StringUtils.split(helmTypeProp, ",");
            List<HelmConfig.HelmType> ret = new ArrayList<>();
            for (String prop : propTypes) {
                ret.add(HelmConfig.HelmType.valueOf(prop.trim().toLowerCase()));
            }
            return ret;
        }
        if (helm != null) {
            List<HelmConfig.HelmType> types = helm.getType();
            if (types != null && types.size() > 0) {
                return types;
            }
        }
        return Arrays.asList(HelmConfig.HelmType.kubernetes);
    }

    private void createChartYaml(String chartName, File outputDir) throws MojoExecutionException {
        Chart chart = helm != null ?
            new Chart(chartName, project, helm.getKeywords(), helm.getEngine()) :
            new Chart(chartName, project);

        String iconUrl = findIconURL();
        getLog().debug("Found icon: " + iconUrl);
        if (StringUtils.isNotBlank(iconUrl)) {
            chart.setIcon(iconUrl);
        }
        File outputChartFile = new File(outputDir, "Chart.yaml");
        try {
            ResourceUtil.save(outputChartFile, chart, ResourceFileType.yaml);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save chart " + outputChartFile + ": " + e, e);
        }
    }


    private void createTemplateParameters(File outputDir, Template template, File templatesDir) throws MojoExecutionException {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode values = nodeFactory.objectNode();
        List<io.fabric8.openshift.api.model.Parameter> parameters = template.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        List<HelmParameter> helmParameters = new ArrayList<>();
        for (io.fabric8.openshift.api.model.Parameter parameter : parameters) {
            HelmParameter helmParameter = new HelmParameter(parameter);
            helmParameter.addToValue(values);
            helmParameters.add(helmParameter);
        }
        File outputChartFile = new File(outputDir, "values.yaml");
        try {
            ResourceUtil.save(outputChartFile, values, ResourceFileType.yaml);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save chart values " + outputChartFile + ": " + e, e);
        }

        // now lets replace all the parameter expressions in each template
        File[] files = templatesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String extension = Files.getFileExtension(file.getName()).toLowerCase();
                    if (extension.equals("yaml") || extension.equals("yml")) {
                        convertTemplateParameterExpressionsWithHelmExpressions(file, helmParameters);
                    }
                }
            }
        }
    }

    private void convertTemplateParameterExpressionsWithHelmExpressions(File file, List<HelmParameter> helmParameters) throws MojoExecutionException {
        String text = null;
        try {
            text = FileUtils.readFileToString(file, Charset.defaultCharset());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load " + file + " so we can replacing template expressions " +e, e);
        }
        String original = text;
        for (HelmParameter helmParameter : helmParameters) {
            text = helmParameter.convertTemplateParameterToHelmExpression(text);
        }
        if (!original.equals(text)) {
            try {
                FileUtils.writeStringToFile(file, text, Charset.defaultCharset());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to save " + file + " after replacing template expressions " +e, e);
            }
        }
    }

    private String findIconURL() throws MojoExecutionException {
        String answer = null;
        if (kubernetesManifest != null && kubernetesManifest.isFile()) {
            Object dto = null;
            try {
                dto = ResourceUtil.load(kubernetesManifest, KubernetesResource.class);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to load kubernetes YAML " + kubernetesManifest + ". " + e, e);
            }
            if (dto instanceof HasMetadata) {
                answer = KubernetesHelper.getOrCreateAnnotations((HasMetadata) dto).get("jkube.io/iconUrl");
            }
            if (StringUtils.isBlank(answer) && dto instanceof KubernetesList) {
                KubernetesList list = (KubernetesList) dto;
                List<HasMetadata> items = list.getItems();
                if (items != null) {
                    for (HasMetadata item : items) {
                        answer = KubernetesHelper.getOrCreateAnnotations(item).get("jkube.io/iconUrl");
                        if (StringUtils.isNotBlank(answer)) {
                            break;
                        }
                    }
                }
            }
        } else {
            getLog().warn("No kubernetes manifest file has been generated yet by the kubernetes:resource goal at: " + kubernetesManifest);
        }
        return answer;
    }

    private Template findTemplate() throws MojoExecutionException {
        if (kubernetesTemplate != null && kubernetesTemplate.isFile()) {
            Object dto = null;
            try {
                dto = ResourceUtil.load(kubernetesTemplate, KubernetesResource.class,ResourceFileType.yaml);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to load kubernetes YAML " + kubernetesTemplate + ". " + e, e);
            }
            if (dto instanceof Template) {
                return (Template) dto;
            }
            if (dto instanceof KubernetesList) {
                KubernetesList list = (KubernetesList) dto;
                List<HasMetadata> items = list.getItems();
                if (items != null) {
                    for (HasMetadata item : items) {
                        if (item instanceof Template) {
                            return (Template) item;
                        }
                    }
                }
            }
        }
        return null;
    }

    private File copyResourceFilesToTemplatesDir(File outputDir, File sourceDir) throws MojoExecutionException {
        File templatesDir = new File(outputDir, "templates");
        templatesDir.mkdirs();
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                Object dto;
                try {
                    dto = ResourceUtil.load(file, KubernetesResource.class, ResourceFileType.yaml);
                } catch (IOException e) {
                    throw new MojoExecutionException(FAILED_TO_LOAD_KUBERNETES_YAML + file + ". " + e, e);
                }
                if (dto instanceof Template) {
                    // lets split the template into separate files!
                    Template template = (Template) dto;
                    copyTemplateResourcesToTemplatesDir(templatesDir, template);
                    continue;
                }

                String name = file.getName();
                if (name.endsWith(".yml")) {
                    name = FileUtil.stripPostfix(name, ".yml") + YAML_EXTENSION;
                }
                File targetFile = new File(templatesDir, name);
                try {
                    // lets escape any {{ or }} characters to avoid creating invalid templates
                    String text = FileUtils.readFileToString(file, Charset.defaultCharset());
                    text = escapeYamlTemplate(text);
                    FileUtils.write(targetFile, text, Charset.defaultCharset());
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to copy manifest files from " + file +
                            " to " + targetFile + ": " + e, e);
                }
            }
        }
        return templatesDir;
    }

    private void copyTemplateResourcesToTemplatesDir(File templatesDir, Template template) throws MojoExecutionException {
        List<HasMetadata> objects = template.getObjects();
        if (objects != null) {
            for (HasMetadata object : objects) {
                String name = KubernetesResourceUtil.getNameWithSuffix(KubernetesHelper.getName(object), KubernetesHelper.getKind(object)) + ".yaml";
                File outFile = new File(templatesDir, name);
                try {
                    ResourceUtil.save(outFile, object);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to save template " + outFile + ": " + e, e);
                }
            }
        }
    }

    public static String escapeYamlTemplate(String template) {
        StringBuffer answer = new StringBuffer();
        int count = 0;
        char last = 0;
        for (int i = 0, size = template.length(); i < size; i++) {
            char ch = template.charAt(i);
            if (ch == '{' || ch == '}') {
                if (count == 0) {
                    last = ch;
                    count = 1;
                } else {
                    if (ch == last) {
                        answer.append( ch == '{' ? "{{\"{{\"}}" : "{{\"}}\"}}");
                    } else {
                        answer.append(last);
                        answer.append(ch);
                    }
                    count = 0;
                    last = 0;
                }
            } else {
                if (count > 0) {
                    answer.append(last);
                }
                answer.append(ch);
                count = 0;
                last = 0;
            }
        }
        if (count > 0) {
            answer.append(last);
        }
        return answer.toString();
    }

    private boolean containsYamlFiles(File sourceDir) {
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String lower = file.getName().toLowerCase();
                if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void copyTextFile(File outputDir, final String srcFile) throws MojoExecutionException {
        try {
            FilenameFilter filter = (dir, name) -> {
                String lower = name.toLowerCase(Locale.ENGLISH);
                return lower.equals(srcFile.toLowerCase()) || lower.startsWith(srcFile.toLowerCase() + ".");
            };
            copyFirstFile(project.getBasedir(), filter, outputDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save " + srcFile + ": " + e, e);
        }
    }

    protected void copyFirstFile(File sourceDir, FilenameFilter filter, File outDir) throws IOException {
        File[] files = sourceDir.listFiles(filter);
        if (files != null && files.length > 0) {
            File sourceFile = files[0];
            FileUtils.copyFile(sourceFile, new File(outDir, sourceFile.getName()));
        }
        if (files != null && files.length > 1) {
            log.warn("Found %d of %s files. Using first one %s", files.length, files[0].getName(), files[0]);
        }
    }

    // =================================================================================================================
    /**
     * Represents the <a href="https://github.com/kubernetes/helm">Helm</a>
     * <a href="https://github.com/kubernetes/helm/blob/master/pkg/proto/hapi/chart/metadata.pb.go#L50">Chart.yaml file</a>
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Chart {
        @JsonProperty
        private String name;
        @JsonProperty
        private String home;
        @JsonProperty
        private List<String> sources;
        @JsonProperty
        private String version;
        @JsonProperty
        private String description;
        @JsonProperty
        private List<String> keywords;
        @JsonProperty
        private List<Maintainer> maintainers;
        @JsonProperty
        private String engine;
        @JsonProperty
        private String icon;

        public Chart() {
        }

        public Chart(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public Chart(String name, MavenProject project) {
            this(name, project, null, null);
        }

        public Chart(String name, MavenProject project, List<String> keywords, String engine) {
            this.name = name;
            this.keywords = keywords;
            this.engine = engine;

            this.name = name;
            if (project != null) {
                this.version = project.getVersion();
                this.description = project.getDescription();
                this.home = project.getUrl();
                this.keywords = keywords;
                this.engine = engine;

                Scm scm = project.getScm();
                if (scm != null) {
                    String url = scm.getUrl();
                    if (url != null) {
                        List<String> sources1 = new ArrayList<>();
                        sources1.add(url);
                        this.sources = sources1;
                    }
                }
                List<Developer> developers = project.getDevelopers();
                if (developers != null) {
                    List<Maintainer> maintainers1 = new ArrayList<>();
                    for (Developer developer : developers) {
                        String email = developer.getEmail();
                        String devName = developer.getName();
                        if (StringUtils.isNotBlank(devName) || StringUtils.isNotBlank(email)) {
                            Maintainer maintainer = new Maintainer(devName, email);
                            maintainers1.add(maintainer);
                        }
                    }
                    this.maintainers = maintainers1;
                }
            }
        }

        @Override
        public String toString() {
            return "Chart{" +
                    "name='" + name + '\'' +
                    ", home='" + home + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHome() {
            return home;
        }

        public void setHome(String home) {
            this.home = home;
        }

        public List<String> getSources() {
            return sources;
        }

        public void setSources(List<String> sources) {
            this.sources = sources;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public List<Maintainer> getMaintainers() {
            return maintainers;
        }

        public void setMaintainers(List<Maintainer> maintainers) {
            this.maintainers = maintainers;
        }

        public String getEngine() {
            return engine;
        }

        public void setEngine(String engine) {
            this.engine = engine;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        /**
         */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Maintainer {

            @JsonProperty
            private String name;

            @JsonProperty
            private String email;

            public Maintainer() {}

            public Maintainer(String name, String email) {
                this.name = name;
                this.email = email;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }
        }
    }

    public static class HelmParameter {
        private final io.fabric8.openshift.api.model.Parameter parameter;
        private final String helmName;

        public HelmParameter(io.fabric8.openshift.api.model.Parameter parameter) {
            this.parameter = parameter;
            this.helmName = parameter.getName().toLowerCase();
        }

        public void addToValue(ObjectNode values) {
            String value = parameter.getValue();
            if (value != null) {
                values.put(helmName, value);
            }
        }

        public io.fabric8.openshift.api.model.Parameter getParameter() {
            return parameter;
        }

        public String getHelmName() {
            return helmName;
        }

        public String convertTemplateParameterToHelmExpression(String text) {
            String name = parameter.getName();
            String from = "${" + name + "}";
            String answer = text;
            String defaultExpression = "";
            String required = "";
            String value = parameter.getValue();
            if (value != null) {
                defaultExpression = " | default \"" + value + "\"";
            }
            Boolean flag = parameter.getRequired();
            if (flag != null && flag.booleanValue()) {
                required = "required \"A valid .Values." + helmName + " entry required!\" ";
            }
            String to = "{{ " + required + ".Values." + helmName + defaultExpression + " }}";
            answer = answer.replace(from,to);
            from = "$" + name;
            return answer.replace(from, to);
        }

    }
}
