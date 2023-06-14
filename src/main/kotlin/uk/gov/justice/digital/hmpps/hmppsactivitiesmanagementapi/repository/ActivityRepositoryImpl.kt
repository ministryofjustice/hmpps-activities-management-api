package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.stereotype.Repository

@Repository
open class ActivityRepositoryImpl {

   /* @Autowired
    private lateinit var entityManager : EntityManager

    override fun getLimited (id: Long, earliestScheduledStartDate: LocalDate?): Optional<Activity> {

        val earliestDate = earliestScheduledStartDate ?: LocalDate.now().minusDays(28)
        println("*** Earliest date is $earliestDate ***")
        val session: Session = entityManager.unwrap(Session::class.java)
        val filter = session.enableFilter("ScheduledInstanceByDateFilter")
        filter.setParameter("earliestStartDate", earliestDate)



        return Optional.ofNullable(entityManagerActivity::class.java, id))
    }*/
}
