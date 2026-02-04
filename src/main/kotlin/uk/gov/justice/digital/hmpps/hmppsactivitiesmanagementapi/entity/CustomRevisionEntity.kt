package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionMapping
import java.time.LocalDateTime

@Entity
@Table(name = "revision")
@RevisionEntity(CustomRevisionListener::class)
open class CustomRevisionEntity(
  open var username: String,

  @Column(insertable = false)
  open var revisionDateTime: LocalDateTime,
) : RevisionMapping()
