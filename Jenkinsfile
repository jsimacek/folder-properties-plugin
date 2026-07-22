/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
  useContainerAgent: false, // Let the pipeline library configure the requested JDK on VM agents
  configurations: [
    [platform: 'linux', jdk: 25],
    [platform: 'windows', jdk: 21],
])
