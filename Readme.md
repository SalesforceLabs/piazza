Piazza is live and running on Heroku: https://piazza.herokuapp.com/

# Basics
Run `make` in Project root directory to setup project.

`make run` to run project.

# Installing Play

Download the latest version of Play from [Play Framework website][play].
Unzip the archive and add it to your `PATH` in your `.bashrc`.

You will also need to declare your application secret in your environment.

    export PLAY_SECRET="[some random string longer than 16 characters]"

Also make sure to mark your local environment as a dev environment:

    $ play id
    [SNIP]
    What is the new framework ID (or blank to unset)? dev
    OK, the framework ID is now dev

[play]: http://www.playframework.org/

# Twitter OAUTH Credentials

Create a Twitter application at https://dev.twitter.com . Once you've done that
you need to set the OAUTH key and secret in your local environment.

Add these lines to your `.bashrc` file:

    export TWT_OAUTH_KEY="[TWITTER OAUTH KEY HERE]"
    export TWT_OAUTH_SECRET="[TWITTER OAUTH SECRET HERE]"

# Salesforce.com OAUTH Credentials

See this [guide to setting up AUTH with Salesforce.com][sfdcoauth]. You will need to create a
Salesforce.com Developer org and generate OAUTH credentials for a new app. Once you're done with that
you will need to update your `.bashrc` with these credentials:

	export SFDC_KEY="[Salesforce.com OAUTH consumer key] "
	export SFDC_SECRET="[Salesforce.com OAUTH consumer secret]"
	export SFDC_REDIRECT_URI="https://localhost:9443/config"

[sfdcoauth]: http://wiki.developerforce.com/index.php/Digging_Deeper_into_OAuth_2.0_at_Salesforce.com

# Setting up PostgreSql

First install PostgreSql. Google for this step, there are many tutorials out there
for how to do this on all platforms.

Once postgres is set up you will want to create a user for yourself and create
a new DB for anza.

    createuser --superuser `whoami` -U postgres
    createdb anza
    

Add the following environment variables to your `bashrc` file:

    # Heroku db setup
    export DATABASE_PATH=postgresql:anza
    export DATABASE_USER=`whoami`
    export DATABASE_PASS=

Restart your shell so the new environment variables are read.

Update the password for your user within postgres db.

    $ psql anza
    anza=# alter user your_user_name password '';

You should now be able to start your Play app.

# memcached (Optional)

Set up memcached to run on port `11211` if you want local memcached support.
Play will fallback on a local cache implementation if memcached doesn't exists. 

# Heroku Environment

Heroku best practices recommend that we use [Heroku config values][herokuconfig]
for setting application specific data. This includes secure values such as
the Play application secret that we do not want to commit to source control.

[herokuconfig]: http://devcenter.heroku.com/articles/config-vars

# Other Make Targets

We use make targets to manage staging and production deployments. You can more about
our branching and strategy [here][staging].

[staging]: http://paksoy.net/post/9634387657/simple-staging-on-heroku 

## `make stage`

Deploy the `staging` branch to the staging environment.

You need to first merge the changes you want to push
into the `staging` branch.

## `make deploy`

Deploy the `prod` branch to the production environment.

You need to first merge the changes you want to push
into the `prod` branch.

*This is the production environment so make sure you're not pushing bad code.*

## `make jscompress`

Combine all JS files into a single file to be used in production.

## `make push`

Push master branch to GitHub.

## `make clean`

Remove compiled class files.

## `make superclean`

*RUN THIS AT YOUR OWN RISK!*

Bring the project into the original git checkout state. Will
wipe away all untracked files and untracked changes.
