package thn.tools;

public class ProcessInfo
{
    public static final String BY_MILLISECONDS = "ms";
    public static final String BY_SECONDS = "s";
    public static final String BY_MINUTES = "min";
    public static final String BY_HOURS = "hrs";

    public static long totalTime = 0L;
    public static long totalCount = 0L;

    public static void addTime(final long time)
    {
        totalTime += time;
    }

    public static void addCount(final long count)
    {
        totalCount += count;
    }

    public static void incrementCounter()
    {
        totalCount++;
    }

    public static String createMessage(final String message, final long start, final long end, final String timeUnits)
    {
        return createMessage(message, (end - start), timeUnits);
    }

    public static String createMessage(final String message, final long timeDiff, final String timeUnits)
    {
        switch (timeUnits)
        {
            case BY_MILLISECONDS:
                return (String.format("%s (%,dms)", message, getTime(timeDiff, timeUnits)));

            default:
                return (String.format("%s (%,.3f%s)", message, getTime(timeDiff, timeUnits), timeUnits));
        }
    }

    public static String getInfo(final String message, final String timeUnits)
    {
        return (String.format("%s %,d items in %,.3f%s)", message, totalCount, getTime(totalTime, timeUnits), timeUnits));
    }

    public static double getTime(final long time, final String timeUnits)
    {
        double convertedTime = time;

        switch (timeUnits)
        {
            case BY_SECONDS:
                convertedTime = toSeconds(time);
                break;

            case BY_MINUTES:
                convertedTime = toMinutes(time);
                break;

            case BY_HOURS:
                convertedTime = toHours(time);
                break;
        }
        return convertedTime;
    }

    public static double toSeconds(final long time)
    {
        return ((double) time / 1000);
    }

    public static double toMinutes(final long time)
    {
        return ((double) time / (60 * 1000));
    }

    public static double toHours(final long time)
    {
        return ((double) time / (60 * 60 * 1000));
    }
}
