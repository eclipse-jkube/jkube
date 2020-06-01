# Where did Eclipse JKube come from?

This project was earlier called [Fabric8 Maven Plugin](https://github.com/fabric8io/fabric8-maven-plugin) which was an essential component of [Fabric8](http://fabric8.io/) project which was an end to end development platform for building, testing and deploying your applications using it's pipelines. However, project was not successful and it was finally archived. However, there were still some projects inside that org which were still relevant and made sense in as general purpose tool/library. We have provided more background in this [RedHat Developer blogpost](https://developers.redhat.com/blog/2020/01/28/introduction-to-eclipse-jkube-java-tooling-for-kubernetes-and-red-hat-openshift/#Background)

The only difference between Fabric8 Maven Plugin and Eclipse JKube is separation of concerns. Fabric8 Maven Plugin used to create resource manifests for both Kubernetes and OpenShift clusters but Eclipse JKube has two different plugins for this task. We have also refactored core Enricher and Generator apis into JKube Kit.

## Appreciating Fabric8 Maven Plugin Authors and Contributors

We as the maintaining team of Fabric8 Maven Plugin felt the need to refactor/rebrand it as a general purpose maven plugin so that any Java developer can use it for his/her Kubernetes/OpenShift workloads. But it must be noted that this project is a result of the hard work being put by Fabric8 Maven Plugin authors and contributors. You can find full list of contributors [here](https://github.com/fabric8io/fabric8-maven-plugin/graphs/contributors). Here is a list of top 20 contributors:

1. Roland Huß ([@rhuss](https://github.com/rhuss)) - Main Author
2. James Strachan ([@jstrachan](https://github.com/jstrachan)) - Main Author
3. Rohan Kumar ([@rohanKanojia](https://github.com/rohanKanojia)) - Maintainer, Also currently working on Eclipse JKube
4. Nicola Ferraro ([@nicolaferraro](https://github.com/nicolaferraro)) - Contributor
5. Hrishikesh Shinde ([@hrishin](https://github.com/hrishin)) - Maintainer
6. Devang Gaur ([@dev-gaur](https://github.com/dev-gaur)) - Maintainer, Also currently working on Eclipse JKube
7. Piyush Garg ([@piyush-garg](https://github.com/piyush-garg)) - Maintainer
8. Hiram Chirino ([@chirino](https://github.com/chirino)) - Contributor
9. Kamesh Sampath ([@kameshsampath](https://github.com/kameshsampath)) - Contributor
10. Alex Soto ([@lordofthejars](https://github.com/lordofthejars)) - Maintainer
11. Andrea Cosentino ([@oscerd](https://github.com/oscerd)) - Contributor
12. Jimmy Dyson ([@jimmidyson](https://github.com/jimmydyson)) - Contributor
13. Clement Escoffier ([@cescoffier](https://github.com/cescoffier)) - Contributor
14. James Rawlings ([@rawlingsj](https://github.com/rawlingsj)) - Contributor
15. Marc Nuri ([@manusa](https://github.com/manusa)) - Maintainer, Also currently working on Eclipse JKube
16. Yuwei Zhou ([@yuwzho](https://github.com/yuwzho)) - Contributor
17. Matthew Costa ([@ucosty](https://github.com/ucosty)) - Contributor
18. James Netherton ([@jamesnetherton](https://github.com/jamesnetherton)) - Contributor
19. Claus Ibsen ([@davsclaus](https://github.com/davsclaus)) - Contributor
20. Kartik Sapra ([@theexplorist](https://github.com/theexplorist)) - Contributor (as a part of Google Summer of Code 2019)
