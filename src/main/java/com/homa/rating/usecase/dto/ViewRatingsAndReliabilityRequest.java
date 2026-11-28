package com.homa.rating.usecase.dto;

/**
 * A customer or provider looks at the ratings and reliability of another party, or their own (RTG-6, RTG-10).
 *
 * @param viewerId    the viewer's own customer or provider id
 * @param viewerRole  CUSTOMER or PROVIDER
 * @param subjectId   the customer or provider looked at
 * @param subjectRole CUSTOMER or PROVIDER
 */
public record ViewRatingsAndReliabilityRequest(
        String viewerId, String viewerRole, String subjectId, String subjectRole) {
}
