package com.corejsf.timesheet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.jsp.jstl.sql.Result;

import com.corejsf.databaseAccess.TimesheetBean;
import com.corejsf.employee.Employee;
import com.corejsf.employee.EmployeeManager;

/**
 * TimsheetManager class implement TimesheetCollection interface,
 * and contain some method to handle timesheet form.
 * @author Thi Hong Phuc Le (Katherine) & Jung Jae Lee
 * @version 1.0
 */
@Named("timesheetManager")
@SessionScoped
public class TimesheetManager implements TimesheetCollection {
    
    @Inject TimesheetBean timesheetDB;
    
    @Inject EmployeeManager currentEmployee;

    /**
     * A serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * A list of time sheets.
     */
    private List<Timesheet> timesheets = new ArrayList<>();
    
    /**
     * The current displayed time sheet to view and edit.
     */
    private Timesheet displayedTimesheet;
    
    /**
     * The current week value.
     */
    private Integer currentWeek;
    
    /**
     * Contains the total hours of each day.
     * Order is SAT-SUN-MON-TUE-WED-THU-FRI-TOTALOFWEEK.
     * Starts from 0 to 7.
     */
    private float[] hourSummary = {0, 0, 0, 0, 0, 0, 0, 0};
    
    /**
     * The employeeManager object to get the current users.
     */
    @Inject
    private EmployeeManager employeeManager;
    
    /**
     * A constructor for TimsheetManager.
     * Set the currentWeek value.
     */
    public TimesheetManager() {
        LocalDate date = LocalDate.now();
        currentWeek = date.get(WeekFields.of(Locale.getDefault()).weekOfYear());
    }

    /**
     * Return the timesheets list.
     * @return the timesheets list
     */
    @Override
    public List<Timesheet> getTimesheets() {
        return timesheets;
    }

    /**
     * Return the timesheets list for a specific employee.
     * @return a time sheet list
     */
    @Override
    public List<Timesheet> getTimesheets(Employee e) {
        List<Timesheet> usersTimesheets = new ArrayList<>();
        for(Timesheet timesheet : timesheets) {
            if (timesheet.getEmployee().getEmpNumber() == e.getEmpNumber()) {
                usersTimesheets.add(timesheet);
            }
        }
        
        return usersTimesheets;
    }

    /**
     * Return a current timesheet for a specific employee
     * @param e an Employee
     * @return a Timesheet
     */
    @Override
    public Timesheet getCurrentTimesheet(Employee e) {
        List<Timesheet> usersTimesheets = getTimesheets(e);
        if(usersTimesheets.size() <= 0) {
            return null;
        }
        
        for (Timesheet timesheet : usersTimesheets) {
            if(timesheet.getWeekNumber() == currentWeek) {
                return timesheet;
            }
        }
        return usersTimesheets.get(usersTimesheets.size() - 1);
    }
    
    /**
     * Create a new time sheet for current user,
     *  add it to the list and return outcome url to navigate.
     *  @return a string
     */
    @Override
    public String addTimesheet() {
        int initialRows = 5;
        LocalDate endDate = LocalDate.now();
        
        Employee currentEmployee = employeeManager.getCurrentEmployee();
        
        Timesheet newTimesheet = new Timesheet(currentEmployee, endDate);
        
        for (int i = 0; i < initialRows; ++i) {
            newTimesheet.addRow();
        }
        
        displayedTimesheet = newTimesheet;
        timesheets.add(newTimesheet);
        
        return "editTimesheet";
    }

    /**
     * Returns the current week.
     * @return currentWeek an integer
     */
    public int getCurrentWeek() {
        return currentWeek;
    }

    /**
     * Setter for the currentWeek.
     * @param currentWeek
     */
    public void setCurrentWeek(int currentWeek) {
        this.currentWeek = currentWeek;
    }

    /**
     * Getter for displayedTimesheet.
     * @return a Timesheet
     */
    public Timesheet getDisplayedTimesheet() {
        Employee currentEmployee = employeeManager.getCurrentEmployee();
        displayedTimesheet =  displayedTimesheet == null ? getCurrentTimesheet(currentEmployee) : displayedTimesheet;
        return displayedTimesheet;
    }

    /**
     * Setter for displayedTimesheet.
     * @param displayedTimesheet a Timesheet
     */
    public void setDisplayedTimesheet(Timesheet displayedTimesheet) {
        this.displayedTimesheet = displayedTimesheet;
    }
    
    /**
     * Getter for employeeManager.
     * @return employeeManager
     */
    public EmployeeManager getEmployeeManager() {
        return employeeManager;
    }

    /**
     * Getter for employeeManager.
     * @param employeeManager
     */
    public void setEmployeeManager(EmployeeManager employeeManager) {
        this.employeeManager = employeeManager;
    }

    /**
     * Handle edit time sheet event and return a outcome to navigate.
     * @param timesheet a Timesheet
     * @return a string
     */
    public String editTimesheet(Timesheet timesheet) {
        displayedTimesheet = timesheet;
        return "editTimesheet";
    }
    
    /**
     * Handle delete time sheet event and return a outcome to navigate.
     * @param timesheet a Timesheet
     * @return a string
     * @throws SQLException 
     */
    public String deleteTimesheet(Timesheet timesheet) throws SQLException {
        
        removeTimesheetFromDB(timesheet);
        timesheets.remove(timesheet);
        return "timesheetList";
    }
    
