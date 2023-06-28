#!groovy

// Use recommended configuration, run all tests to completion (don't fail fast)
buildPlugin(
  useContainerAgent: true,
  failFast: false,          
  configurations: [
    [platform: 'linux', jdk: 8],
    [platform: 'windows', jdk: 8],
])
