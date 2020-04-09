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
package org.eclipse.jkube.kit.config.service.openshift;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.ImageStreamSpec;
import io.fabric8.openshift.api.model.ImageStreamStatus;
import io.fabric8.openshift.api.model.NamedTagEventList;
import io.fabric8.openshift.api.model.TagEvent;
import io.fabric8.openshift.api.model.TagReference;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handler object for managing image streams
 *
 * @author roland
 * @since 16/01/17
 */
public class ImageStreamService {


    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final int IMAGE_STREAM_TAG_RETRIES = 15;
    private static final long IMAGE_STREAM_TAG_RETRY_TIMEOUT_IN_MILLIS = 1000;

    private final OpenShiftClient client;
    private final KitLogger log;

    public ImageStreamService(OpenShiftClient client, KitLogger log) {
        this.client = client;
        this.log = log;
    }

    /**
     * Save the images stream to a file
     * @param imageName name of the image for which the stream should be extracted
     * @param target file to store the image stream
     */
    public void appendImageStreamResource(ImageName imageName, File target) throws IOException {
        String tag = StringUtils.isBlank(imageName.getTag()) ? "latest" : imageName.getTag();
        try {
            ImageStream is = new ImageStreamBuilder()
                    .withNewMetadata()
                    .withName(imageName.getSimpleName())
                    .endMetadata()

                    .withNewSpec()
                    .addNewTag()
                      .withName(tag)
                      .withNewFrom().withKind("ImageStreamImage").endFrom()
                    .endTag()
                    .endSpec()

                    .build();
            createOrUpdateImageStreamTag(client, imageName, is);
            appendImageStreamToFile(is, target);
            log.info("ImageStream %s written to %s", imageName.getSimpleName(), target);
        } catch (KubernetesClientException e) {
            KubernetesHelper.handleKubernetesClientException(e, this.log);
        } catch (IOException e) {
            throw new IOException(String.format("Cannot write ImageStream descriptor for %s to %s : %s",
                                                           imageName.getFullName(), target.getAbsoluteFile(), e.getMessage()),e);
        }
    }

    private void appendImageStreamToFile(ImageStream is, File target) throws IOException {

        Map<String, ImageStream> imageStreams = readAlreadyExtractedImageStreams(target);
        // Override with given image stream
        imageStreams.put(is.getMetadata().getName(),is);
        KubernetesList isList =
            new KubernetesListBuilder().withItems(new ArrayList<HasMetadata>(imageStreams.values())).build();
        ResourceUtil.save(target, isList);
    }

    private Map<String, ImageStream> readAlreadyExtractedImageStreams(File target) throws IOException {
        // If it already exists, read in the file and use it for update
        Map<String, ImageStream> imageStreams = new HashMap<>();
        if (target.length() > 0) {
            for (HasMetadata entity : KubernetesHelper.loadResources(target)) {
                if ("ImageStream".equals(KubernetesHelper.getKind(entity))) {
                    imageStreams.put(entity.getMetadata().getName(), (ImageStream) entity);
                }
                // Ignore all other kind of entities. There shouldn't be any included anyway
            }
        }
        return imageStreams;
    }

    private void createOrUpdateImageStreamTag(OpenShiftClient client, ImageName image, ImageStream is) {
        String namespace = client.getNamespace();
        String tagSha = findTagSha(client, image.getSimpleName(), client.getNamespace());
        String name = image.getSimpleName() + "@" + tagSha;

        TagReference tag = extractTag(is);
        ObjectReference from = extractFrom(tag);

        if (!Objects.equals(image.getTag(), tag.getName())) {
            tag.setName(image.getTag());
        }
        if (!Objects.equals("ImageStreamImage", from.getKind())) {
            from.setKind("ImageStreamImage");
        }
        if (!Objects.equals(namespace, from.getNamespace())) {
            from.setNamespace(namespace);
        }
        if (!Objects.equals(name, from.getName())) {
            from.setName(name);
        }
    }

    private ObjectReference extractFrom(TagReference tag) {
        ObjectReference from = tag.getFrom();
        if (from == null) {
            from = new ObjectReference();
            tag.setFrom(from);
        }
        return from;
    }

    private TagReference extractTag(ImageStream is) {
        ImageStreamSpec spec = is.getSpec();
        if (spec == null) {
            spec = new ImageStreamSpec();
            is.setSpec(spec);
        }
        List<TagReference> tags = spec.getTags();
        if (tags == null) {
            tags = new ArrayList<>();
            spec.setTags(tags);
        }
        TagReference tag = null;
        if (tags.isEmpty()) {
            tag = new TagReference();
            tags.add(tag);
        } else {
            tag = tags.get(tags.size() - 1);
        }
        return tag;
    }

    private String findTagSha(OpenShiftClient client, String imageStreamName, String namespace) {
        ImageStream currentImageStream = null;

        for (int i = 0; i < IMAGE_STREAM_TAG_RETRIES; i++) {
            if (i > 0) {
                log.info("Retrying to find tag on ImageStream %s", imageStreamName);
                try {
                    Thread.sleep(IMAGE_STREAM_TAG_RETRY_TIMEOUT_IN_MILLIS);
                } catch (InterruptedException e) {
                    log.debug("interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
            currentImageStream = client.imageStreams().withName(imageStreamName).get();
            if (currentImageStream == null) {
                continue;
            }
            ImageStreamStatus status = currentImageStream.getStatus();
            if (status == null) {
                continue;
            }
            List<NamedTagEventList> tags = status.getTags();
            if (tags == null || tags.isEmpty()) {
                continue;
            }

            // Iterate all imagestream tags and get the latest one by 'created' attribute
            TagEvent latestTag = null;

            TAG_EVENT_LIST:
            for (NamedTagEventList list : tags) {
                List<TagEvent> items = list.getItems();
                if (items == null || items.isEmpty()) {
                    continue TAG_EVENT_LIST;
                }

                for (TagEvent tag : items) {
                    latestTag = latestTag == null ? tag : newerTag(tag, latestTag);
                }
            }

            if (latestTag != null && StringUtils.isNotBlank(latestTag.getImage())) {
                String image = latestTag.getImage();
                log.info("Found tag on ImageStream " + imageStreamName + " tag: " + image);
                return image;
            }
        }

        // No image found, even after several retries:
        if (currentImageStream == null) {
            throw new IllegalStateException("Could not find a current ImageStream with name " + imageStreamName + " in namespace " + namespace);
        } else {
            throw new IllegalStateException("Could not find a tag in the ImageStream " + imageStreamName);
        }
    }

    public TagEvent newerTag(TagEvent tag1, TagEvent tag2) {
        Date tag1Date = extractDate(tag1);
        Date tag2Date = extractDate(tag2);

        if(tag1Date == null) {
            return tag2;
        }

        if(tag2Date == null) {
            return tag1;
        }

        return tag1Date.compareTo(tag2Date) > 0 ? tag1 : tag2;
    }

    private Date extractDate(TagEvent tag) {
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(tag.getCreated());
        } catch (ParseException e) {
            log.error("parsing date error : " + e.getMessage(), e);
            return null;
        } catch (NullPointerException e) {
            log.error("tag date is null : " + e.getMessage(), e);
            return null;
        }

    }
}
