docker run \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://activities-management-db:15433/activities-management-db?sslmode=prefer" \
  -e HMPPS_SQS_LOCALSTACK_URL="http://activities-localstack:4566" \
  -e SPRING_PROFILES_ACTIVE="local" \
  --network hmpps-activities-management_hmpps \
  -p 8080:8080 \
  activities-api