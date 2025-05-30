//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class DailyScheduler {
//    public static void scheduleDailyTask(Runnable task, int hour, int minute) {
//        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime firstRun = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
//
//        if (now.compareTo(firstRun) > 0) {
//            // الوقت الحالي تجاوز 11:59، نضبط أول تشغيل للغد
//            firstRun = firstRun.plusDays(1);
//        }
//
//        long initialDelay = Duration.between(now, firstRun).getSeconds();
//        long period = TimeUnit.DAYS.toSeconds(1); // كل 24 ساعة
//
//        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
//    }
//}
