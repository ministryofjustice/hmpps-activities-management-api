package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory

val educationCategory = ActivityCategory(id = 1, code = "SAA_EDUCATION", name = "Education", description = "Such as classes in English, maths, construction or barbering")
val industriesCategory = ActivityCategory(id = 2, code = "SAA_INDUSTRIES", name = "Industries", description = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities")
val prisonJobsCategory = ActivityCategory(id = 3, code = "SAA_PRISON_JOBS", name = "Prison jobs", description = "Such as kitchen, cleaning, gardens or other maintenance and services to keep the prison running")
val gymSportsFitnessCategory = ActivityCategory(id = 4, code = "SAA_GYM_SPORTS_FITNESS", name = "Gym, sport, fitness", description = "Such as sports clubs, like football, or recreational gym sessions")
val inductionCategory = ActivityCategory(id = 5, code = "SAA_INDUCTION", name = "Induction", description = "Such as gym induction, education assessments or health and safety workshops")
val interventionsCategory = ActivityCategory(id = 6, code = "SAA_INTERVENTIONS", name = "Intervention programmes", description = "Such as programmes for behaviour management, drug and alcohol misuse and community rehabilitation")
val faithAndSpiritualityCategory = ActivityCategory(id = 7, code = "SAA_FAITH_SPIRITUALITY", name = "Faith and spirituality", description = "Such as chapel, prayer meetings or meditation")
val notInWorkCategory = ActivityCategory(id = 8, code = "SAA_NOT_IN_WORK", name = "Not in work", description = "Such as unemployed, retired, long-term sick, or on remand")
val otherCategory = ActivityCategory(id = 9, code = "SAA_OTHER", name = "Other", description = "Select if the activity you’re creating doesn’t fit any other category")
