package com.stavya.janawaaz.service;

import com.stavya.janawaaz.dto.StatsDtos.*;
import com.stavya.janawaaz.entity.*;
import com.stavya.janawaaz.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsightsService {

    /** Surveys finished faster than this are flagged as possibly not filled in properly. */
    public static final int MIN_SECONDS_PER_SURVEY = 180;
    /** An issue needs this many real answers before it can rank as a top problem. */
    private static final int MIN_ANSWERS_TO_RANK = 10;
    private static final int TOP_N = 5;

    private final WardRepository wardRepository;
    private final QuestionRepository questionRepository;
    private final VolunteerRepository volunteerRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyAnswerRepository answerRepository;
    private final ComplaintRepository complaintRepository;
    private final MetaService metaService;

    @Value("${app.public.min-households:30}")
    private int minHouseholds;

    // ---------- Ward view (Janta and volunteer) ----------

    public WardReport wardReport(Integer wardId) {
        Ward ward = metaService.ward(wardId);

        Map<ComplaintCategory, long[]> byCat = new EnumMap<>(ComplaintCategory.class);
        for (Object[] r : complaintRepository.countByCategoryAndStatusForWard(wardId)) {
            byCat.computeIfAbsent((ComplaintCategory) r[0], k -> new long[3])
                    [((ComplaintStatus) r[1]).ordinal()] += (Long) r[2];
        }
        long[] totals = new long[3];
        List<CategoryCount> categories = new ArrayList<>();
        byCat.forEach((cat, c) -> {
            for (int i = 0; i < 3; i++) totals[i] += c[i];
            categories.add(new CategoryCount(cat, c[0] + c[1] + c[2], c[0], c[1], c[2]));
        });
        categories.sort(Comparator.comparingLong(CategoryCount::total).reversed());

        List<TicketPoint> points = complaintRepository.pointsForWard(wardId).stream()
                .map(r -> new TicketPoint((Double) r[0], (Double) r[1],
                        (ComplaintStatus) r[2], (ComplaintCategory) r[3]))
                .toList();

        long surveyed = surveyRepository.countByWardId(wardId);
        boolean enough = surveyed >= minHouseholds;
        List<IssueStat> top = enough
                ? topRed(issueStats(answerRepository.countByQuestionAndRatingForWard(wardId)))
                : List.of();

        return new WardReport(ward.getId(), ward.getLocalityEn(), ward.getLocalityHi(),
                ward.getCenterLat(), ward.getCenterLng(), ward.getRadiusMeters(),
                ticketTotals(totals), categories, points, surveyed, minHouseholds, enough, top);
    }

    // ---------- Admin ----------

    public AdminOverview adminOverview() {
        List<Ward> wards = wardRepository.findAll(Sort.by("id"));
        Map<Integer, Long> surveyed = toCountMap(surveyRepository.countPerWard());
        Map<Integer, Long> flagged = toCountMap(surveyRepository.countFlaggedPerWard(MIN_SECONDS_PER_SURVEY));
        Map<Integer, Long> volunteers = toCountMap(volunteerRepository.countPerWard());
        Map<Integer, long[]> ticketsByWard = ticketsByWard();

        long[] allTickets = new long[3];
        List<AdminWardRow> rows = new ArrayList<>();
        for (Ward w : wards) {
            long[] t = ticketsByWard.getOrDefault(w.getId(), new long[3]);
            for (int i = 0; i < 3; i++) allTickets[i] += t[i];
            rows.add(new AdminWardRow(w.getId(), w.getLocalityEn(), w.getLocalityHi(),
                    surveyed.getOrDefault(w.getId(), 0L), w.getTargetHouseholds(), ticketTotals(t),
                    volunteers.getOrDefault(w.getId(), 0L), flagged.getOrDefault(w.getId(), 0L)));
        }

        Instant today = MetaService.startOfTodayIst();
        return new AdminOverview(
                surveyed.values().stream().mapToLong(Long::longValue).sum(),
                wards.stream().mapToLong(Ward::getTargetHouseholds).sum(),
                volunteerRepository.count(),
                surveyRepository.countBySubmittedAtAfter(today),
                complaintRepository.countByCreatedAtAfter(today),
                ticketTotals(allTickets),
                avgResolutionDays(),
                surveyRepository.countFlagged(MIN_SECONDS_PER_SURVEY),
                topRed(issueStats(answerRepository.countByQuestionAndRating())),
                rows);
    }

    public AdminWardDetail adminWardDetail(Integer wardId) {
        WardReport report = wardReport(wardId);

        Map<Long, long[]> activity = new HashMap<>();      // [surveys, tickets, flagged]
        Map<Long, Instant> lastActive = new HashMap<>();
        for (Object[] r : surveyRepository.activityByVolunteerForWard(wardId)) {
            activity.computeIfAbsent((Long) r[0], k -> new long[3])[0] = (Long) r[1];
            lastActive.merge((Long) r[0], (Instant) r[2], InsightsService::later);
        }
        for (Object[] r : complaintRepository.activityByVolunteerForWard(wardId)) {
            activity.computeIfAbsent((Long) r[0], k -> new long[3])[1] = (Long) r[1];
            lastActive.merge((Long) r[0], (Instant) r[2], InsightsService::later);
        }
        for (Object[] r : surveyRepository.flaggedByVolunteerForWard(wardId, MIN_SECONDS_PER_SURVEY)) {
            activity.computeIfAbsent((Long) r[0], k -> new long[3])[2] = (Long) r[1];
        }
        // Assigned volunteers appear even if they have done nothing yet.
        volunteerRepository.findByWardId(wardId).forEach(v -> activity.computeIfAbsent(v.getId(), k -> new long[3]));

        Map<Long, Volunteer> volunteerById = volunteerRepository.findAllById(activity.keySet()).stream()
                .collect(Collectors.toMap(Volunteer::getId, Function.identity()));
        List<VolunteerActivity> volunteers = activity.entrySet().stream()
                .map(e -> {
                    Volunteer v = volunteerById.get(e.getKey());
                    long[] a = e.getValue();
                    return new VolunteerActivity(v.getId(), v.getCode(), v.getName(),
                            a[0], a[1], a[2], lastActive.get(v.getId()));
                })
                .sorted(Comparator.comparingLong((VolunteerActivity v) -> v.surveys() + v.tickets()).reversed())
                .toList();

        long flagged = volunteers.stream().mapToLong(VolunteerActivity::flaggedSurveys).sum();
        var recent = complaintRepository.findByWardIdOrderByCreatedAtDesc(wardId, PageRequest.of(0, 10))
                .stream().map(ComplaintService::toResponse).toList();

        return new AdminWardDetail(report, flagged, volunteers, recent);
    }

    public Heatmap heatmap() {
        List<Question> questions = questionRepository.findAll(Sort.by("id"));
        // wardId -> questionId -> [green, yellow, red, na]
        Map<Integer, Map<Integer, long[]>> counts = new HashMap<>();
        for (Object[] r : answerRepository.countByWardQuestionAndRating()) {
            counts.computeIfAbsent((Integer) r[0], k -> new HashMap<>())
                    .computeIfAbsent((Integer) r[1], k -> new long[4])
                    [((Rating) r[2]).ordinal()] += (Long) r[3];
        }

        List<HeatmapColumn> columns = questions.stream()
                .map(q -> new HeatmapColumn(q.getId(), q.getShortEn(), q.getShortHi()))
                .toList();
        List<HeatmapRow> rows = wardRepository.findAll(Sort.by("id")).stream()
                .map(w -> {
                    Map<Integer, long[]> byQ = counts.getOrDefault(w.getId(), Map.of());
                    List<Integer> reds = new ArrayList<>();
                    for (Question q : questions) {
                        long[] c = byQ.get(q.getId());
                        long answered = c == null ? 0 : c[0] + c[1] + c[2];
                        reds.add(answered < 5 ? null : pct(c[2], answered));
                    }
                    return new HeatmapRow(w.getId(), w.getLocalityEn(), w.getLocalityHi(), reds);
                })
                .toList();
        return new Heatmap(columns, rows);
    }

    public Quality quality() {
        List<FlaggedSurvey> flagged = surveyRepository
                .findFlagged(MIN_SECONDS_PER_SURVEY, PageRequest.of(0, 50)).stream()
                .map(s -> new FlaggedSurvey(s.getReceiptNo(), s.getWard().getId(), s.getWard().getLocalityEn(),
                        s.getWard().getLocalityHi(), s.getVolunteer().getCode(), s.getVolunteer().getName(),
                        s.getLocationStatus() == LocationStatus.OUTSIDE_WARD,
                        s.getDurationSeconds() < MIN_SECONDS_PER_SURVEY,
                        s.getDurationSeconds(), s.getDistanceFromWardMeters(), s.getSubmittedAt()))
                .toList();
        return new Quality(surveyRepository.count(),
                surveyRepository.countByLocationStatus(LocationStatus.OUTSIDE_WARD),
                surveyRepository.countByDurationSecondsLessThan(MIN_SECONDS_PER_SURVEY),
                MIN_SECONDS_PER_SURVEY, flagged);
    }

    public AdminMap adminMap() {
        Map<Integer, long[]> ticketsByWard = ticketsByWard();
        List<WardHeat> wards = wardRepository.findAll(Sort.by("id")).stream()
                .map(w -> new WardHeat(w.getId(), w.getLocalityEn(), w.getLocalityHi(),
                        w.getCenterLat(), w.getCenterLng(), w.getRadiusMeters(),
                        ticketTotals(ticketsByWard.getOrDefault(w.getId(), new long[3]))))
                .toList();
        List<AdminTicketPoint> tickets = complaintRepository.allPoints().stream()
                .map(r -> new AdminTicketPoint((Double) r[0], (Double) r[1], (Integer) r[2], (String) r[3],
                        (ComplaintStatus) r[4], (ComplaintCategory) r[5]))
                .toList();
        List<SurveyPoint> surveys = surveyRepository.allPoints().stream()
                .map(r -> new SurveyPoint((Double) r[0], (Double) r[1], (Integer) r[2], (LocationStatus) r[3]))
                .toList();
        return new AdminMap(wards, tickets, surveys);
    }

    // ---------- helpers ----------

    /** wardId -> [reported, inProgress, resolved] */
    private Map<Integer, long[]> ticketsByWard() {
        Map<Integer, long[]> map = new HashMap<>();
        for (Object[] r : complaintRepository.countByWardAndStatus()) {
            map.computeIfAbsent((Integer) r[0], k -> new long[3])[((ComplaintStatus) r[1]).ordinal()] += (Long) r[2];
        }
        return map;
    }

    private List<IssueStat> issueStats(List<Object[]> rows) {
        Map<Integer, long[]> byQuestion = new HashMap<>();
        for (Object[] r : rows) {
            byQuestion.computeIfAbsent((Integer) r[0], k -> new long[4])[((Rating) r[1]).ordinal()] += (Long) r[2];
        }
        List<IssueStat> stats = new ArrayList<>();
        for (Question q : questionRepository.findAll(Sort.by("id"))) {
            long[] c = byQuestion.getOrDefault(q.getId(), new long[4]);
            long answered = c[0] + c[1] + c[2];
            stats.add(new IssueStat(q.getId(), q.getShortEn(), q.getShortHi(), c[0], c[1], c[2], c[3],
                    pct(c[0], answered), pct(c[1], answered), pct(c[2], answered)));
        }
        return stats;
    }

    private static List<IssueStat> topRed(List<IssueStat> stats) {
        return stats.stream()
                .filter(s -> s.green() + s.yellow() + s.red() >= MIN_ANSWERS_TO_RANK)
                .sorted(Comparator.comparingInt(IssueStat::redPct).reversed()
                        .thenComparing(Comparator.comparingInt(IssueStat::yellowPct).reversed()))
                .limit(TOP_N)
                .toList();
    }

    private Double avgResolutionDays() {
        List<Object[]> rows = complaintRepository.resolutionTimes();
        if (rows.isEmpty()) {
            return null;
        }
        double totalHours = rows.stream()
                .mapToLong(r -> Duration.between((Instant) r[0], (Instant) r[1]).toHours())
                .sum();
        return Math.round(totalHours / 24.0 / rows.size() * 10) / 10.0;
    }

    private static TicketTotals ticketTotals(long[] t) {
        return new TicketTotals(t[0] + t[1] + t[2], t[0], t[1], t[2]);
    }

    private static Map<Integer, Long> toCountMap(List<Object[]> rows) {
        Map<Integer, Long> m = new HashMap<>();
        rows.forEach(r -> m.put((Integer) r[0], (Long) r[1]));
        return m;
    }

    private static int pct(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    private static Instant later(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
