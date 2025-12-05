package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import jakarta.persistence.EntityManager
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.CustomRevisionEntity
import kotlin.reflect.KClass

@Component
class AuditQueryHelper(
  private val entityManager: EntityManager,
) {

  fun <T : Any> getRevisionsForEntity(
    entityClass: KClass<T>,
    id: Long,
  ): List<Triple<T, CustomRevisionEntity, RevisionType>> {
    val auditReader = AuditReaderFactory.get(entityManager)

    @Suppress("UNCHECKED_CAST")
    val rows = auditReader.createQuery()
      .forRevisionsOfEntity(entityClass.java, false, true)
      .add(AuditEntity.id().eq(id))
      .addOrder(AuditEntity.revisionNumber().desc())
      .resultList
      .mapNotNull { it as? Array<Any?> }

    return rows.map { row ->
      val entity = entityClass.java.cast(row[0])
      val revision = row[1] as CustomRevisionEntity
      val revisionType = row[2] as RevisionType
      Triple(entity, revision, revisionType)
    }
  }
}
