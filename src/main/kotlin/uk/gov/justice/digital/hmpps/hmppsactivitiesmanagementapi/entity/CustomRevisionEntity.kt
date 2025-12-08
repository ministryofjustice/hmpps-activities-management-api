package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionEntity
import java.time.LocalDateTime

@Entity
@Table(name = "revision")
@RevisionEntity(CustomRevisionListener::class)
open class CustomRevisionEntity(
  var username: String,

  @Column(insertable = false)
  var revisionDateTime: LocalDateTime,
) : DefaultRevisionEntity()
