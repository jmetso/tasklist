package net.metja.todolist.database.bean;

/**
 * @author: Janne Metso @copy; 2022
 * @since: 2022-11-08
 */
public class Repeat {

    public enum TimePeriod { None, Days, Weeks, Months, Years }
    private int times;
    private TimePeriod period;

    public Repeat() {}

    public Repeat(int times, TimePeriod period) {
        this.times = times;
        this.period = period;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public TimePeriod getPeriod() {
        return period;
    }

    public void setPeriod(TimePeriod period) {
        this.period = period;
    }

    public static Repeat parse(String text) {
        if("No".equalsIgnoreCase(text)) {
            return new Repeat(0, TimePeriod.None);
        } else if("Daily".equalsIgnoreCase(text)) {
            return new Repeat(1, TimePeriod.Days);
        } else if("Weekly".equalsIgnoreCase(text)) {
            return new Repeat(1, TimePeriod.Weeks);
        } else if("Biweekly".equalsIgnoreCase(text)) {
            return new Repeat(2, TimePeriod.Weeks);
        } else if("Monthly".equalsIgnoreCase(text)) {
            return new Repeat(1, TimePeriod.Months);
        } else if("Yearly".equalsIgnoreCase(text)) {
            return new Repeat(1, TimePeriod.Years);
        } else if(text != null && text.startsWith("Every ")) {
            String timesValue = text.substring(6, text.indexOf(" ", 6));
            String periodValue = text.substring(text.indexOf(" ", 6)+1);
            int times = Integer.parseInt(timesValue);
            if("Days".equalsIgnoreCase(periodValue)) {
                return new Repeat(times, TimePeriod.Days);
            } else if("Weeks".equalsIgnoreCase(periodValue)) {
                return new Repeat(times, TimePeriod.Weeks);
            } else if("Months".equalsIgnoreCase(periodValue)) {
                return new Repeat(times, TimePeriod.Months);
            } else if("Years".equalsIgnoreCase(periodValue)) {
                return new Repeat(times, TimePeriod.Years);
            }
        }
        return null;
    }

    public String toString() {
        if(this.period == TimePeriod.None) {
            return "No";
        } else {
            return "Every " + this.times + " " + this.period;
        }
    }
    
}
