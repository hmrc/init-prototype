# init-prototype

This is an SBT library for building Gov.UK Design System prototypes. It is used as part 
of the HMRC CI/CD environment.

Also included in this repository are a collection of SBT build tasks for managing the HMRC
prototyping environment on Heroku.

## Heroku prerequisites

To use the Heroku build tasks, you will need:
* a Heroku account linked to your HMRC email address
* be a member and have admin access to the HMRC team
* a Heroku API Key generated on the Heroku 'Manage Account' settings page

## Running a Heroku usage report

The SBT build task generateHerokuReport can be used to generate a usage report on all prototypes hosted
in Heroku. The report lists the prototypes, their sizes, when they were first created and last deployed.

To run this task, having cloned the repository and changed to the repository root directory,

```shell script
sbt -Dheroku.apiToken=REPLACE_WITH_TOKEN "generateHerokuReport report.txt"
```

This generates a tab-separated plain text file, report.txt, in the repository root directory that can
be imported into a spreadsheet for manipulation.

## Spinning down Heroku prototypes

To spin down multiple prototypes, create a plain text file, e.g. spin-down-list.txt,
with the name of each prototype on a new line e.g.

```text
prototype-one
prototype-two
prototype-three
```

Save this file into the root directory of this repository. Then run:

```shell script
sbt -Dheroku.apiToken=REPLACE_WITH_TOKEN "spinDownHerokuApps spin-down-list.txt"
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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    