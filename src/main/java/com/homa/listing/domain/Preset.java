package com.homa.listing.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Homa's presets, kept by Homa admins: the job types, tools and cities a provider may choose from (OFR-1). City
 * codes carry business meaning here (coverage, RES-1), which is why they are presets and not free text.
 */
public final class Preset {

    private final Set<JobType> jobTypes;
    private final Set<Tool> tools;
    private final Set<City> cities;

    private Preset(Set<JobType> jobTypes, Set<Tool> tools, Set<City> cities) {
        this.jobTypes = new LinkedHashSet<>(jobTypes);
        this.tools = new LinkedHashSet<>(tools);
        this.cities = new LinkedHashSet<>(cities);
    }

    /** A catalogue with nothing in it yet. */
    public static Preset empty() {
        return new Preset(Set.of(), Set.of(), Set.of());
    }

    /** The catalogue as it was stored. */
    public static Preset of(Set<JobType> jobTypes, Set<Tool> tools, Set<City> cities) {
        return new Preset(jobTypes, tools, cities);
    }

    public void addJobType(JobType jobType) {
        jobTypes.add(jobType);
    }

    public void addTool(Tool tool) {
        tools.add(tool);
    }

    public void addCity(City city) {
        cities.add(city);
    }

    public Set<JobType> jobTypes() {
        return Set.copyOf(jobTypes);
    }

    public Set<Tool> tools() {
        return Set.copyOf(tools);
    }

    public Set<City> cities() {
        return Set.copyOf(cities);
    }

    /** The codes of the chosen jobs, tools and cities that are not presets; empty when every choice is a preset. */
    public List<String> notInPresets(Offerings offerings) {
        List<String> missing = new ArrayList<>();
        offerings.jobs().stream().filter(job -> !jobTypes.contains(job)).forEach(job -> missing.add(job.code()));
        offerings.tools().stream().filter(tool -> !tools.contains(tool)).forEach(tool -> missing.add(tool.code()));
        offerings.cities().stream().filter(city -> !cities.contains(city)).forEach(city -> missing.add(city.code()));
        return List.copyOf(missing);
    }
}
