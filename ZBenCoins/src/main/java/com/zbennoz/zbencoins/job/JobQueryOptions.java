package com.zbennoz.zbencoins.job;

/**
 * Filter- und Sortieroptionen für die Job-Übersicht.
 */
public class JobQueryOptions {

    public enum SortOption {
        BELOHNUNG_ABSTEIGEND,
        BELOHNUNG_AUFSTEIGEND,
        NEUESTE,
        ABLAUFEND
    }

    private String searchTerm = "";
    private JobType typeFilter = null;
    private JobStatus statusFilter = null;
    private SortOption sortOption = SortOption.NEUESTE;
    private int page = 0;

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm == null ? "" : searchTerm.trim();
    }

    public JobType getTypeFilter() {
        return typeFilter;
    }

    public void setTypeFilter(JobType typeFilter) {
        this.typeFilter = typeFilter;
    }

    public JobStatus getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(JobStatus statusFilter) {
        this.statusFilter = statusFilter;
    }

    public SortOption getSortOption() {
        return sortOption;
    }

    public void setSortOption(SortOption sortOption) {
        this.sortOption = sortOption;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public void reset() {
        searchTerm = "";
        typeFilter = null;
        statusFilter = null;
        sortOption = SortOption.NEUESTE;
        page = 0;
    }

    public JobQueryOptions copy() {
        JobQueryOptions options = new JobQueryOptions();
        options.searchTerm = this.searchTerm;
        options.typeFilter = this.typeFilter;
        options.statusFilter = this.statusFilter;
        options.sortOption = this.sortOption;
        options.page = this.page;
        return options;
    }
}
