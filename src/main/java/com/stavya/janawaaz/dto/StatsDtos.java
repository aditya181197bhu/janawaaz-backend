package com.stavya.janawaaz.dto;

import com.stavya.janawaaz.entity.ComplaintCategory;
import com.stavya.janawaaz.entity.ComplaintStatus;
import com.stavya.janawaaz.entity.LocationStatus;

import java.time.Instant;
import java.util.List;

public final class StatsDtos {

    private StatsDtos() {
    }

    /** Percentages are of households that answered (Not applicable excluded). */
    public record IssueStat(Integer questionId, String shortEn, String shortHi,
                            long green, long yellow, long red, long notApplicable,
                            int greenPct, int yellowPct, int redPct) {
    }

    /** "reported" means logged but not yet picked up. */
    public record TicketTotals(long total, long reported, long inProgress, long resolved) {
        public static final TicketTotals EMPTY = new TicketTotals(0, 0, 0, 0);

        public long open() {
            return reported + inProgress;
        }
    }

    public record CategoryCount(ComplaintCategory category, long total, long reported,
                                long inProgress, long resolved) {
    }

    /** Public map point: no ticket number, name or description. */
    public record TicketPoint(double lat, double lng, ComplaintStatus status, ComplaintCategory category) {
    }

    // ---- Janta (public) and volunteer ward view ----

    public record WardReport(Integer wardId, String localityEn, String localityHi,
                             double centerLat, double centerLng, int radiusMeters,
                             TicketTotals tickets, List<CategoryCount> byCategory, List<TicketPoint> points,
                             long householdsSurveyed, int minHouseholds, boolean enoughSurveyData,
                             List<IssueStat> topProblems) {
    }

    // ---- Admin ----

    public record AdminWardRow(Integer wardId, String localityEn, String localityHi,
                               long surveyed, int target, TicketTotals tickets,
                               long volunteers, long flaggedSurveys) {
    }

    public record AdminOverview(long householdsSurveyed, long targetHouseholds, long volunteers,
                                long surveysToday, long ticketsToday, TicketTotals tickets,
                                Double avgResolutionDays, long flaggedSurveys,
                                List<IssueStat> topRedIssues, List<AdminWardRow> wards) {
    }

    public record VolunteerActivity(Long volunteerId, String code, String name,
                                    long surveys, long tickets, long flaggedSurveys, Instant lastActiveAt) {
    }

    public record AdminWardDetail(WardReport report, long flaggedSurveys,
                                  List<VolunteerActivity> volunteers,
                                  List<ComplaintDtos.ComplaintResponse> recentTickets) {
    }

    public record HeatmapColumn(Integer questionId, String shortEn, String shortHi) {
    }

    /** redPct holds one value per column, in column order; null means too few answers. */
    public record HeatmapRow(Integer wardId, String localityEn, String localityHi, List<Integer> redPct) {
    }

    public record Heatmap(List<HeatmapColumn> columns, List<HeatmapRow> rows) {
    }

    public record FlaggedSurvey(String receiptNo, Integer wardId, String localityEn, String localityHi,
                                String volunteerCode, String volunteerName, boolean outsideWard,
                                boolean tooFast, Integer durationSeconds, Integer distanceFromWardMeters,
                                Instant submittedAt) {
    }

    public record Quality(long totalSurveys, long outsideWard, long tooFast, int minSecondsPerSurvey,
                          List<FlaggedSurvey> flagged) {
    }

    public record WardHeat(Integer wardId, String localityEn, String localityHi,
                           double centerLat, double centerLng, int radiusMeters, TicketTotals tickets) {
    }

    public record AdminTicketPoint(double lat, double lng, Integer wardId, String ticketNo,
                                   ComplaintStatus status, ComplaintCategory category) {
    }

    public record SurveyPoint(double lat, double lng, Integer wardId, LocationStatus locationStatus) {
    }

    public record AdminMap(List<WardHeat> wards, List<AdminTicketPoint> tickets, List<SurveyPoint> surveys) {
    }
}
