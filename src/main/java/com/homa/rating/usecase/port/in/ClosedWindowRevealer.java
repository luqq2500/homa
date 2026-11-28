package com.homa.rating.usecase.port.in;

import com.homa.rating.usecase.dto.RevealClosedRatingWindowsRequest;
import com.homa.rating.usecase.dto.RevealClosedRatingWindowsResponse;

/**
 * RTG-3: the scheduler reveals the ratings of every reservation whose Rating Window has closed. It is not wrapped in
 * one transaction: each session is revealed in its own unit of work.
 */
public interface ClosedWindowRevealer {

    RevealClosedRatingWindowsResponse revealClosed(RevealClosedRatingWindowsRequest request);
}
