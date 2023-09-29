openapi-generator generate -i https://prisoner-search-dev.prison.service.justice.gov.uk/v3/api-docs \
-g kotlin-spring \
--model-package=uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model \
--additional-properties=dateLibrary=java8-localdatetime,serializationLibrary=jackson,useBeanValidation=false,enumPropertyNaming=UPPERCASE
