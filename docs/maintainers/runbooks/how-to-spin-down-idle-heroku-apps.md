# How to spin down idle Heroku apps

Spinning down a prototype turns it off by reducing its dyno count to 0. A spun down prototype can be turned on again by
increasing its dyno count to 1. A dyno represents one running instance of your application on Heroku.

We need to periodically spin down unused prototypes because we have a monthly resource quota for the HMRC Heroku account
that we sometimes overrun. Before we turn off a prototype, we give notice via the community-prototype channel on slack,
so users can opt to keep their prototype running.

Spinning down idle apps is a two-step process, normally performed over the course of a week to give teams plenty of
notice before we turn off a prototype.

## Step 1: create and publish a list of the Heroku apps we think are idle and intend to turn off

The heuristic we use to judge if an app is idle is how recently it's been updated and if it's been added to the list of
apps we want to always keep running.

1. Make sure you've got an up-to-date checkout of the `hmrc/init-prototype` repo locally

2. Open your terminal and change directory to your local checkout of the `hmrc/init-prototype` repo

3. Run `sbt generateSpinDownList`

4. Post into #community-prototype channel on slack, attaching the generated spin-down-list.txt to the message

   > Hi all 👋 you might not be aware that we have a monthly resource quota for running prototypes on Heroku. For that
   reason, we periodically turn off prototypes we think might have been left running mistakenly because they haven't
   been updated recently.
   >
   > Turning off a prototype is not destructive, your prototype will not be removed from Heroku, and you can still push
   changes to it while it's turned off.
   >
   > Before we turn anything off, we make an announcement like this to give you a week's notice of the running
   prototypes we've identified may no longer be in use. Comment below with your prototype's name if you would like us to
   leave your prototype running this time. You can also let us know if your prototype should always be left running, and
   in that case, we'll additionally add it to an allow list, so it's not automatically proposed again.
   >
   > If you make an update to your prototype during the week, we'll also leave it running. Then, after we turn off the
   remaining prototypes, we'll post the updated list of which prototypes were effected.
   >
   > If we turn off your prototype, then you can turn it on again yourself by setting its dyno count back to 1 in the
   Heroku control panel. A dyno represents a single running instance of your prototype on Heroku, and we turn prototypes
   off by setting this to 0. Alternatively, you can ask here or in #team-plat-ui and someone will be able to help.
   >
   > The current list of prototypes that we plan to turn off next week at {{time}} on {{date}} is:
   >
   > {{attachment:spin-down-list.txt}}

## Step 2: turn off the remaining Heroku apps and notify prototype community

On the spin down date planned in step 1 (normally a week after the initial announcement):

1. Manually update your local spin-down-list.txt to remove any prototypes that people have requested be kept running

   > **Note**
   > Additionally, add any prototypes people have requested should be kept running all the time to
   the `heroku.appsToKeepRunning` list and create a PR for the change.

2. Run `sbt generateSpinDownList` to update your spin-down-list.txt to additionally remove any prototypes from the list
   which were updated during the week

3. Run `sbt spinDownPrototypes spin-down-list.txt` to turn off the remaining prototypes

4. Post into the previous thread on slack, setting it to "[x] Also send to #community-prototype "

   > The following list of prototypes have now been turned off.
   >
   > If we turned off your prototype, then you can turn it on again yourself by setting its dyno count back to 1 in the
   Heroku control panel. Alternatively, you can ask here or in #team-plat-ui and someone will be able to help
   >
   > {{attachment:spin-down-list.txt}}

5. Make sure that any PR on `hmrc/init-prototype` repo to add new prototypes that should always be kept running is
   merged before you
   complete the spin down









