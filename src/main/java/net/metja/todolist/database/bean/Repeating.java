package net.metja.todolist.database.bean;

/**
 * @author: Janne Metso @copy; 2019
 * @since: 2019-11-23
 */
public enum Repeating {

    No, Daily, Weekly, BiWeekly, Monthly, Yearly;

    public static String getRepeating(Repeating repeating) {
        return ""+repeating;
    }

    public static Repeating getRepeating(String repeating) {
        if("no".equalsIgnoreCase(repeating)) {
            return Repeating.No;
        } else if("daily".equalsIgnoreCase(repeating)) {
            return Repeating.Daily;
        } else if("weekly".equalsIgnoreCase(repeating)) {
            return Repeating.Weekly;
        } else if("biweekly".equalsIgnoreCase(repeating)) {
            return Repeating.BiWeekly;
        } else if("monthly".equalsIgnoreCase(repeating)) {
            return Repeating.Monthly;
        } else {
            return Repeating.Yearly;
        }
    }

}
