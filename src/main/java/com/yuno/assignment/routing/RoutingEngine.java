package com.yuno.assignment.routing;

import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.provider.connector.ProviderConnector;

import java.util.List;

public interface RoutingEngine {

    List<ProviderConnector> selectRoute(Payment payment);
}
