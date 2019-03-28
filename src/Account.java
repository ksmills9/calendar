package src;

import java.util.TimeZone;

/**
 * Account class stores the necessary information about the user.
 */

public class Account {
    private int ID;
    private String name;
    private TimeZone timeZone = TimeZone.getDefault();
    private AllEvents allEvents = new AllEvents();
    private AllReminders allReminders = new AllReminders();

    /**
     * Create an account with name
     * @param name name of the user
     */
    public Account(String name){
        this.name = name;
    }

    /**
     * Create an account with name and timezone
     * @param name name of the user
     * @param timezone timezone of the user
     */
    public Account(String name, String timezone){
        this.name = name;
        this.timeZone = TimeZone.getTimeZone(timezone);
    }

    /**
     * Create an account with name, ID and timezone of the user
     * @param ID ID of the account
     * @param name name of the user
     * @param timezone timezone of the user
     */
    public Account(int ID, String name, String timezone){
        this.name = name;
        this.timeZone = TimeZone.getTimeZone(timezone);
        this.ID = ID;
    }

    /**
     * Copy constructor
     * @param toCopy the account to copy
     */
    public Account(Account toCopy){
        this.name = toCopy.name;
        this.ID = toCopy.ID;
        this.timeZone = toCopy.timeZone;
    }

    /**
     * Get the name of the account
     * @return name of the account
     */
    public String getName() {
        return name;
    }

    /**
     * get the timezone of the account as a string
     * @return timezone of the account
     */
    public String getTimeZone() {
        return timeZone.toZoneId().toString();
    }

    /**
     * Set/change the name of the account.
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set/change the timezone of the account
     * @param timeZone the timezone to set
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = TimeZone.getTimeZone(timeZone);
    }

    /**
     * Set the ID of the account
     * @param ID the ID to set
     */
    public void setID(int ID){
        this.ID = ID;
    }

    /**
     * Get the ID of the account
     * @return ID of the account
     */
    public int getID(){
        return ID;
    }

    /**
     * get a copy of AllEvent instance of the account
     * @return a new copy of AllEvents instance
     */
    public AllEvents getAllEvents() {
        return allEvents;
    }

    public AllReminders getAllReminders(){
        return allReminders;
    }

    /**
     * set a new AllEvents instance to the account
     * @param allEvents the AllEvents instance to copy from
     */
    public void setAllEvents(AllEvents allEvents) {
        this.allEvents = allEvents;
    }

    public void setAllReminders(All Reminders allReminders){
        this.allReminders = allReminders;
    }

    /**
     * Returns all information of the user as a string
     * @return string version of the instance
     */
    @Override
    public String toString() {
        return "My Name: " + name +
                "\nMy ID: " + ID +
                "\nMy TimeZone: " + getTimeZone() +
                "\nMy Events: " + getAllEvents().getEvents().size();
    }
}