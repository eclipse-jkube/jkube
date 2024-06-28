    @Test
    @DisplayName("should work with KubernetesClient config provided by KubernetesMockServer")
    void exportKubernetesClientConfigToFile_worksWithKubernetesMockServer(@TempDir Path temporaryFolder) throws IOException {
        // When
        final Path result = KubernetesMockServerUtil.exportKubernetesClientConfigToFile(mockServer, temporaryFolder.resolve("config"));
        // Then
        final io.fabric8.kubernetes.api.model.Config kc = Serialization
            .unmarshal(result.toFile(), io.fabric8.kubernetes.api.model.Config.class);
        assertThat(kc)
            .hasFieldOrPropertyWithValue("currentContext", "mock-server")
            .satisfies(c -> assertThat(c.getContexts())
                .singleElement(InstanceOfAssertFactories.type(NamedContext.class))
                .hasFieldOrPropertyWithValue("name", "mock-server")
                .hasFieldOrPropertyWithValue("context.namespace", "test")
                .hasFieldOrPropertyWithValue("context.user", "mock-server-user")
                .extracting("context.cluster").asString()
                .isEqualTo(mockServer.getHostName() + ":" + mockServer.getPort()) // Use actual values
            )
            .satisfies(c -> assertThat(c.getClusters())
                .singleElement(InstanceOfAssertFactories.type(NamedCluster.class))
                .hasFieldOrPropertyWithValue("cluster.insecureSkipTlsVerify", true)
                .extracting("cluster.server", "name")
                .allMatch(s -> s.toString().equals(mockServer.getHostName() + ":" + mockServer.getPort())) // Use actual values
            )
            .satisfies(c -> assertThat(c.getUsers())
                .singleElement(InstanceOfAssertFactories.type(NamedAuthInfo.class))
                .hasFieldOrPropertyWithValue("name", "mock-server-user"));
    }