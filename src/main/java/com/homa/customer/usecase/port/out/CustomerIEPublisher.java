package com.homa.customer.usecase.port.out;

import com.homa.ie.port.CustomerIE;

/** The Customer context's single integration event publisher, called inside the use case's transaction. */
public interface CustomerIEPublisher {

    void publish(CustomerIE event);
}
