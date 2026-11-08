package catalogue.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Test-data builders for the catalogue, read as domain sentences. */
final class CatalogueBuilders {

    static final JobGroup KITCHEN = JobGroup.of("Kitchen");

    private CatalogueBuilders() {
    }

    static JobListBuilder aJobList() {
        return new JobListBuilder();
    }

    static ToolListBuilder aToolList() {
        return new ToolListBuilder();
    }

    static JobCode job(String code) {
        return JobCode.of(code);
    }

    static ToolCode tool(String code) {
        return ToolCode.of(code);
    }

    static PresetSearch aPresetSearchFor(String... jobs) {
        return PresetSearch.of(Arrays.stream(jobs).map(JobCode::of).collect(Collectors.toSet()), 2, 1);
    }

    static final class JobListBuilder {
        private final List<String> codes = new ArrayList<>();

        JobListBuilder with(String... jobCodes) {
            codes.addAll(List.of(jobCodes));
            return this;
        }

        JobList build() {
            JobList list = JobList.create();
            codes.forEach(code -> list.add(JobCode.of(code), "Job " + code, KITCHEN));
            return list;
        }
    }

    static final class ToolListBuilder {
        private final List<String> codes = new ArrayList<>();

        ToolListBuilder with(String... toolCodes) {
            codes.addAll(List.of(toolCodes));
            return this;
        }

        ToolList build() {
            ToolList list = ToolList.create();
            codes.forEach(code -> list.add(ToolCode.of(code), "Tool " + code));
            return list;
        }
    }
}
