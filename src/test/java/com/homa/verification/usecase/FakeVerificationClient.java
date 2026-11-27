package com.homa.verification.usecase;

import com.homa.verification.usecase.port.out.StartedVerification;
import com.homa.verification.usecase.port.out.VerificationAnswer;
import com.homa.verification.usecase.port.out.VerificationClient;
import com.homa.verification.usecase.port.out.VerificationSubmission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The verification service: it takes every check, and answers what the test scripts. */
public final class FakeVerificationClient implements VerificationClient {

    private final List<VerificationSubmission> submissions = new ArrayList<>();
    private final Map<String, String> referenceByVerification = new HashMap<>();
    private final Map<String, VerificationAnswer> answers = new HashMap<>();
    private final List<String> resultsAsked = new ArrayList<>();

    @Override
    public StartedVerification start(VerificationSubmission submission) {
        submissions.add(submission);
        String reference = "service-ref-" + submissions.size();
        referenceByVerification.put(submission.verificationId(), reference);
        return new StartedVerification(reference, "https://verify.example/session/" + reference);
    }

    @Override
    public Optional<VerificationAnswer> resultOf(String reference) {
        resultsAsked.add(reference);
        return Optional.ofNullable(answers.get(reference));
    }

    /** The service answers the check it knows by {@code reference}. */
    public void answers(String reference, VerificationAnswer answer) {
        answers.put(reference, answer);
    }

    /** How the service knows Homa's verification {@code verificationId}. */
    public String referenceOf(String verificationId) {
        return referenceByVerification.get(verificationId);
    }

    public List<VerificationSubmission> submissions() {
        return List.copyOf(submissions);
    }

    public List<String> resultsAsked() {
        return List.copyOf(resultsAsked);
    }
}
