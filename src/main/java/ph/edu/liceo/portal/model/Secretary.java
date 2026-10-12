package ph.edu.liceo.portal.model;

public class Secretary extends User {

    private String department;

    public Secretary(
            int id,
            String fullName,
            String email,
            String password,
            String department) {

        super(id, fullName, email, password);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}