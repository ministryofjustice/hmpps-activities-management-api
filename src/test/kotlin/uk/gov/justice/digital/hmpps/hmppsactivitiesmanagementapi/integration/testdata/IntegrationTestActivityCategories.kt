package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory

val educationCategory = ActivityCategory(id = 1, code = "SAA_EDUCATION", name = "Education", description = "Such as classes in English, maths, construction and computer skills")
val industriesCategory = ActivityCategory(id = 2, code = "SAA_INDUSTRIES", name = "Industries", description = "Such as work in the prison and with employers and charities")
val servicesCategory = ActivityCategory(id = 3, code = "SAA_SERVICES", name = "Services", description = "Such as work in the kitchens and laundry, cleaning, gardening, and mentoring")
val gymSportsFitnessCategory = ActivityCategory(id = 4, code = "SAA_GYM_SPORTS_FITNESS", name = "Gym, sport and fitness", description = "Such as sport clubs, like football, fitness classes and gym sessions")
val inductionCategory = ActivityCategory(id = 5, code = "SAA_INDUCTION", name = "Induction", description = "Such as gym induction, education assessments, health and safety workshops")
val interventionsCategory = ActivityCategory(id = 6, code = "SAA_INTERVENTIONS", name = "Intervention programmes", description = "Such as programmes for behaviour management, drug and alcohol misuse and community rehabilitation")
val leisureAndSocialCategory = ActivityCategory(id = 7, code = "SAA_LEISURE_SOCIAL", name = "Leisure and social", description = "Such as association, library time and social clubs, like music or art")
val notInWorkCategory = ActivityCategory(id = 8, code = "SAA_UNEMPLOYMENT", name = "Not in work", description = "Such as unemployed, retired, long-term sick, or on remand")
