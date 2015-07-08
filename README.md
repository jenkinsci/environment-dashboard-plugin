Jenkins Environment Dashboard Plugin
=========================

This Jenkins plugin creates a custom view which can be used as a dashboard to display what code release versions have been deployed to what test and production environments (or devices).

![JobConfig](https://github.com/vipinsthename/environment-dashboard/raw/master/img/config.png)

![Configuration](https://github.com/vipinsthename/environment-dashboard/raw/master/img/dashboard_config.png)

![Dashboard](https://github.com/vipinsthename/environment-dashboard/raw/master/img/dashboard.png)

Click on the Environment name to get a historical view of the deployments.

![Dashboard history](https://github.com/vipinsthename/environment-dashboard/raw/master/img/dashboard_history.png)

## Example set up ##

You have 3 software components:
* UI
* Web Application and API
* Database

Each component has a corresponding Jenkins "build and package" job that creates new environment agnostic build packages and puts the code into something like Nexus waiting for someone to pick it up and deploy it.

You have 4 environments

* CI Test
* Performance Test
* Pre-production
* Production

You have Jenkins jobs for deploying build packages to your environments.

You find it hard to track which version of each application is currently deployed to.

You configure your deployment jobs to publish a record of each deployment to this plugin.

You create a view which displays a matrix with rows for your 3 applications, columns for your environments and the intersections with the deployed code version.

Other information:
* Bug Tracker for known issues and expectations : [Jenkins Build Flow Component](https://issues.jenkins-ci.org/browse/JENKINS/component/TBC)
* Discussions on this plugin are hosted on  [jenkins-user mailing list](https://wiki.jenkins-ci.org/display/JENKINS/Mailing+Lists)


Configuration
=============

After installing the plugin, you'll get a new option in the Build Environment section of a job configuration page.  If your job deploys to an environment, check the box and complete the form using the context sensitive help.

You can also specify how long to retain the dashboard data, the default is set to 30 days. Any data older than 30 days from the current time is automatically deleted.

Once you have run at least one job with a populated Details for Environment dashboard section, you now have enough data to generate a dashboard.  On the Jenkins home page, click the + to create a new view and create a view.  If you leave all settings blank, you will see the deployments of all components into all environments. You can also limit the deployment history shown when you click on the environment name on the dashboard. The default is last 10 deploys.
