#
# This script is used to run the Activities management API locally.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -local and -localstack profiles. The latter overrides some of the defaults.
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
export FEATURE_EVENTS_SNS_ENABLED=true

# Run the application with stdout and local profiles active
SPRING_PROFILES_ACTIVE=stdout,localstack ./gradlew bootRun

# End

