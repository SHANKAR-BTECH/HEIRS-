package com.heirs.desktop.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic placeholder catalogue for the Phase J1 UI.
 *
 * <p>Contains sixty demo records whose category distribution intentionally
 * mirrors the existing web demo dataset (Policy 19, Scheme 12, Regulation 9,
 * Project 10, Rules 10). Everything is generated in-memory; nothing is
 * persisted and no API is contacted.
 *
 * <p>TODO Phase J2/J4: remove this class and load real records from the
 * Spring Boot REST API instead.
 */
public final class DemoData {

    public static final String[] CATEGORIES = {"Policy", "Scheme", "Regulation", "Project", "Rules"};
    public static final String[] STATUSES = {"Active", "Draft", "Completed", "Archived"};
    public static final String[] DEPARTMENTS = {
            "Higher Education",
            "Technical Education",
            "Medical Education",
            "Scholarships",
            "Research & Quality",
            "University Affairs"
    };
    public static final int[] YEARS = {2019, 2020, 2021, 2022, 2023, 2024, 2025, 2026};
    public static final String[] STATUS_CYCLE = {
            "Active", "Active", "Active", "Active", "Draft", "Completed", "Active", "Archived"
    };

    private DemoData() {
    }

    /** Full placeholder catalogue (60 records). */
    public static List<DemoRecord> records() {
        return RECORDS;
    }

    public static List<DemoRecord> recent(int max) {
        return RECORDS.stream()
                .sorted((a, b) -> {
                    int byYear = Integer.compare(b.getYear(), a.getYear());
                    return byYear != 0 ? byYear : Long.compare(b.getId(), a.getId());
                })
                .limit(max)
                .collect(Collectors.toList());
    }

    /** Map category_name -> record count. */
    public static Map<String, Integer> statusCounts() {
        return countBy(DemoRecord::getStatus, List.of(STATUSES));
    }

    /** Map category_name -> record count. */
    public static Map<String, Integer> categoryCounts() {
        return countBy(DemoRecord::getCategory, List.of(CATEGORIES));
    }

