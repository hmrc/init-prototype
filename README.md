# init-prototype

> **Note**
> new prototype creation is current pinned at v12.3.0 of the govuk-prototype kit because from v13 creation is now via an npx command rather than copying the repo as a template. Work in progress to support v13 is happening under PLATUI-2102.

This is an SBT library for building Gov.UK Design System prototypes. It is used as part 
of the HMRC CI/CD environment.

Also included in this repository are a collection of SBT build tasks for managing the HMRC
prototyping environment on Heroku.

## Heroku prerequisites

To use the Heroku build tasks, you will need:
* a Heroku account linked to your HMRC email address
* be a member and have admin access to the HMRC team
* have the heroku cli installed locally, or have a Heroku API Key generated from the [Heroku 'Manage Account' settings page](https://dashboard.heroku.com/account)

## Github prerequisites

To run the package-lock report, you'll also need:
* a Github [personal access token](https://github.com/settings/tokens/)

## Running a Heroku usage report

The SBT build task generateHerokuReport can be used to generate a usage report on all prototypes hosted
in Heroku. The report lists the prototypes, their sizes, when they were first created and last deployed.

To run this task, having cloned the repository and changed to the repository root directory,

```shell script
sbt "generateHerokuReport report.txt"
```

This generates a tab-separated plain text file, report.txt, in the repository root directory that can
be imported into a spreadsheet for manipulation.

The usage report, by default, disregards automated releases made by the Heroku API maintenance user. This can be 
changed or additional email addresses added to this exclude list by modifying the heroku.administratorEmails key
in [application.conf](src/main/resources/application.conf)

## Spinning down Heroku prototypes

> **Note**
> With the introduction of the prototype auto-publish functionality we now automatically delete prototypes from heroku after 90 days unless they are listed as a protected prototype in the B&D API. So the current spin down approach is somewhat superseded by that, and in the future we may explore additional automated idling of running prototypes not being actively used. If you want to add a prototype to the list of protected prototypes, ask in #team-plat-ui on slack.

Spinning down a prototype turns it off by reducing its dyno count to 0. A spun down prototype can be turned on again by
increasing its dyno count to 1. A dyno represents one running instance of your application on heroku. 

We need to periodically spin down unused prototypes because we have a monthly resource quota for the hmrc heroku account
that we sometimes overrun. Before we turn off a prototype, we give notice via the community-prototype channel on slack
so users can opt to keep their prototype running.

The sections below document the tasks used as part of the spin down process, if you're performing a spin down you can
find the complete sequence of steps in the
runbook [how to spin down idle heroku apps](./docs/maintainers/runbooks/how-to-spin-down-idle-heroku-apps.md).

### Generate a list of currently running candidates for spinning down

The `generateSpinDownList` sbt task creates a list of prototypes that have not been updated in the last 84 days, and are
not present in the `heroku.appsToKeepRunning` list. It outputs to the text file `spin-down-list.txt`, this file can then
be manually edited to remove an application if requested, and if you re-run the sbt task with an existing spin down list
present then it will automatically update the list to remove prototypes that have been updated since the list was
created or added to the `heroku.appsToKeepRunning` list. It will not add newly inactive prototypes to the list. If you 
want to generate a new list, you will need to remove or empty the existing spin down list. The task will notify you in 
the console when it is performing an update rather than creating a new spin down list.

### Spin down list of heroku apps by name

The `spinDownHerokuApps` sbt task accepts a file with the names of heroku apps you want to spin down. For example:

```text
prototype-one
prototype-two
prototype-three
```

> **Note**
> You can generate a list of candidate heroku applications for spinning down that might be no longer in use by running
> the `generateSpinDownList` sbt
> task, [see the previous section for more information](#generate-a-list-of-currently-running-candidates-for-spinning-down).

```shell script
sbt "spinDownHerokuApps spin-down-list.txt"
```

You should get a report similar to:

```text
Starting Heroku request: /apps/prototype-one/formation/web
Starting Heroku request: /apps/prototype-two/formation/web
Starting Heroku request: /apps/prototype-three/formation/web
Finished Heroku request: /apps/prototype-one/formation/web
Finished Heroku request: /apps/prototype-two/formation/web
Finished Heroku request: /apps/prototype-three/formation/web
[success] Total time: 1 s, completed 24-Jul-2020 16:49:32
```

### Comparing Heroku app repositories with their corresponding HMRC repositories

The script [compare-repositories.sh](bin/compare-repositories.sh) has been designed to systematically compare a set of Heroku repositories
with their corresponding HMRC repositories for the purposes of:
* determining whether all commits are backed-up in the HMRC repository
* assessing whether it is safe to delete the Heroku repository

First create an empty directory,

```shell script
mkdir -p ~/projects/prototypes 
```

Next create a tab-separated-value (TSV) file whose first column contains the name of the heroku App and second column
contains the name of the hmrc repository within the Github HMRC organisation. For example,

```text
herokuApp	hmrcRepository
prototype-1	prototype-1-prototype
prototype-2	prototype-2-prototype
```

Save the file to the directory you just created, for example, `~/projects/prototypes/apps.tsv`

Assuming you have cloned this repository to `~/projects/hmrc/init-prototype`, run the script as follows,

```shell script
cd ~/projects/prototypes && \
~/projects/hmrc/init-prototype/bin/compare-repositories.sh apps.tsv > out.tsv
```

The output saved to `out.tsv` indicates for each Heroku app, whether the HMRC repository is the same
as the Heroku repository with TRUE or FALSE. For example,

```text
herokuApp	hmrcRepository	isSynced
prototype-1	prototype-1-prototype	TRUE
prototype-2	prototype-2-prototype	FALSE
```

## Generate report of deployed repositories with details of GitHub repo, package.json, and package-lock.json

The sbt task `generatePackageLockReport` can produce a report of which Github repos deployed to Heroku contain a 
`package.json` file and a `package-lock.json` file. To do this, the task will:
1. Parse a local copy of `report.txt` generated by `generateHerokuReport` (meaning this task must be run as a prerequisite)
2. For each deployed prototype, check if a Github repo exists with the same name as the Heroku app
3. If not, try to resolve the Github repo name by searching for the commit ids of the 10 earliest Heroku releases of the app
4. If the Github repo is known, check if it contains a package.json and/or package-lock.json
5. Produce a report of deployed Heroku prototypes with the following details:
* Heroku app name
* Github repo name
* If the repo has a package.json
* If the repo has a package-lock.json
* If the prototype is deployed to Heroku
* If the prototype is running in Heroku
* If the Github repo was found

The output will be saved to `package-lock-analysis.tsv`.

```shell script
sbt -Dgithub.apiToken=REPLACE_WITH_GITHUB_PERSONAL_ACCESS_TOKEN "generateHerokuReport report.txt"
sbt generatePackageLockReport
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
