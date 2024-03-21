# init-prototype

This is an SBT library for building Gov.UK Design System prototypes. It is used as part 
of the HMRC CI/CD environment.

Also included in this repository are a collection of SBT build tasks for managing the HMRC
prototyping environment on Heroku.

## Heroku prerequisites

To run the main build task, you will need:
* a Heroku account linked to your HMRC email address
* be a member and have admin access to the HMRC team
* have the heroku cli installed locally, or have a Heroku API Key generated from the [Heroku 'Manage Account' settings page](https://dashboard.heroku.com/account)

## Local testing of prototype creation
It is possible to test the creation of prototypes locally, although it does require some prior setup due to that fact
`init-protoype` normally runs as part of a larger `create-a-prototype` job which handles some environment setup.

### Local testing prerequisites
You'll need:
* A cloneable repository for your prototype from Github
* A Github [personal access token](https://github.com/settings/tokens/)

```shell script
sbt "run  --github-username=REPLACE_WITH_GITHUB_USER_NAME --github-token=REPLACE_WITH_GITHUB_PERSONAL_ACCESS_TOKEN 
--target-github-host=github.com --target-git-org=hmrc --target-repo-name=REPLACE_WITH_GITHUB_REPO_NAME"
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
