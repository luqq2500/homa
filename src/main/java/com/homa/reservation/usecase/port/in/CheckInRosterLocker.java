package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.LockRostersAtCheckInRequest;
import com.homa.reservation.usecase.dto.LockRostersAtCheckInResponse;

/** ROS-1, ROS-7, NRS-1 to NRS-3: the scheduler locks every roster whose Check-in Period has started. */
public interface CheckInRosterLocker {

    LockRostersAtCheckInResponse lockRosters(LockRostersAtCheckInRequest request);
}
