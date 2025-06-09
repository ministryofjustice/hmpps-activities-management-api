package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.envers.RevisionListener
import org.springframework.security.core.context.SecurityContextHolder

class CustomRevisionListener : RevisionListener {
  override fun newRevision(revisionEntity: Any) {
    val revision = revisionEntity as CustomRevisionEntity
    val username = SecurityContextHolder.getContext().authentication?.name ?: "system"

    revision.username = username
  }
}
