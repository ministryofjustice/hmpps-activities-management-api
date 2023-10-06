package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.springframework.context.ApplicationEvent
import org.springframework.core.ResolvableType
import org.springframework.core.ResolvableTypeProvider
import org.springframework.data.domain.AbstractAggregateRoot

abstract class DomainEventEntity<T : AbstractAggregateRoot<T>> : AbstractAggregateRoot<T>() {

  fun registerCreate() {
    registerEvent(DomainEntityCreatedEvent(this))
  }

  fun registerUpdate() {
    registerEvent(DomainEntityUpdatedEvent(this))
  }
}

class DomainEntityCreatedEvent<T>(entity: T) : ApplicationEvent(entity!!), ResolvableTypeProvider {
  override fun getResolvableType(): ResolvableType? {
    return ResolvableType.forClassWithGenerics(javaClass, ResolvableType.forInstance(getSource()))
  }
}

class DomainEntityUpdatedEvent<T>(entity: T) : ApplicationEvent(entity!!), ResolvableTypeProvider {
  override fun getResolvableType(): ResolvableType? {
    return ResolvableType.forClassWithGenerics(javaClass, ResolvableType.forInstance(getSource()))
  }
}