    /** Map year -> record count (ascending). */
    public static Map<Integer, Integer> yearCounts() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (DemoRecord r : RECORDS) {
            counts.merge(r.getYear(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private static Map<String, Integer> countBy(
            java.util.function.Function<DemoRecord, String> key, List<String> orderedKeys) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        orderedKeys.forEach(k -> counts.put(k, 0));
        for (DemoRecord r : RECORDS) {
            counts.merge(key.apply(r), 1, Integer::sum);
        }
        return counts;
    }

    /** Unique years used by the catalogue. */
    public static List<Integer> distinctYears() {
        Set<Integer> set = new LinkedHashSet<>();
        for (DemoRecord r : RECORDS) {
            set.add(r.getYear());
        }
        return set.stream().sorted().collect(Collectors.toList());
    }

    /* ------------------------------------------------------------------ */

    private static List<DemoRecord> buildRecords() {
        List<String[]> categories = new ArrayList<>();
        int[] counts = {19, 12, 9, 10, 10};
        categories.add(seed("Policy", counts[0], POLICY_SUBJECTS, "POL",
                "Policy guidance governing %s across state higher-education institutions."));
        categories.add(seed("Scheme", counts[1], SCHEME_NAMES, "SCM",
                "Financial assistance programme under %s, administered by the department."));
        categories.add(seed("Regulation", counts[2], REGULATION_SUBJECTS, "REG",
                "Regulatory framework and compliance requirements on %s for affiliated institutions."));
        categories.add(seed("Project", counts[3], PROJECT_NAMES, "PRJ",
                "Institutional modernisation project focused on %s, implemented under HEIRS."));
        categories.add(seed("Rules", counts[4], RULES_SUBJECTS, "RUL",
                "Operational rules and code of practice covering %s."));

        List<DemoRecord> all = new ArrayList<>();
        long id = 1;
        int global = 0;
        for (String[] cat : categories) {
            String category = cat[0];
            String[] subjects = cat[1].split("\\|");
            String prefix = cat[2];
            String template = cat[3];
            for (int i = 0; i < subjects.length; i++) {
                String subject = subjects[i];
                int year = YEARS[global % YEARS.length];
                String status = STATUS_CYCLE[global % STATUS_CYCLE.length];
                String department = DEPARTMENTS[(global + i) % DEPARTMENTS.length];
                String reference = prefix + "-" + year + "-" + String.format("%03d", i + 1);
                String title = subject;
                String description = String.format(template, subject.toLowerCase());
                String keywords = String.join(", ",
                        List.of(category.toLowerCase(), subject.toLowerCase(),
                                "higher education", String.valueOf(year)));
                all.add(new DemoRecord(id++, reference, title, category, department, year,
                        status, description, keywords, "HEIRS Demo Repository"));
                global++;
            }
        }
        return List.copyOf(all);
    }

    private static String[] seed(String category, int count, String[] items, String prefix,
                                 String descriptionTemplate) {
        return new String[]{category, String.join("|", items), prefix, descriptionTemplate};
    }

    /* Subject / title pools — each entry yields one unique record title. */

    private static final String[] POLICY_SUBJECTS = {
            "Research & Development Policy (2021)",
            "Teacher Education Policy (2022)",
            "Student Welfare Policy (2020)",
            "Digital Campus Policy (2023)",
            "International Education Policy (2024)",
            "Skill Development Policy (2021)",
            "University Autonomy Policy (2019)",
            "Open Access Policy (2022)",
            "Quality Assurance Policy (2023)",
            "Women Education Policy (2020)",
            "Tribal Education Policy (2021)",
            "Distance Education Policy (2022)",
            "Entrepreneurship Policy (2024)",
            "Green Campus Policy (2023)",
            "Academic Integrity Policy (2024)",
            "Capacity Building Policy (2020)",
            "Adult Literacy Policy (2021)",
            "National Education Policy Alignment (2025)",
            "Campus Security Policy (2026)"
    };

    private static final String[] SCHEME_NAMES = {
            "Post-Matric Merit Scholarship Scheme (2024)",
            "State Research Fellowship Scheme (2023)",
            "Women Scholar Grant Scheme (2022)",
            "Minority Community Scholarship Scheme (2021)",
            "Digital Textbook Initiative Scheme (2024)",
            "Faculty Travel Grant Scheme (2020)",
            "Rural College Development Grant Scheme (2021)",
            "Research Pilot Project Fund Scheme (2022)",
            "Library Modernization Grant Scheme (2023)",
            "STEM Outreach Camp Scheme (2024)",
            "Vocational Training Sponsorship Scheme (2020)",
            "University Innovation Seed Fund Scheme (2025)"
    };

    private static final String[] REGULATION_SUBJECTS = {
            "Regulation on Accreditation Norms (2022)",
            "Regulation on Fee Structure Guidelines (2021)",
            "Regulation on Admissions Procedure (2023)",
            "Regulation on Examination Reform Protocol (2024)",
            "Regulation on Anti-Ragging Measures (2020)",
            "Regulation on Reservation Implementation (2022)",
            "Regulation on Faculty Recruitment Standards (2023)",
            "Regulation on Affiliation Conditions (2021)",
            "Regulation on Online Course Approval (2024)"
    };

    private static final String[] PROJECT_NAMES = {
            "Digital University Records Project (2023)",
            "Campus Wi-Fi Modernization Project (2022)",
            "Research Data Repository Project (2024)",
            "Library Digitization Project (2021)",
            "AI-Enabled Student Support Project (2025)",
            "Green Energy Campus Project (2023)",
            "Smart Classroom Rollout Project (2024)",
            "Alumni Network Platform Project (2022)",
            "Institutional ERP Consolidation Project (2025)",
            "Open Courseware Portal Project (2026)"
    };

    private static final String[] RULES_SUBJECTS = {
            "Code of Conduct for Students Rules (2021)",
            "Staff Leave Guidelines Rules (2020)",
            "Laboratory Safety Rules (2022)",
            "Sports Facility Usage Rules (2021)",
            "Library Borrowing Rules (2019)",
            "Hostel Discipline Rules (2022)",
            "Data Retention Guidelines Rules (2023)",
            "Conduct of Examinations Rules (2020)",
            "Media & Press Interaction Rules (2021)",
            "Procurement of Equipment Rules (2023)"
    };

    /** Full placeholder catalogue (60 records). */
    private static final List<DemoRecord> RECORDS = buildRecords();

}