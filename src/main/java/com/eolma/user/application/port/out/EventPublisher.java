package com.eolma.user.application.port.out;

import com.eolma.common.event.DomainEvent;

public interface EventPublisher {

    void publish(DomainEvent<?> event);
}