    /**
     * Handle save time sheet form event and return a outcome to navigate.
     * @param timesheet a Timesheet
     * @return a string
     */
    public String saveTimesheet() {
        System.out.println("Save is clicked");
        System.out.println(displayedTimesheet);

        for (Timesheet timesheet : timesheets) {
            if(timesheet.getEndDate() == displayedTimesheet.getEndDate() 
              && timesheet.getEmployee().getEmpNumber()
              == displayedTimesheet.getEmployee().getEmpNumber()) {
                timesheet.setDetails(displayedTimesheet.getDetails());
                timesheet.setEndDate(displayedTimesheet.getEndDate());
            }
        }
        
        calculateTotalHours();
        return "viewTimesheet";
    }
    
    /**
     * Handle view time sheet list event and return a outcome to navigate.
     * @param timesheet a Timesheet
     * @return a string
     */
    public String displayTimesheetList() {
        return "timesheetList";
    }

    /**
     * Handle edit time sheet event and return a outcome to navigate.
     * @param timesheet a Timesheet
     * @return a string
     * @throws SQLException 
     */
    public String viewTimesheet(Timesheet timesheet) throws SQLException {
        for (int i = 0; i < timesheet.getDetails().size();i++) {
            System.out.println("TIMESHEET DETAILS:\n" + timesheet.getDetails().get(i).getNotes());
        }
        setDisplayedTimesheet(timesheet);
        
        getCurrentTimesheetFromDBForMainViewPage(timesheet);
        return "viewTimesheet";
    }
    
    /**
     * Handle add time sheet row event and return a outcome to redirect.
     * @param timesheet a Timesheet
     * @return a string
     */
    public String addRowToDisplayedTimesheet() {
        displayedTimesheet.addRow();
        return "editTimesheet";
    }
    
    /**
     * Calculate the total hours for week.
     */
    public void calculateTotalHours() {
        int totalIndex = 7;
        for(TimesheetRow row : displayedTimesheet.getDetails()) {
            float[] hoursRow = row.getHoursPerDay();
            float totalHourPerRow = 0;
            for (int i = 0; i < hoursRow.length; i++) {
                hourSummary[i] += hoursRow[i];
                totalHourPerRow += hoursRow[i];
            }
            row.setTotalWeekHours(totalHourPerRow);
            hourSummary[totalIndex] += totalHourPerRow;
        }
    }
    
    /**
     * Getter for hourSummary.
     * @return hourSummary a float array
     */
    public float[] getHourSummary() {
        return hourSummary;
    }
    
    public String addTimesheetRowToDB() throws SQLException {
        saveTimesheet();
        
        for (int i = 0; i < this.displayedTimesheet.getDetails().size(); i++) {
            if (this.displayedTimesheet.getDetails().get(i).getProjectId() != 0 && this.displayedTimesheet.getDetails().get(i).getWorkPackageId() != null) {
                this.displayedTimesheet.getDetails().get(i).setWeekNumber(displayedTimesheet.getWeekNumber());
                timesheetDB.addTimesheetRow(this.displayedTimesheet.getDetails().get(i), currentEmployee.getCurrentEmployee(), displayedTimesheet.getWeekNumber());
            }
        }
        
        return "viewSingleTimesheet.xhtml";
    }
    
    public List<TimesheetRow> getCurrentTimesheetFromDB() throws SQLException {
        if (displayedTimesheet == null) {
            List<TimesheetRow> t1 = getCurrentTimesheetFromDBForMainViewPage(getDisplayedTimesheet());
            Timesheet t = new Timesheet();
            t.setEmployee(currentEmployee.getCurrentEmployee());
            t.setDetails(t1);
            return t.getDetails();
        } else {
             List<TimesheetRow> r = timesheetDB.getCurrentTimesheet(currentEmployee.getCurrentEmployee(), displayedTimesheet);
        Timesheet t = new Timesheet();
        
        t.setEmployee(currentEmployee.getCurrentEmployee());
        t.setDetails(r);
        
        //System.out.println(r.getColumnNames().toString());
        return t.getDetails();
        }
       
    }
    public List<Timesheet> getAllTimesheetFromDB() throws SQLException {
        List<Timesheet> r = timesheetDB.getAll(currentEmployee.getCurrentEmployee());
        
        return r;
    }
    
    public List<TimesheetRow> getCurrentTimesheetFromDBForMainViewPage(Timesheet t) throws SQLException {
        List<TimesheetRow> r = timesheetDB.getCurrentTimesheet(currentEmployee.getCurrentEmployee(), t);
        Timesheet t1 = new Timesheet();
        
        t1.setEmployee(currentEmployee.getCurrentEmployee());
        t1.setDetails(r);
        
        //System.out.println(r.getColumnNames().toString());
        return t1.getDetails();
    }
    
    public void removeTimesheetFromDB(Timesheet t) throws SQLException {
        timesheetDB.removeTimesheet(t, currentEmployee.getEmpNumber());
    }
    
    public float[] getTotalDailyHours(){
        float[] arr = displayedTimesheet.getDailyHours();
        return arr;
    }
    
    
}
