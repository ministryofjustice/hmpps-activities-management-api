#
# This script is used to run the Activities management API locally.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -local profile (from application-local.yml). The latter overrides some of the defaults.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around removing the SSL connection to the database and setting the DB properties, SERVER_PORT and client credentials
# to match those used in the docker-compose files.
#

# Provide the DB connection details to local container-hosted Postgresql DB running
export DB_SERVER=localhost
export DB_NAME=activities-management-db
export DB_USER=activities-management
export DB_PASS=activities-management
export DB_SSL_MODE=prefer
export DPR_USER=dpr_user
export DPR_PASSWORD=dpr_password
export AWS_S3_AP_BUCKET=dummy-bucket
export AWS_S3_AP_PROJECT=dummy-project
export $(cat .env | xargs)  # If you want to set or update the current shell environment

# Run the application with stdout and local profiles active
SPRING_PROFILES_ACTIVE=stdout,local ./gradlew bootRun

# End

