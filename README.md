# github-api

App provides one HTTP endpoint which shows data about user repositories and their branches from GitHub api.
## Beforehand
### Prerequisite
To run this app you need to have installed and configured `maven 3.9.5` or newer and `java 21 JDK` or newer.
### Authorization and Authentication
This app in its own does not contain any of above, but it is advised to use GitHub Personal Access Token
to authorize requests against GitHub API to avoid rate limiting. You can provide PAT in `application.properties`
file in `gh-access-token` field, as a run parameter `--gh-access-token={PAT}` or environment variable. When PAT is not provided
the app will still run but requests may fail due to GitHub API limiting. You can find info about PAT at GitHub website.
## Installing / Running project
Since this project is just a demo you can run with it with maven, by typing this into terminal while in project folder:
```shell
mvn spring-boot:run
```
To build project into standalone `.jar` file run:
```shell
mvn clean install -DskipTests
```
Before building, you can run tests:
```shell
mvn test
```
When you have `.jar` file ready you can run app with:
```shell
java -jar {jar file}
```
## Features and Usage
App runs on port `8080` and provides one api endpoint: `/api/{username}` which gives info about user repositories
and their branches based on information from GitHub api. App is also well documented at address `http://localhost:8080/swagger-ui.html`.
* In response for request, you receive list of user's repositories that are not forks and contains repository name, 
owners login and list of all repository branches which consists of name of the branch and sha of branch last commit. 
* You *should* specify header "Accept: application/json" as other media types are not supported.
* If user does not exist you will be given 404 error response message in specified format.  
Example usage:
```shell
curl http://localhost:8080/api/{username} --header "Accept:application/json"
```
And expected response is:
```
[
  {
    "name": "string",
    "owner": {
      "login": "string"
    },
    "branches": [
      {
        "name": "string",
        "lastCommit": {
          "sha": "string"
        }
      }
    ]
  }
]
```