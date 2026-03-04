
# Changes to SAR template

If you are making any changes to the SAR .mustache template, you'll need to regenerate the sar-generated-report.html test file which records what the result of the SAR report generation process should look like. This allows the integration test to run the SAR generation process and compare the output to this file.

To generate the file, simply run the integration test with the below env variable set to true: 
SAR_GENERATE_ACTUAL=true

e.g.
SAR_GENERATE_ACTUAL=true ./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.SubjectAccessRequestTemplateIntegrationTest"

This will output a new file sar-generated-file.html.log into the test/resources folder
Rename this file to remove the .log extension and copy it into the /test/resources/sar directory

[Further info here](https://github.com/ministryofjustice/hmpps-subject-access-request-lib/blob/main/README.md)