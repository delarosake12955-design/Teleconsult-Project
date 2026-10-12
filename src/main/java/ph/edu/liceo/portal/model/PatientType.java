package ph.edu.liceo.portal.model;

/**
 * How a patient is served at the clinic.
 *
 * SENIOR is worked out from the date of birth (age 60 or above). PWD (person with
 * disability) is declared by the patient together with a PWD ID number. Seniors and PWDs
 * are priority patients: their requests are reviewed before regular ones.
 */
public enum PatientType {

    REGULAR("Regular", false),
    SENIOR("Senior", true),
    PWD("PWD", true),
    SENIOR_PWD("Senior + PWD", true);

    /** Age (in years) at which a patient counts as a senior citizen. */
    public static final int SENIOR_AGE = 60;

    private final String label;
    private final boolean priority;

    PatientType(String label, boolean priority) {
        this.label = label;
        this.priority = priority;
    }

    public String getLabel() {
        return label;
    }

    /** True for seniors and PWDs: they are reviewed first. */
    public boolean isPriority() {
        return priority;
    }

    public static PatientType of(boolean senior, boolean pwd) {
        if (senior && pwd) return SENIOR_PWD;
        if (senior) return SENIOR;
        if (pwd) return PWD;
        return REGULAR;
    }
}
