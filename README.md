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
sbt -Dheroku.apiToken=REPLACE_WITH_TOKEN generateHerokuReport
```

This generates a tab-separated text file in the console standard output
that can be copied into a spreadsheet.

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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    