package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.ProposeSmallerRosterRequest;
import com.homa.reservation.usecase.dto.ProposeSmallerRosterResponse;

/** ROS-8: a provider short of helpers offers the customer a smaller roster before the Check-in Period. */
public interface SmallerRosterProposer {

    ProposeSmallerRosterResponse propose(ProposeSmallerRosterRequest request);
}
