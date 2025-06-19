package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionEntity

@Entity
@Table(name = "revision")
@RevisionEntity(CustomRevisionListener::class)
data class CustomRevisionEntity(

  var username: String? = null,

) : DefaultRevisionEntity()
