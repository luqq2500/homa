package catalogue.usecase;

import catalogue.domain.Job;
import catalogue.domain.JobGroup;
import catalogue.usecase.dto.ViewCatalogueResponse;
import catalogue.usecase.dto.ViewCatalogueResponse.Group;
import catalogue.usecase.dto.ViewCatalogueResponse.Item;
import catalogue.usecase.port.in.ProviderCatalogueViewer;
import catalogue.usecase.port.out.JobGroupRepository;
import catalogue.usecase.port.out.JobRepository;
import catalogue.usecase.port.out.ToolRepository;
import java.util.List;

public final class ProviderViewCatalogue implements ProviderCatalogueViewer {

    private final JobGroupRepository groups;
    private final JobRepository jobs;
    private final ToolRepository tools;

    public ProviderViewCatalogue(JobGroupRepository groups, JobRepository jobs, ToolRepository tools) {
        this.groups = groups;
        this.jobs = jobs;
        this.tools = tools;
    }

    @Override
    public ViewCatalogueResponse view() {
        List<Job> offerable = jobs.all().stream().filter(job -> !job.isRetired()).toList();
        List<Group> byGroup = groups.all().stream().map(group -> groupOf(group, offerable)).toList();
        List<Item> pickable = tools.all().stream()
                .filter(tool -> !tool.isRetired())
                .map(tool -> new Item(tool.id(), tool.name()))
                .toList();
        return new ViewCatalogueResponse(byGroup, pickable);
    }

    private static Group groupOf(JobGroup group, List<Job> offerable) {
        List<Item> jobsInGroup = offerable.stream()
                .filter(job -> job.groupId().equals(group.id()))
                .map(job -> new Item(job.id(), job.name()))
                .toList();
        return new Group(group.id(), group.name(), jobsInGroup);
    }
}
