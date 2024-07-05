# Node.js Pipeline Library

This is a Jenkins shared library for building, testing, creating Docker images, and pushing the images to a registry for Node.js projects. It supports automatic detection of new releases using GitHub tags and allows configuration flexibility for different projects.

## Project Structure
```

nodejs-pipeline-library/
├── vars/
│ └── buildNodeApp.groovy
├── src/
│ └── org/
│ └── example/
│ └── BuildUtils.groovy
└── resources/
└── org/
└── example/
└── config/
└── defaultConfig.yml

````

## Usage

### 1. Setup Jenkins Shared Library

1. Go to Jenkins > Manage Jenkins > Configure System.
2. Under **Global Pipeline Libraries**, add a new library:
   - **Name**: `nodejs-pipeline-library`
   - **Default Version**: `master` (or the branch you prefer)
   - **Retrieval Method**: Modern SCM
   - **Source Code Management**: Select the appropriate SCM and provide the repository URL

### 2. Create a Jenkinsfile in Your Project

In each project that will use the shared library, create a `Jenkinsfile` with the following content:

```groovy
@Library('nodejs-pipeline-library') _

buildNodeApp(
    repoUrl: 'https://github.com/my-org/my-node-app.git',
    branch: 'develop',
    dockerImageName: 'my-node-app'
)
````

### 3. Customize Default Configuration

The default configuration is defined in `resources/org/example/config/defaultConfig.yml`. You can override these defaults by providing a custom configuration in the `Jenkinsfile`.

Example `defaultConfig.yml`:

```yaml
repoUrl: "https://github.com/example/repo.git"
branch: "main"
dockerCredentialsId: "docker-credentials"
dockerRegistryUrl: "https://registry.example.com"
dockerImageName: "example-app"
```

### 4. Set Up GitHub Webhooks for Release Tags

To automatically build and push images on new releases, set up a webhook in your GitHub repository:

1. Go to your GitHub repository settings > Webhooks.
2. Add a new webhook with the following settings:
   - **Payload URL**: `http://your-jenkins-server/github-webhook/`
   - **Content type**: `application/json`
   - **Events**: `Release`

## Pipeline Stages

The shared library defines the following stages in the pipeline:

1. **Checkout**: Clones the repository.
2. **Build**: Installs dependencies and builds the application.
3. **Test**: Runs tests.
4. **Docker Build**: Builds the Docker image.
5. **Docker Push**: Pushes the Docker image to the registry.
6. **Release**: Tags and pushes the release image if a Git tag is detected.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.