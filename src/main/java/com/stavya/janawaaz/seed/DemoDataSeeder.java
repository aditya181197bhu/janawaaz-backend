package com.stavya.janawaaz.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stavya.janawaaz.entity.*;
import com.stavya.janawaaz.repository.*;
import com.stavya.janawaaz.service.ComplaintService;
import com.stavya.janawaaz.service.LocationService;
import com.stavya.janawaaz.service.MetaService;
import com.stavya.janawaaz.service.SurveyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Loads wards and questions from resources/seed, and (when app.demo.seed=true) fills an
 * empty database with realistic sample surveys and tickets so the demo looks alive.
 * A fixed random seed keeps the sample data the same on every fresh database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final WardRepository wardRepository;
    private final QuestionRepository questionRepository;
    private final VolunteerRepository volunteerRepository;
    private final SurveyRepository surveyRepository;
    private final ComplaintRepository complaintRepository;
    private final LocationService locationService;
    private final ObjectMapper objectMapper;

    @Value("${app.demo.seed:true}")
    private boolean seedSamples;

    private final Random random = new Random(42);

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        if (wardRepository.count() == 0) {
            wardRepository.saveAll(readJson("seed/wards.json", new TypeReference<List<Ward>>() { }));
            questionRepository.saveAll(readJson("seed/questions.json", new TypeReference<List<Question>>() { }));
            log.info("Seeded wards and questions");
        }
        if (volunteerRepository.count() == 0) {
            seedVolunteers();
        }
        if (seedSamples && surveyRepository.count() == 0) {
            seedSurveys();
            seedComplaints();
        }
    }

    private <T> T readJson(String path, TypeReference<T> type) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        }
    }

    // ---------------- volunteers ----------------

    private static final String[][] VOLUNTEER_NAMES = {
            {"Amit Kumar", "Pooja Singh"}, {"Rakesh Yadav", "Neha Maurya"}, {"Sanjay Gupta", "Anjali Patel"},
            {"Vikas Mishra", "Sunita Devi"}, {"Rahul Bind", "Priya Sonkar"}, {"Arvind Pandey", "Kavita Tiwari"},
            {"Dinesh Kol", "Rekha Prajapati"}, {"Manoj Kushwaha", "Shalini Verma"}, {"Suresh Pal", "Geeta Chaurasia"},
            {"Ajay Dubey", "Ritu Jaiswal"}};

    /** The first volunteer (Amit Kumar, SV-0101, Ward 1) is the app's demo volunteer profile. */
    private void seedVolunteers() {
        for (Ward ward : wardRepository.findAll(Sort.by("id"))) {
            String[] names = VOLUNTEER_NAMES[ward.getId() - 1];
            for (int i = 0; i < names.length; i++) {
                volunteerRepository.save(Volunteer.builder()
                        .code(String.format("SV-%02d%02d", ward.getId(), i + 1))
                        .name(names[i])
                        .phone("98" + String.format("%08d", random.nextInt(100_000_000)))
                        .ward(ward)
                        .build());
            }
        }
        log.info("Seeded volunteers");
    }

    // ---------------- surveys ----------------

    /** Seat-wide chance that a household rates each issue Red (index = question id). */
    private static final double[] BASE_RED = {0,
            .25, .25, .33, .20, .33, .20, .28, .30, .25, .20,
            .25, .30, .30, .33, .25, .20, .38, .20, .50, .45,
            .25, .30, .30, .30, .35};

    /** Ward character: extra Red chance for specific issues. */
    private static final Map<Integer, Map<Integer, Double>> WARD_BUMPS = Map.of(
            1, Map.of(12, .20, 3, .15),
            2, Map.of(13, .25, 5, .10),
            3, Map.of(17, .30, 5, .15, 21, .15),
            4, Map.of(4, .20, 10, .20),
            5, Map.of(14, .25, 16, .15),
            6, Map.of(20, .10, 1, -.10, 2, -.10, 3, -.10, 5, -.10),
            7, Map.of(2, .20, 3, .20, 7, .25),
            8, Map.of(2, .20, 3, .15, 7, .25, 18, .15),
            9, Map.of(1, .25, 18, .15),
            10, Map.of(11, .30, 14, .20, 17, .15));

    private static final String[] FEMALE = {"Sunita", "Meena", "Rekha", "Anita", "Kiran", "Savitri", "Pushpa", "Seema",
            "Lalita", "Nisha", "Asha", "Shabnam", "Rani", "Kusum", "Poonam"};
    private static final String[] MALE = {"Ramesh", "Suresh", "Mohan", "Rajesh", "Shyam", "Ravi", "Salim", "Mahesh",
            "Raju", "Ashok", "Dinesh", "Imran", "Gopal", "Santosh", "Mukesh"};
    private static final String[] SURNAMES = {"Yadav", "Gupta", "Maurya", "Bind", "Patel", "Sonkar", "Mishra",
            "Ansari", "Kol", "Prajapati", "Jaiswal", "Pal", "Kushwaha", "Chaurasia", "Verma"};
    private static final String[] OCCUPATIONS = {"Weaver", "Farmer", "Shopkeeper", "Daily wage labourer", "Homemaker",
            "Student", "Teacher", "Driver", "Brass worker", "Government employee", "Retired", "Vendor"};

    private void seedSurveys() {
        List<Question> questions = questionRepository.findAll(Sort.by("id"));
        List<Survey> all = new ArrayList<>();
        for (Ward ward : wardRepository.findAll(Sort.by("id"))) {
            List<Volunteer> volunteers = volunteerRepository.findByWardId(ward.getId());
            int count = 30 + random.nextInt(13);
            for (int i = 0; i < count; i++) {
                all.add(buildSurvey(ward, volunteers.get(random.nextInt(volunteers.size())), questions));
            }
        }
        surveyRepository.saveAll(all);
        all.forEach(s -> s.setReceiptNo(SurveyService.receiptNo(s.getWard().getId(), s.getId())));
        log.info("Seeded {} sample surveys", all.size());
    }

    private Survey buildSurvey(Ward ward, Volunteer volunteer, List<Question> questions) {
        boolean female = random.nextBoolean();
        String first = pick(female ? FEMALE : MALE);
        boolean hasMobile = random.nextDouble() < .65;
        Instant submitted = randomPastTime(13);
        int duration = random.nextDouble() < .06 ? 90 + random.nextInt(80) : 240 + random.nextInt(660);
        double[] point = randomPoint(ward, random.nextDouble() < .07);
        LocationService.Check location = locationService.check(ward, point[0], point[1]);

        Survey s = Survey.builder()
                .volunteer(volunteer)
                .ward(ward)
                .respondentName(first + " " + pick(SURNAMES))
                .respondentAge(18 + random.nextInt(58))
                .respondentGender(female ? Gender.FEMALE : Gender.MALE)
                .occupation(pick(OCCUPATIONS))
                .mobile(hasMobile ? "9" + String.format("%09d", random.nextInt(1_000_000_000)) : null)
                .consentGiven(true)
                .whatsappOptIn(hasMobile && random.nextDouble() < .45)
                .latitude(point[0])
                .longitude(point[1])
                .gpsAccuracyMeters(5.0 + random.nextInt(20))
                .distanceFromWardMeters(location.distanceMeters())
                .locationStatus(location.status())
                .startedAt(submitted.minusSeconds(duration))
                .submittedAt(submitted)
                .durationSeconds(duration)
                .build();

        List<Integer> reds = new ArrayList<>();
        List<Integer> yellows = new ArrayList<>();
        for (Question q : questions) {
            Rating rating = rate(ward.getId(), q.getId());
            if (rating == Rating.RED) reds.add(q.getId());
            if (rating == Rating.YELLOW) yellows.add(q.getId());
            s.getAnswers().add(SurveyAnswer.builder().survey(s).question(q).rating(rating).build());
        }
        Collections.shuffle(reds, random);
        Collections.shuffle(yellows, random);
        List<Integer> priorities = new ArrayList<>(reds);
        priorities.addAll(yellows);
        s.setPriorityQuestionIds(new ArrayList<>(priorities.subList(0, Math.min(3, priorities.size()))));
        return s;
    }

    private Rating rate(int wardId, int questionId) {
        if (random.nextDouble() < notApplicableChance(wardId, questionId)) {
            return Rating.NA;
        }
        double red = BASE_RED[questionId] + WARD_BUMPS.getOrDefault(wardId, Map.of()).getOrDefault(questionId, 0.0);
        red = Math.max(.03, Math.min(.85, red));
        double yellow = Math.min(.38, 1 - red - .05);
        double r = random.nextDouble();
        return r < red ? Rating.RED : r < red + yellow ? Rating.YELLOW : Rating.GREEN;
    }

    /** Some issues don't apply everywhere, e.g. canal irrigation in the city core. */
    private static double notApplicableChance(int wardId, int questionId) {
        boolean outer = wardId >= 7;
        return switch (questionId) {
            case 7 -> outer ? .05 : .35;
            case 11 -> wardId == 10 ? .02 : .45;
            case 12 -> wardId == 1 ? .20 : .45;
            case 13 -> wardId == 2 ? .15 : .50;
            case 14 -> wardId == 5 || wardId == 10 ? .05 : .25;
            case 15 -> outer ? .30 : .90;
            case 16 -> outer ? .35 : wardId == 5 ? .40 : .80;
            case 18 -> outer ? .10 : .45;
            case 22 -> .15;
            default -> .03;
        };
    }

    // ---------------- tickets ----------------

    private static final Map<ComplaintCategory, String[]> DESCRIPTIONS = Map.ofEntries(
            Map.entry(ComplaintCategory.DRINKING_WATER, new String[]{
                    "No water in the taps for the last 4 days.",
                    "Pipeline leaking at the lane corner; dirty water mixing in.",
                    "Hand pump broken for two weeks."}),
            Map.entry(ComplaintCategory.ROAD, new String[]{
                    "Large potholes on the lane; water collects after rain.",
                    "Road dug up for a pipeline and never repaired.",
                    "Kachcha lane, impassable for two-wheelers in rain."}),
            Map.entry(ComplaintCategory.DRAINAGE, new String[]{
                    "Drain choked and overflowing onto the road.",
                    "Open drain without a cover right next to houses.",
                    "Sewage flows back into homes when it rains."}),
            Map.entry(ComplaintCategory.STREETLIGHT, new String[]{
                    "Three street lights not working on this lane.",
                    "Pole light fused for over a month.",
                    "The whole lane is dark at night."}),
            Map.entry(ComplaintCategory.GARBAGE, new String[]{
                    "Garbage not collected for a week.",
                    "Garbage dumped on the empty plot near the school.",
                    "Waste pile next to the drain; foul smell."}),
            Map.entry(ComplaintCategory.ELECTRICITY, new String[]{
                    "Low voltage every evening; fans and coolers don't run.",
                    "Transformer burnt, no power for 3 days.",
                    "Loose hanging wires near the market."}),
            Map.entry(ComplaintCategory.STRAY_CATTLE, new String[]{
                    "Stray cattle block the main road every morning.",
                    "Cattle damaging crops in the nearby fields."}),
            Map.entry(ComplaintCategory.HEALTH, new String[]{
                    "Doctor not present at the health centre for days.",
                    "No medicines available at the health centre."}),
            Map.entry(ComplaintCategory.SCHOOL, new String[]{
                    "School toilets are not usable.",
                    "Teachers often absent; mid-day meal irregular."}),
            Map.entry(ComplaintCategory.RATION, new String[]{
                    "Ration short by 2 kg per card.",
                    "Ration shop opens on irregular dates."}),
            Map.entry(ComplaintCategory.PENSION_HOUSING, new String[]{
                    "Old-age pension not received for 4 months.",
                    "Awas instalment pending for 8 months."}),
            Map.entry(ComplaintCategory.OTHER, new String[]{
                    "Encroachment blocking the lane.",
                    "Public toilet kept locked."}));

    private static final String[] LANDMARKS = {"Near the primary school", "Behind the Shiv temple", "Main market lane",
            "Opposite the water tank", "Near the ration shop", "Next to the masjid", "Near the bus stop",
            "Behind the post office", "Near the ghat steps", "Lane 3, near the flour mill"};

    private static final String[] PROGRESS_NOTES = {"Forwarded to Nagar Palika.", "Team visited, work started.",
            "Raised with the ward office.", "Department has acknowledged the issue."};
    private static final String[] RESOLVED_NOTES = {"Repaired and confirmed with the resident.", "Work completed.",
            "Fixed; photo taken after repair.", "Resolved by the department and checked by volunteer."};

    private void seedComplaints() {
        Instant now = Instant.now();
        List<Complaint> all = new ArrayList<>();
        for (Ward ward : wardRepository.findAll(Sort.by("id"))) {
            List<Volunteer> volunteers = volunteerRepository.findByWardId(ward.getId());
            int count = 9 + random.nextInt(7);
            for (int i = 0; i < count; i++) {
                ComplaintCategory category = pickCategory(ward.getId());
                // Complaints stay inside their ward; only surveys get deliberate outliers (for the Checks tab).
                double[] point = randomPoint(ward, false);
                LocationService.Check location = locationService.check(ward, point[0], point[1]);
                Instant created = now.minus(Duration.ofHours(24 + random.nextInt(24 * 24)));

                Complaint c = Complaint.builder()
                        .ward(ward)
                        .volunteer(volunteers.get(random.nextInt(volunteers.size())))
                        .category(category)
                        .description(pick(DESCRIPTIONS.get(category)))
                        .landmark(pick(LANDMARKS))
                        .residentName(random.nextBoolean() ? pick(random.nextBoolean() ? FEMALE : MALE) + " " + pick(SURNAMES) : null)
                        .residentMobile(random.nextBoolean() ? "9" + String.format("%09d", random.nextInt(1_000_000_000)) : null)
                        .latitude(point[0])
                        .longitude(point[1])
                        .distanceFromWardMeters(location.distanceMeters())
                        .locationStatus(location.status())
                        .status(ComplaintStatus.REPORTED)
                        .createdAt(created)
                        .build();
                c.getUpdates().add(update(c, ComplaintStatus.REPORTED, null, created));

                double r = random.nextDouble();
                if (r >= .42) {
                    long age = Duration.between(created, now).toMinutes();
                    Instant progressAt = created.plus(Duration.ofMinutes((long) (age * (.2 + random.nextDouble() * .2))));
                    c.setStatus(ComplaintStatus.IN_PROGRESS);
                    c.getUpdates().add(update(c, ComplaintStatus.IN_PROGRESS, pick(PROGRESS_NOTES), progressAt));
                    if (r >= .67) {
                        long rest = Duration.between(progressAt, now).toMinutes();
                        Instant resolvedAt = progressAt.plus(Duration.ofMinutes((long) (rest * (.3 + random.nextDouble() * .6))));
                        c.setStatus(ComplaintStatus.RESOLVED);
                        c.setResolvedAt(resolvedAt);
                        c.getUpdates().add(update(c, ComplaintStatus.RESOLVED, pick(RESOLVED_NOTES), resolvedAt));
                    }
                }
                all.add(c);
            }
        }
        complaintRepository.saveAll(all);
        all.forEach(c -> c.setTicketNo(ComplaintService.ticketNo(c.getWard().getId(), c.getId())));
        log.info("Seeded {} sample tickets", all.size());
    }

    private ComplaintCategory pickCategory(int wardId) {
        boolean outer = wardId >= 7;
        Map<ComplaintCategory, Double> weights = new EnumMap<>(ComplaintCategory.class);
        weights.put(ComplaintCategory.ROAD, outer ? 4.0 : 3.0);
        weights.put(ComplaintCategory.DRAINAGE, outer ? 4.0 : 3.0);
        weights.put(ComplaintCategory.GARBAGE, outer ? 2.0 : 3.5);
        weights.put(ComplaintCategory.DRINKING_WATER, wardId == 9 ? 4.0 : 2.5);
        weights.put(ComplaintCategory.STREETLIGHT, wardId == 4 ? 4.0 : 2.5);
        weights.put(ComplaintCategory.ELECTRICITY, 2.0);
        weights.put(ComplaintCategory.STRAY_CATTLE, outer ? 2.5 : 0.7);
        weights.put(ComplaintCategory.HEALTH, 1.0);
        weights.put(ComplaintCategory.SCHOOL, 0.8);
        weights.put(ComplaintCategory.RATION, wardId == 4 ? 2.5 : 1.0);
        weights.put(ComplaintCategory.PENSION_HOUSING, 1.0);
        weights.put(ComplaintCategory.OTHER, 0.5);

        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double r = random.nextDouble() * total;
        for (Map.Entry<ComplaintCategory, Double> e : weights.entrySet()) {
            r -= e.getValue();
            if (r <= 0) return e.getKey();
        }
        return ComplaintCategory.OTHER;
    }

    private static ComplaintUpdate update(Complaint c, ComplaintStatus status, String note, Instant at) {
        return ComplaintUpdate.builder().complaint(c).status(status).note(note).createdAt(at).build();
    }

    // ---------------- helpers ----------------

    /** A point inside the ward, or (when outside=true) beyond its edge to exercise the GPS check. */
    private double[] randomPoint(Ward ward, boolean outside) {
        double distance = outside
                ? ward.getRadiusMeters() * (1.25 + random.nextDouble() * 0.6)
                : ward.getRadiusMeters() * .85 * Math.sqrt(random.nextDouble());
        double bearing = random.nextDouble() * 2 * Math.PI;
        double dLat = distance * Math.cos(bearing) / 111_320.0;
        double dLng = distance * Math.sin(bearing) / (111_320.0 * Math.cos(Math.toRadians(ward.getCenterLat())));
        return new double[]{ward.getCenterLat() + dLat, ward.getCenterLng() + dLng};
    }

    /** Daytime (8am-7pm IST) on one of the last {@code days} days, never in the future. */
    private Instant randomPastTime(int days) {
        Instant now = Instant.now();
        LocalDate day = LocalDate.now(MetaService.IST).minusDays(random.nextInt(days + 1));
        Instant t = day.atTime(LocalTime.of(8 + random.nextInt(11), random.nextInt(60)))
                .atZone(MetaService.IST).toInstant();
        return t.isAfter(now) ? t.minus(Duration.ofDays(1)) : t;
    }

    private <T> T pick(T[] options) {
        return options[random.nextInt(options.length)];
    }
}
