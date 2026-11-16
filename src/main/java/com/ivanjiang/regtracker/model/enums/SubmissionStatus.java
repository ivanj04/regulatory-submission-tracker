package com.ivanjiang.regtracker.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum SubmissionStatus {
    DRAFT,
    IN_PREPARATION,
    SUBMITTED,
    UNDER_REVIEW,
    ADDITIONAL_INFO_REQUESTED,
    APPROVED,
    REJECTED,
    WITHDRAWN;

    public boolean canTransitionTo(SubmissionStatus next) {
        return getAllowedTransitions(this).contains(next);
    }

    private static Set<SubmissionStatus> getAllowedTransitions(SubmissionStatus from) {
        return switch (from) {
            case DRAFT -> EnumSet.of(IN_PREPARATION, WITHDRAWN);
            case IN_PREPARATION -> EnumSet.of(SUBMITTED, DRAFT, WITHDRAWN);
            case SUBMITTED -> EnumSet.of(UNDER_REVIEW, WITHDRAWN);
            case UNDER_REVIEW -> EnumSet.of(ADDITIONAL_INFO_REQUESTED, APPROVED, REJECTED, WITHDRAWN);
            case ADDITIONAL_INFO_REQUESTED -> EnumSet.of(SUBMITTED, WITHDRAWN);
            case APPROVED, REJECTED, WITHDRAWN -> EnumSet.noneOf(SubmissionStatus.class);
        };
    }
}
