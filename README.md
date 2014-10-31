Jenkins Environment Dashboard Plugin
=========================

This Jenkins plugin creates a custom view which can be used as a dashboard to display what code release versions have been deployed to what test and production environments (or devices).

[![Build Status](https://jenkins.ci.cloudbees.com/job/plugins/job/environment-dashboard/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/environment-dashboard/)

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

You have Jenkins jobs for deploying the build packages to your environments.

You find it hard to track which version of each application is currently deployed to.

You configure your deployment jobs to publish a record of each deployment to this plugin.

You create a view which displays a matrix with rows for your 3 applications, columns for your environments and the intersections with the deployed code version.


Other information:
* Bug Tracker for known issues and expectations : [Jenkins Build Flow Component](https://issues.jenkins-ci.org/browse/JENKINS/component/TBC)
* Discussions on this plugin are hosted on  [jenkins-user mailing list](https://wiki.jenkins-ci.org/display/JENKINS/Mailing+Lists)


Configuration
=============

After installing the plugin, you'll get a new option in the Build Environment section of a job configuration page.  If your job deploys to an environment, check the box and complete the form using the context sensitive help.


Once you have run at least one job with a populated Details for Environment dashboard section, you now have enough data to generate a dashboard.  On the Jenkins home page, click the + to create a new view and create a view.  If you leave all settings blank, you will see the deployments of all components into all environments.


